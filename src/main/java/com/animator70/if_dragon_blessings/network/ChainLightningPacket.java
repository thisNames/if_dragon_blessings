package com.animator70.if_dragon_blessings.network;

// 我的类
import com.animator70.if_dragon_blessings.client.render.ClientLightningRenderer;

// Minecraft 类
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
    @SuppressWarnings("deprecation")
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        // enqueueWork 形参：Runnable（void 的方法）
        // DistExecutor.runWhenOn（就是一个 void 的方法）
        // runWhenOn 形参 1：Dist
        // runWhenOn 形参 2：是一个 Supplier<Runnable>，也就是说一个函数，此函数的返回一个函数，返回值为 Runnable
        // 最终：
        context.enqueueWork(() -> {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> {
                return () -> {
                    ClientLightningRenderer.spawn(this.positions);
                };
            });
        });

        context.setPacketHandled(true);
    }
}
