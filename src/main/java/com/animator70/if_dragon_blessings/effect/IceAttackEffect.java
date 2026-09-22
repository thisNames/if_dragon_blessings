package com.animator70.if_dragon_blessings.effect;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 冰龙的恩惠：作为攻击方式的标记 buff，本身不产生副作用
 */
public class IceAttackEffect extends MobEffect {
    public IceAttackEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7FC7FF);
    }
}
