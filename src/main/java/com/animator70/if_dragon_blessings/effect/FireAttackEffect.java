package com.animator70.if_dragon_blessings.effect;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 火力的祝福：作为攻击方式的标记 buff，本身不产生副作用
 */
public class FireAttackEffect extends MobEffect {
    public FireAttackEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6A00);
    }
}
