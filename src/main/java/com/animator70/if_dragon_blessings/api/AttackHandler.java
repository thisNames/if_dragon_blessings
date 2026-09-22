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

        // 火龙：点燃 + 轻微击退
        if (attacker.hasEffect(ModMobEffects.FIRE_ATTACK.get())) {
            target.setSecondsOnFire(5);

            knockback(target, attacker, 0.35F);
        }

        // 冰龙：只施加原版缓慢效果，不定身、不击退
        if (attacker.hasEffect(ModMobEffects.ICE_ATTACK.get())) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        }

        // 电龙：闪电链（带冷却，防止手速过快导致鬼畜）
        if (attacker.hasEffect(ModMobEffects.LIGHTNING_ATTACK.get())) {
            // 冷却检查：每个攻击者上次触发后，冷却时间内不再重复触发
            long now = attacker.level().getGameTime();
            long cooldown = DragonBlessingsConfig.CHAIN_COOLDOWN.get();
            Long lastTrigger = LAST_TRIGGER_TIME.get(attacker.getUUID());

            if (lastTrigger != null && now - lastTrigger < cooldown) {
                return;
            }
            LAST_TRIGGER_TIME.put(attacker.getUUID(), now);

            MobEffectInstance effect = attacker.getEffect(ModMobEffects.LIGHTNING_ATTACK.get());
            int amplifier = effect != null ? effect.getAmplifier() : 0;

            ChainLightningHelper.createChainLightning(attacker.level(), target, attacker, amplifier);
        }
    }

    private static void knockback(LivingEntity target, LivingEntity attacker, float strength) {
        double dx = attacker.getX() - target.getX();
        double dz = attacker.getZ() - target.getZ();

        target.knockback(strength, dx, dz);
    }
}
