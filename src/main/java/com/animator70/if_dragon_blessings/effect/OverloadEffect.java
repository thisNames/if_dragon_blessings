package com.animator70.if_dragon_blessings.effect;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 【元素反应】超载：火 + 电 触发的反应标记效果，本身不产生副作用。
 * 实际额外伤害由 {@code ElementalReactionHelper} 在触发时直接结算。
 */
public class OverloadEffect extends MobEffect {
    public OverloadEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF8C3A);
    }
}
