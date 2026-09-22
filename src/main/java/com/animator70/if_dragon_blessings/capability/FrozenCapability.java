package com.animator70.if_dragon_blessings.capability;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;

// Minecraft 类
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// Forge 类
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

// JetBrains 注解
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 【冰龙】冰冻状态：冰龙攻击施加的“剩余冰冻 tick”。
 * 状态存 Capability，通过自定义网络包（SetFrozenPacket）同步到所有追踪者，
 * 不依赖 vanilla 的 MobEffect 同步（后者在 1.20.1 对远程实体同步不可靠）。
 *
 * 注意：冰冻是临时状态（10 秒），因此**不做持久化**（不实现 ICapabilitySerializable）。
 * 实体跨 chunk 卸载后，冰冻状态直接丢弃——这符合“临时 debuff”的语义，
 * 也避免卸载期间 frozenTicks 不递减导致的“冰冻暂停”问题。
 */
public class FrozenCapability {

    // 能力
    public static final Capability<FrozenCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    // 剩余冰冻 tick（0 表示未冰冻）
    private int frozenTicks = 0;

    /**
     * 剩余冰冻 tick
     */
    public int getFrozenTicks() {
        return this.frozenTicks;
    }

    /**
     * 设置剩余冰冻 tick（负值会被钳制为 0）
     */
    public void setFrozenTicks(int ticks) {
        this.frozenTicks = Math.max(0, ticks);
    }

    /**
     * 是否处于冰冻状态
     */
    public boolean isFrozen() {
        return this.frozenTicks > 0;
    }

    /**
     * 获取实体的冰冻 Capability
     */
    public static LazyOptional<FrozenCapability> get(Entity entity) {
        return entity.getCapability(CAPABILITY);
    }

    /**
     * 给每个 LivingEntity 动态附加冰冻 Capability
     */
    @SuppressWarnings("removal")
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(new ResourceLocation(IfDragonBlessings.MODID, "frozen"), new Provider());
        }
    }

    /**
     * 能力提供者：把 {@link FrozenCapability} 暴露给 Forge（不持久化，冰冻是临时状态）
     */
    public static class Provider implements ICapabilityProvider {
        private final FrozenCapability instance = new FrozenCapability();
        private final LazyOptional<FrozenCapability> lazy = LazyOptional.of(() -> this.instance);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, this.lazy);
        }
    }
}
