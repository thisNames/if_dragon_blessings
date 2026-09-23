package com.animator70.if_dragon_blessings.api;

// 我的类
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;
import com.animator70.if_dragon_blessings.init.ModMobEffects;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 元素反应：集中处理三种元素（冰 / 火 / 电）互相触发的反应。
 *
 * 反应规则（顺序无关）：
 * - 冰 + 火 = 融化（Melt）
 * - 冰 + 电 = 超导（Superconduct）
 * - 火 + 电 = 超载（Overload）
 * 
 * 伤害公式：单个反应伤害 = 反应基础伤害 × (施加元素等级 × 已有元素等级 × 0.1)。
 * 等级按 1 起算（I 级 = 1），默认上限 3 级时乘积最高 3×3=9，即 90%。
 * 反应基础伤害为代码内常量（非配置），仅「是否开启」暴露为配置总开关。
 *
 * 伤害合并：同一次攻击可能同时触发多个反应（如目标同时带冰、火，电击触发超导+超载），
 * 本类只负责「打标记 + 累加伤害」，由调用方把累加结果一次性 hurt，
 * 避免原版无敌帧吞掉同一 tick 内的多段叠加伤害。伤害归属由调用方用 indirectMagic(attacker, null) 决定。
 *
 * 触发方式：攻击施加某个元素后，调用对应的 {@code applyXxxReactions}，
 * 检查目标身上是否已有另外两种元素，有则各自触发对应反应（可同时触发多个）。
 */
public final class ElementalReactionHelper {
    // 反应标记效果持续时长（tick）：仅作视觉标记，2 秒
    private static final int MARKER_DURATION = 40;
    // 反应基础伤害：单个反应伤害 = 基础 × (等级相乘 × 0.1)。默认 20 → 单反应伤害 2.0（1×1，1 颗心）~ 18.0（3×3，9 颗心）
    private static final double BASE_DAMAGE = 20.0D;

    private ElementalReactionHelper() {
    }

    /**
     * 冰（frozen）被施加：检查火（→融化）、电（→超导），返回总反应伤害（不含结算）。
     */
    public static double applyFrozenReactions(LivingEntity target, int iceAmplifier) {
        double total = 0.0D;
        total += react(target, iceAmplifier, ModMobEffects.BLAZE.get(), ModMobEffects.MELT.get());
        total += react(target, iceAmplifier, ModMobEffects.SHOCKED.get(), ModMobEffects.SUPERCONDUCT.get());
        return total;
    }

    /**
     * 火（blaze）被施加：检查冰（→融化）、电（→超载），返回总反应伤害（不含结算）。
     */
    public static double applyBlazeReactions(LivingEntity target, int fireAmplifier) {
        double total = 0.0D;
        total += react(target, fireAmplifier, ModMobEffects.FROZEN.get(), ModMobEffects.MELT.get());
        total += react(target, fireAmplifier, ModMobEffects.SHOCKED.get(), ModMobEffects.OVERLOAD.get());
        return total;
    }

    /**
     * 电（shocked）被施加：检查冰（→超导）、火（→超载），返回总反应伤害（不含结算）。
     */
    public static double applyShockedReactions(LivingEntity target, int lightningAmplifier) {
        double total = 0.0D;
        total += react(target, lightningAmplifier, ModMobEffects.FROZEN.get(), ModMobEffects.SUPERCONDUCT.get());
        total += react(target, lightningAmplifier, ModMobEffects.BLAZE.get(), ModMobEffects.OVERLOAD.get());
        return total;
    }

    /**
     * 单个反应：目标身上存在 existingEffect 时，打上反应标记并返回该反应伤害，否则返回 0。
     * 不在此处结算伤害——伤害由调用方累加后一次性 hurt，避免无敌帧吞掉多段叠加。
     *
     * @param target           目标
     * @param appliedAmplifier 刚施加元素的等级（0 起，I 级 = 0）
     * @param existingEffect   目标身上需要已存在的元素效果
     * @param reactionMarker   反应触发后打上的标记效果
     * @return 该反应的额外伤害（未触发返回 0）
     */
    private static double react(
            LivingEntity target,
            int appliedAmplifier,
            MobEffect existingEffect,
            MobEffect reactionMarker) {
        // code
        // 是否启用了元素反应
        if (!DragonBlessingsConfig.REACTION_ENABLED.get()) {
            return 0.0D;
        }

        // 是否存在已有的效果
        MobEffectInstance existing = target.getEffect(existingEffect);
        if (existing == null) {
            return 0.0D;
        }

        // 等级 1 起：I 级 = 1，乘积最高 3×3=9 → 90%
        int appliedLevel = appliedAmplifier + 1;
        int existingLevel = existing.getAmplifier() + 1;
        double percentage = appliedLevel * existingLevel * 0.1D;

        // 打标记（固定 0 级）
        target.addEffect(new MobEffectInstance(reactionMarker, MARKER_DURATION, 0));

        return BASE_DAMAGE * percentage;
    }
}
