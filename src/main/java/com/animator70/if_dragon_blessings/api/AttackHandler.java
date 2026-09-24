package com.animator70.if_dragon_blessings.api;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;
import com.animator70.if_dragon_blessings.init.ModMobEffects;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// Forge 类
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Java 类
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    /**
     * 攻击事件（LivingHurtEvent）：在护甲减免后、伤害结算前触发。
     * 用 LivingHurtEvent 而非 LivingAttackEvent，是为了用 setAmount 把龙之力伤害合并到玩家原本攻击伤害里，
     * 让两者一次结算、正确叠加（LivingAttackEvent 无 setAmount，嵌套 hurt 会被原版无敌帧吞掉其中一段）。
     *
     * 伤害结算统一收敛在这里：三个 calculate 方法只负责「计算伤害 + 施加效果」并返回伤害值，
     * 由本方法累加后一次性 setAmount 结算；triggeredReactions 跨三个方法去重，
     * 保证每种元素反应在同一次攻击里只触发一次（避免三效果同时攻击时重复触发）。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        // 攻击者必须是活体实体
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }
        // 客户端不处理
        if (attacker.level().isClientSide) {
            return;
        }

        // 获取实体目标
        LivingEntity target = event.getEntity();

        // 本次攻击已触发的元素反应（去重：每种反应只结算一次）
        Set<MobEffect> triggeredReactions = new HashSet<>();
        // 龙之力的总额外伤害
        float extraDamage = 0.0F;

        // 火龙：持有火龙之力才打出火龙攻击
        MobEffectInstance fireEffect = attacker.getEffect(ModMobEffects.FIRE_ATTACK.get());
        if (fireEffect != null) {
            extraDamage += calculateFireAttack(attacker, target, fireEffect, triggeredReactions);
        }

        // 冰龙：持有冰龙之力才打出冰龙攻击
        MobEffectInstance iceEffect = attacker.getEffect(ModMobEffects.ICE_ATTACK.get());
        if (iceEffect != null) {
            extraDamage += calculateIceAttack(target, iceEffect, triggeredReactions);
        }

        // 电龙：持有电龙之力才打出电龙攻击
        MobEffectInstance lightningEffect = attacker.getEffect(ModMobEffects.LIGHTNING_ATTACK.get());
        if (lightningEffect != null) {
            extraDamage += calculateLightningAttack(attacker, target, lightningEffect, triggeredReactions);
        }

        // 统一结算：玩家原本伤害 + 所有龙之力伤害
        event.setAmount(event.getAmount() + extraDamage);
    }

    /**
     * 【火龙】打出火龙攻击：点燃 + 烈焰标记 + 击退（等级越高持续越久、击退越强）
     * 返回元素反应伤害（不结算，由 onLivingHurt 统一累加）。
     */
    private static float calculateFireAttack(
            LivingEntity attacker,
            LivingEntity target,
            MobEffectInstance fireEffect,
            Set<MobEffect> triggeredReactions) {
        // fire attack code
        double fireMultiplier = AttackMultipliers.of(
                fireEffect.getAmplifier(),
                DragonBlessingsConfig.FIRE_ATTACK_MAX_LEVEL.get());

        // 烈焰标记的等级镜像火龙之力的等级（例：3 级火龙之力 → 3 级烈焰）
        int blazeAmplifier = fireEffect.getAmplifier();

        // 燃烧时间随等级增加（冰克制火：目标身上有冰封标记时不点燃）
        if (!target.hasEffect(ModMobEffects.FROZEN.get())) {
            target.setSecondsOnFire((int) Math.round(5 * fireMultiplier));
        }
        // 添加烈焰标记
        target.addEffect(new MobEffectInstance(
                ModMobEffects.BLAZE.get(),
                (int) Math.round(100 * fireMultiplier), blazeAmplifier));

        // 元素反应：火 + 冰(融化) / 火 + 电(超载)，返回反应伤害（不结算）
        double reactionDamage = ElementalReactionHelper.applyBlazeReactions(target, fireEffect.getAmplifier(),
                triggeredReactions);

        // 击退
        knockback(target, attacker, (float) fireMultiplier);

        return (float) reactionDamage;
    }

    /**
     * 【冰龙】打出冰龙攻击：冰封 + 缓慢 III + 挖掘疲劳 III（持续 10 秒，等级越高持续越久）
     * 冰块渲染由 FrozenEvents 监听 MobEffectEvent 自动同步（任何方式施加 FROZEN 效果都生效）
     * 返回元素反应伤害（不结算，由 onLivingHurt 统一累加）。
     */
    private static float calculateIceAttack(
            LivingEntity target,
            MobEffectInstance iceEffect,
            Set<MobEffect> triggeredReactions) {
        // ice attack code
        double iceMultiplier = AttackMultipliers.of(
                iceEffect.getAmplifier(),
                DragonBlessingsConfig.ICE_ATTACK_MAX_LEVEL.get());

        // 冰封标记的等级镜像冰龙之力的等级（缓慢/挖掘疲劳保持固定 III）
        int frozenAmplifier = iceEffect.getAmplifier();
        int duration = (int) Math.round(200 * iceMultiplier);

        // 冰克制火：直接灭火
        target.clearFire();

        // 添加效果
        target.addEffect(new MobEffectInstance(ModMobEffects.FROZEN.get(), duration, frozenAmplifier));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2));

        // 元素反应：冰 + 火(融化) / 冰 + 电(超导)，返回反应伤害（不结算）
        double reactionDamage = ElementalReactionHelper.applyFrozenReactions(target, iceEffect.getAmplifier(),
                triggeredReactions);

        return (float) reactionDamage;
    }

    /**
     * 【电龙】打出电龙攻击：闪电链（带冷却，防止手速过快导致鬼畜）
     * 返回中心目标伤害（不结算，由 onLivingHurt 统一累加）。
     */
    private static float calculateLightningAttack(
            LivingEntity attacker,
            LivingEntity target,
            MobEffectInstance lightningEffect,
            Set<MobEffect> triggeredReactions) {
        // lightning attack code
        long now = attacker.level().getGameTime();
        long cooldown = DragonBlessingsConfig.CHAIN_COOLDOWN.get();
        Long lastTrigger = LAST_TRIGGER_TIME.get(attacker.getUUID());

        // 冷却过了才触发
        if (lastTrigger == null || now - lastTrigger >= cooldown) {
            LAST_TRIGGER_TIME.put(attacker.getUUID(), now);

            // 中心目标伤害由 createChainLightning 返回，由 onLivingHurt 统一结算
            return ChainLightningHelper.createChainLightning(
                    attacker.level(),
                    target,
                    attacker,
                    lightningEffect.getAmplifier(),
                    triggeredReactions);
        }

        return 0.0F;
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
