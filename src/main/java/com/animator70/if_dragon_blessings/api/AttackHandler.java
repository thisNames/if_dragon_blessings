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
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Java 类
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 攻击方式与物品无关：只要攻击者持有对应的“攻击方式”buff
 * 命中目标时就触发对应的攻击特效
 */
@Mod.EventBusSubscriber(modid = IfDragonBlessings.MODID)
public class AttackHandler {
    // 记录每个攻击者上次触发闪电链的世界时间（tick），用于冷却，防止手速过快导致闪电链鬼畜。
    // 残留量极小（仅存在线玩家数量级），无需专门清理。
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

        // 火龙：点燃 5 秒 + 烈焰标记 5 秒 + 击退
        if (attacker.hasEffect(ModMobEffects.FIRE_ATTACK.get())) {
            target.setSecondsOnFire(5);
            target.addEffect(new MobEffectInstance(ModMobEffects.BLAZE.get(), 100, 0));

            knockback(target, attacker, 1.0F);
        }

        // 冰龙：冰封 + 缓慢 III + 挖掘疲劳 III（持续 10 秒）
        // 冰块渲染由 FrozenEvents 监听 MobEffectEvent 自动同步（任何方式施加 FROZEN 效果都生效）
        if (attacker.hasEffect(ModMobEffects.ICE_ATTACK.get())) {
            target.addEffect(new MobEffectInstance(ModMobEffects.FROZEN.get(), 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2));
        }

        // 电龙：闪电链（带冷却，防止手速过快导致鬼畜）
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
     * 玩家退出时清理其冷却记录，避免 LAST_TRIGGER_TIME 长期累积（防止轻微内存泄漏）
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_TRIGGER_TIME.remove(event.getEntity().getUUID());
    }
}
