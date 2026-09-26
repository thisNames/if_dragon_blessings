package com.animator70.if_dragon_blessings.effect;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 【火龙】烈焰：火龙攻击施加的状态效果。
 * 作为“目标被点燃”的语义标记，实际点燃由原版 setSecondsOnFire 承担。
 */
public class BlazeEffect extends MobEffect {

    public BlazeEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFCB2E);
    }
}
