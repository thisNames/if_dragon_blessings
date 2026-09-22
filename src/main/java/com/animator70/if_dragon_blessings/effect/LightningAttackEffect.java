package com.animator70.if_dragon_blessings.effect;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 电龙的庇佑：作为攻击方式的标记 buff，本身不产生副作用
 */
public class LightningAttackEffect extends MobEffect {
    public LightningAttackEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xA929EE);
    }
}
