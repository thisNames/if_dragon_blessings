package com.animator70.if_dragon_blessings.event;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.capability.FrozenCapability;
import com.animator70.if_dragon_blessings.init.ModMobEffects;
import com.animator70.if_dragon_blessings.network.ModNetwork;

// Minecraft 类
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// Forge 类
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 【冰龙】冰冻状态相关事件：
 * 1. 给所有 LivingEntity 附加冰冻 Capability
 * 2. 监听 FROZEN 效果的生命周期（添加/到期/移除），自动同步冰块渲染状态
 * 冰块只是装饰：用“效果添加时触发”即可，存档重载后不补发（丢就丢了，不影响任何实际效果）
 */
@Mod.EventBusSubscriber(modid = IfDragonBlessings.MODID)
public class FrozenEvents {

    /**
     * 给每个 LivingEntity 动态附加冰冻 Capability
     */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        FrozenCapability.attach(event);
    }

    /**
     * FROZEN 效果添加（攻击 / 命令 / 其他模组）：设 Capability + 同步给所有追踪者
     */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide) {
            return;
        }

        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModMobEffects.FROZEN.get()) {
            int ticks = event.getEffectInstance().getDuration();

            FrozenCapability.get(entity).ifPresent(cap -> cap.setFrozenTicks(ticks));
            ModNetwork.sendFrozen(entity, ticks);
        }
    }

    /**
     * FROZEN 效果自然到期：清 Capability + 同步解除
     */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide) {
            return;
        }

        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModMobEffects.FROZEN.get()) {
            FrozenCapability.get(entity).ifPresent(cap -> cap.setFrozenTicks(0));
            ModNetwork.sendFrozen(entity, 0);

            spawnBreakEffects(entity);
        }
    }

    /**
     * FROZEN 效果被移除（牛奶 / 命令 / 死亡等）：清 Capability + 同步解除
     */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide) {
            return;
        }

        if (event.getEffect() == ModMobEffects.FROZEN.get()) {
            FrozenCapability.get(entity).ifPresent(cap -> cap.setFrozenTicks(0));
            ModNetwork.sendFrozen(entity, 0);

            spawnBreakEffects(entity);
        }
    }

    /**
     * 播放原版碎冰音效 + 生成原版冰块碎裂粒子（效果消失时调用，模拟冰块碎裂融化）
     */
    private static void spawnBreakEffects(LivingEntity entity) {
        // 原版玻璃碎裂音效（碎冰）
        entity.playSound(SoundEvents.GLASS_BREAK, 1.0F, 1.2F);

        // 原版方块碎裂粒子 + 冰块，在实体周围生成 8 粒
        if (entity.level() instanceof ServerLevel serverLevel) {
            BlockState iceState = Blocks.ICE.defaultBlockState();
            double x = entity.getX();
            double y = entity.getY() + entity.getBbHeight() / 2.0D;
            double z = entity.getZ();
            double dx = entity.getBbWidth() / 2.0D;
            double dy = entity.getBbHeight() / 2.0D;
            double dz = entity.getBbWidth() / 2.0D;

            // 发送粒子
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, iceState),
                    x, y, z, 8, dx, dy, dz, 0.1D);
        }
    }
}
