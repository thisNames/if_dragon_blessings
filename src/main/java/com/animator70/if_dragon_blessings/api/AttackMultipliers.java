package com.animator70.if_dragon_blessings.api;

// Minecraft 类
import net.minecraft.world.effect.MobEffectInstance;

// JetBrains 注解
import org.jetbrains.annotations.Nullable;

/**
 * 龙之力攻击等级换算工具：等级越高效果越强（每级 +25%）
 * 火 / 冰 / 电三种攻击共用同一套倍率算法，避免多处实现不一致
 */
public final class AttackMultipliers {
    private AttackMultipliers() {
    }

    /**
     * 根据 buff 等级（amplifier）计算效果强度系数。
     *
     * @param amplifier buff 的 amplifier（0 表示 I 级）
     * @param maxLevel  该 buff 的最大有效等级（来自配置），超出部分按上限计算
     * @return 强度系数（1.0 起步，每级 +0.25）
     */
    public static double of(int amplifier, int maxLevel) {
        return 1.0D + Math.min(amplifier, maxLevel) * 0.25D;
    }

    /**
     * 根据效果实例计算强度系数（实例可能为 null，按 0 级处理）
     */
    public static double of(@Nullable MobEffectInstance effect, int maxLevel) {
        return of(effect != null ? effect.getAmplifier() : 0, maxLevel);
    }
}
