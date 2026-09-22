package com.animator70.if_dragon_blessings.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 冰封：冰龙攻击施加的状态效果。
 * 减速与挖掘疲劳由原版缓慢/挖掘疲劳效果承担，此效果作为“目标被冰封”的语义标记。
 */
public class FrozenEffect extends MobEffect {

    public FrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FC7FF);
    }
}
