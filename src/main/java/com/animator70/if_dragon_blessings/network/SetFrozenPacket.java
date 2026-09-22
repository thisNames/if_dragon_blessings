package com.animator70.if_dragon_blessings.network;

// 我的类
import com.animator70.if_dragon_blessings.capability.FrozenCapability;

// Minecraft 类
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;

// Forge 类
import net.minecraftforge.network.NetworkEvent;

// Java 类
import java.util.function.Supplier;

/**
 * 【冰龙】冰冻状态同步包（服务端 → 客户端）
 * 客户端收到后，把实体的冰冻 tick 写入其 Capability（供渲染层读取）。
 */
public class SetFrozenPacket {
    // 实体 id
    private final int entityId;
    // 剩余冰冻 tick（0 表示解除冰冻）
    private final int frozenTicks;

    public SetFrozenPacket(int entityId, int frozenTicks) {
        this.entityId = entityId;
        this.frozenTicks = frozenTicks;
    }

    public SetFrozenPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.frozenTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.frozenTicks);
    }

    /**
     * 客户端接收：按实体 id 找到实体，把冰冻 tick 写入其 Capability。
     */
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();

            if (mc.level != null) {
                Entity entity = mc.level.getEntity(this.entityId);

                if (entity != null) {
                    FrozenCapability.get(entity).ifPresent(cap -> cap.setFrozenTicks(this.frozenTicks));
                }
            }
        });

        context.setPacketHandled(true);
    }
}
