package com.animator70.if_dragon_blessings.api;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;
import com.animator70.if_dragon_blessings.init.ModMobEffects;

// Minecraft 类
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// Forge 类
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Java 类
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 攻击方式与物品无关：只要攻击者持有对应的“攻击方式”buff
 * 命中目标时就触发对应的攻击特效（火龙 / 冰龙 / 电龙 三种攻击方式）
 */
@Mod.EventBusSubscriber(modid = IfDragonBlessings.MODID)
public class AttackHandler {
    // 记录每个攻击者上次触发闪电链的世界时间（tick），用于冷却，防止手速过快导致闪电链鬼畜。
    // 实体离开世界时（EntityLeaveLevelEvent）自动清理，玩家与非玩家一视同仁，不会累积。
    private static final Map<UUID, Long> LAST_TRIGGER_TIME = new HashMap<>();

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker.level().isClientSide) {
            return;
        }

        LivingEntity target = event.getEntity();

        // 【火龙】点燃 5 秒 + 烈焰标记 5 秒 + 击退（等级越高持续越久、击退越强）
        if (attacker.hasEffect(ModMobEffects.FIRE_ATTACK.get())) {
            double fireMultiplier = AttackMultipliers.of(attacker.getEffect(ModMobEffects.FIRE_ATTACK.get()),
                    DragonBlessingsConfig.FIRE_ATTACK_MAX_LEVEL.get());

            target.setSecondsOnFire((int) Math.round(5 * fireMultiplier));
            target.addEffect(new MobEffectInstance(ModMobEffects.BLAZE.get(),
                    (int) Math.round(100 * fireMultiplier), 0));

            knockback(target, attacker, (float) fireMultiplier);
        }

        // 【冰龙】冰封 + 缓慢 III + 挖掘疲劳 III（持续 10 秒，等级越高持续越久）
        // 冰块渲染由 FrozenEvents 监听 MobEffectEvent 自动同步（任何方式施加 FROZEN 效果都生效）
        if (attacker.hasEffect(ModMobEffects.ICE_ATTACK.get())) {
            double iceMultiplier = AttackMultipliers.of(
                    attacker.getEffect(ModMobEffects.ICE_ATTACK.get()),
                    DragonBlessingsConfig.ICE_ATTACK_MAX_LEVEL.get());

            int duration = (int) Math.round(200 * iceMultiplier);

            target.addEffect(new MobEffectInstance(ModMobEffects.FROZEN.get(), duration, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2));
        }

        // 【电龙】闪电链（带冷却，防止手速过快导致鬼畜）
        if (attacker.hasEffect(ModMobEffects.LIGHTNING_ATTACK.get())) {
            long now = attacker.level().getGameTime();
            long cooldown = DragonBlessingsConfig.CHAIN_COOLDOWN.get();
            Long lastTrigger = LAST_TRIGGER_TIME.get(attacker.getUUID());

            // 冷却过了才触发（不 return，避免影响同一次攻击里的火/冰分支）
            if (lastTrigger == null || now - lastTrigger >= cooldown) {
                LAST_TRIGGER_TIME.put(attacker.getUUID(), now);

                MobEffectInstance effect = attacker.getEffect(ModMobEffects.LIGHTNING_ATTACK.get());
                int amplifier = effect != null ? effect.getAmplifier() : 0;

                ChainLightningHelper.createChainLightning(attacker.level(), target, attacker, amplifier);
            }
        }
    }

    /**
     * 击退目标
     */
    private static void knockback(LivingEntity target, LivingEntity attacker, float strength) {
        double dx = attacker.getX() - target.getX();
        double dz = attacker.getZ() - target.getZ();

        target.knockback(strength, dx, dz);
    }

    /**
     * 实体离开世界（死亡 / 消失 / 区块卸载 / 换维度 / 玩家退出）时清理其冷却记录，
     * 防止 LAST_TRIGGER_TIME 长期累积（防止轻微内存泄漏）。
     * 覆盖所有移除路径，比只监听玩家退出或生物死亡更完整。
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        LAST_TRIGGER_TIME.remove(event.getEntity().getUUID());
    }
}
