package com.animator70.if_dragon_blessings.network;

// 我的类
import com.animator70.if_dragon_blessings.client.particle.LightningChainParticle;

// Minecraft 类
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

// Java 类
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;

/**
 * 闪电链粒子包：在客户端播放闪电链粒子效果
 * ChainLightningPacket
 */
public class ChainLightningPacket {
    private final List<Integer> entityIds;

    public ChainLightningPacket(List<Integer> entityIds) {
        this.entityIds = entityIds;
    }

    public ChainLightningPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.entityIds = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            this.entityIds.add(buf.readInt());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.entityIds.size());

        for (int id : this.entityIds) {
            buf.writeInt(id);
        }
    }

    /**
     * 处理包：在客户端播放闪电链粒子效果
     */
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;

            // 至少两个实体：中心实体 + 目标实体
            if (level == null || this.entityIds.size() < 2) {
                return;
            }

            // 第一个实体是闪电链的中心，从中心向其余每个目标放射状发散
            Entity center = level.getEntity(this.entityIds.get(0));

            // 中心实体不存在，可能被移除，忽略
            if (center == null) {
                return;
            }

            // 中心实体位置 + 半身高
            Vec3 centerPos = center.position().add(0.0D, center.getBbHeight() / 2.0D, 0.0D);

            // 从第二个实体开始，依次放射状发散
            for (int i = 1; i < this.entityIds.size(); i++) {
                Entity entity = level.getEntity(this.entityIds.get(i));

                if (entity == null) {
                    continue;
                }

                // 实体位置 + 半身高
                Vec3 pos = entity.position().add(0.0D, entity.getBbHeight() / 2.0D, 0.0D);

                // 在实体位置放射状发散
                mc.particleEngine.add(new LightningChainParticle(level, centerPos, pos));
            }
        });

        // 处理完成，不再继续处理其他包
        context.setPacketHandled(true);
    }
}
