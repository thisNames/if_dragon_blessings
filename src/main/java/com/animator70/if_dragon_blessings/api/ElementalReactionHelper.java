package com.animator70.if_dragon_blessings.api;

// 我的类
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;
import com.animator70.if_dragon_blessings.init.ModMobEffects;
import com.animator70.if_dragon_blessings.init.ModSounds;

// Minecraft 类
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

// 第三方库
import org.joml.Vector3f;

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
 * 冷却/去重：用反应标记自身充当冷却计时器——目标已有该反应标记时不再触发，
 * 既防止三效果同一次攻击内重复触发，也防止手速过快反复刷伤害。
 * 反应标记（冷却）随等级增加：等级越高伤害越高，冷却也越长，两者成正比，保持元素机制平衡。
 *
 * 伤害合并：本类只负责「打标记 + 累加伤害」，由调用方把累加结果一次性 hurt，
 * 避免原版无敌帧吞掉同一 tick 内的多段叠加伤害。伤害归属由调用方用 indirectMagic(attacker, null) 决定。
 */
public final class ElementalReactionHelper {
    // 元素标记（冰封/烈焰/感电）的固定时长（tick）：3 秒。决定元素反应的触发窗口。
    public static final int ELEMENT_MARKER_DURATION = 60;
    // 反应标记（融化/超导/超载）的基础时长（tick）：2 秒。随等级增加（冷却随等级，平衡高伤害）。
    private static final int REACTION_BASE_DURATION = 40;
    // 冷却每级增量系数：冷却 = 基础 × (1 + 有效等级 × 0.5)
    private static final double REACTION_COOLDOWN_PER_LEVEL = 0.5D;
    // 反应基础伤害：单个反应伤害 = 基础 × (等级相乘 × 0.1)。默认 10 → 单反应伤害 1.0（1×1，半颗心）~ 9.0（3×3，4.5 颗心）
    private static final double BASE_DAMAGE = 10.0D;

    private ElementalReactionHelper() {
    }

    /**
     * 冰（frozen）被施加：检查火（→融化）、电（→超导），返回总反应伤害（不含结算）。
     */
    public static double applyFrozenReactions(LivingEntity target, int iceAmplifier) {
        int effectiveLevel = Math.min(iceAmplifier, DragonBlessingsConfig.ICE_ATTACK_MAX_LEVEL.get());
        double total = 0.0D;

        total += react(
                target,
                iceAmplifier,
                ModMobEffects.BLAZE.get(),
                ModMobEffects.MELT.get(), effectiveLevel);
        total += react(
                target,
                iceAmplifier,
                ModMobEffects.SHOCKED.get(),
                ModMobEffects.SUPERCONDUCT.get(),
                effectiveLevel);

        return total;
    }

    /**
     * 火（blaze）被施加：检查冰（→融化）、电（→超载），返回总反应伤害（不含结算）。
     */
    public static double applyBlazeReactions(LivingEntity target, int fireAmplifier) {
        int effectiveLevel = Math.min(fireAmplifier, DragonBlessingsConfig.FIRE_ATTACK_MAX_LEVEL.get());
        double total = 0.0D;

        total += react(
                target,
                fireAmplifier,
                ModMobEffects.FROZEN.get(),
                ModMobEffects.MELT.get(),
                effectiveLevel);

        total += react(
                target,
                fireAmplifier,
                ModMobEffects.SHOCKED.get(),
                ModMobEffects.OVERLOAD.get(),
                effectiveLevel);

        return total;
    }

    /**
     * 电（shocked）被施加：检查冰（→超导）、火（→超载），返回总反应伤害（不含结算）。
     */
    public static double applyShockedReactions(LivingEntity target, int lightningAmplifier) {
        int effectiveLevel = Math.min(lightningAmplifier, DragonBlessingsConfig.LIGHTNING_ATTACK_MAX_LEVEL.get());
        double total = 0.0D;

        total += react(
                target,
                lightningAmplifier,
                ModMobEffects.FROZEN.get(),
                ModMobEffects.SUPERCONDUCT.get(),
                effectiveLevel);
        total += react(
                target,
                lightningAmplifier,
                ModMobEffects.BLAZE.get(),
                ModMobEffects.OVERLOAD.get(),
                effectiveLevel);

        return total;
    }

