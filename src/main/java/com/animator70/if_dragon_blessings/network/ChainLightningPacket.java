package com.animator70.if_dragon_blessings.network;

// 我的类
import com.animator70.if_dragon_blessings.client.particle.LightningChainParticle;

// Minecraft 类
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

// Java 类
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;

/**
 * 【电龙】闪电链粒子包：在客户端播放闪电链粒子效果。
 * 直接携带各连接点的坐标（而非实体 ID），避免实体被转换/移除后按 ID 找不到、闪电视觉丢失。
 */
public class ChainLightningPacket {
    private final List<Vec3> positions;

    public ChainLightningPacket(List<Vec3> positions) {
        this.positions = positions;
    }

    public ChainLightningPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.positions = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            this.positions.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.positions.size());

        for (Vec3 pos : this.positions) {
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
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

            // 至少两个点：中心点 + 目标点
            if (level == null || this.positions.size() < 2) {
                return;
            }

            // 第一个坐标是闪电链的中心，从中心向其余每个点放射状发散
            Vec3 centerPos = this.positions.get(0);

            for (int i = 1; i < this.positions.size(); i++) {
                mc.particleEngine.add(new LightningChainParticle(level, centerPos, this.positions.get(i)));
            }
        });

        context.setPacketHandled(true);
    }
}
