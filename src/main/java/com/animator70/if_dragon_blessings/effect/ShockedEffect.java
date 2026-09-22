package com.animator70.if_dragon_blessings.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 感电：电龙攻击施加的状态效果
 * 拥有感电效果的实体无法移动（定身）：每 tick 清零水平速度，保留向下速度（允许受重力下落）
 * 这是模组的核心效果，替代原模组依赖的 Lycanites 麻痹效果
 */
public class ShockedEffect extends MobEffect {
    public ShockedEffect() {
        super(MobEffectCategory.HARMFUL, 0xE5C100);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 定身：清零水平移动，保留向下速度（允许正常下落，不清空重力）
        Vec3 motion = entity.getDeltaMovement();

        entity.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每 tick 都应用，确保持续定身
        return true;
    }
}