    /**
     * 单个反应：目标身上存在 existingEffect 时，打上反应标记并返回该反应伤害，否则返回 0。
     * 不在此处结算伤害——伤害由调用方累加后一次性 hurt，避免无敌帧吞掉多段叠加。
     *
     * @param target           目标
     * @param appliedAmplifier 刚施加元素的等级（0 起，I 级 = 0），用于计算伤害
     * @param existingEffect   目标身上需要已存在的元素效果
     * @param reactionMarker   反应触发后打上的标记效果（同时充当冷却计时器）
     * @param effectiveLevel   攻击者有效等级（封顶后），用于计算冷却时长
     * @return 该反应的额外伤害（未触发或冷却中返回 0）
     */
    private static double react(
            LivingEntity target,
            int appliedAmplifier,
            MobEffect existingEffect,
            MobEffect reactionMarker,
            int effectiveLevel) {
        // 是否启用了元素反应
        if (!DragonBlessingsConfig.REACTION_ENABLED.get()) {
            return 0.0D;
        }

        // 冷却/去重：目标已有该反应标记，不再触发
        if (target.hasEffect(reactionMarker)) {
            return 0.0D;
        }

        // 是否存在已有的元素标记（冰/火/电）
        MobEffectInstance existing = target.getEffect(existingEffect);
        if (existing == null) {
            return 0.0D;
        }

        // 等级 1 起：I 级 = 1，乘积最高 3×3=9 → 90%
        int appliedLevel = appliedAmplifier + 1;
        int existingLevel = existing.getAmplifier() + 1;
        double percentage = appliedLevel * existingLevel * 0.1D;

        // 打反应标记（冷却随等级：等级越高冷却越长，与伤害成正比）
        int cooldown = (int) (REACTION_BASE_DURATION * (1.0D + effectiveLevel * REACTION_COOLDOWN_PER_LEVEL));
        target.addEffect(new MobEffectInstance(reactionMarker, cooldown, 0));

        // 反应特效（只有新反应才会走到这里，天然防抖）
        spawnReactionParticles(target, reactionMarker);

        return BASE_DAMAGE * percentage;
    }

    /**
     * 在目标身上生成元素反应的粒子特效与音效（仅服务端）。
     * 三种反应各配一组粒子 + 音效，视觉上可区分。
     */
    private static void spawnReactionParticles(LivingEntity target, MobEffect reactionMarker) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2.0D;
        double z = target.getZ();

        if (reactionMarker == ModMobEffects.MELT.get()) {
            // 融化（冰+火）：白色水汽 + 火焰
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 8, 0.5D, 0.5D, 0.5D, 0.1D);
            serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.4D, 0.4D, 0.4D, 0.1D);

            target.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.0F);
        } else if (reactionMarker == ModMobEffects.SUPERCONDUCT.get()) {
            // 超导（冰+电）：亮紫色粒子 + 大冰晶
            serverLevel.sendParticles(new DustParticleOptions(
                    new Vector3f(0.706F, 0.302F, 1.0F), 2.5F), x, y, z, 15, 1.0D, 1.0D, 1.0D, 0.25D);

            serverLevel.sendParticles(new DustParticleOptions(
                    new Vector3f(0.8F, 0.9F, 1.0F), 3.5F), x, y, z, 10, 1.0D, 1.0D, 1.0D, 0.15D);

            // target.playSound(ModSounds.SUPERCONDUCT.get(), 0.4F, 1.0F);
        } else if (reactionMarker == ModMobEffects.OVERLOAD.get()) {
            // 超载（火+电）：爆炸（改小）+ 火焰（加量）
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4, 0.3D, 0.3D, 0.3D, 0.1D);
            serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 14, 0.5D, 0.5D, 0.5D, 0.2D);

            target.playSound(SoundEvents.GENERIC_EXPLODE, 0.7F, 1.0F);
        }
    }
}
