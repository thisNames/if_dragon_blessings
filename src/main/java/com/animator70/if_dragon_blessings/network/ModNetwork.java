package com.animator70.if_dragon_blessings.network;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;

// Minecraft 类
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

// Forge 类
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

// Java 类
import java.util.List;

/**
 * 网络
 * ModNetwork
 */
public class ModNetwork {
    // 协议版本
    private static final String PROTOCOL_VERSION = "1";

    @SuppressWarnings("removal")
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(IfDragonBlessings.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        int id = 0;

        // 闪电链渲染包
        CHANNEL.messageBuilder(ChainLightningPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ChainLightningPacket::encode)
                .decoder(ChainLightningPacket::new)
                .consumerMainThread(ChainLightningPacket::handle)
                .add();

        // 配置同步包
        CHANNEL.messageBuilder(ConfigSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigSyncPacket::encode)
                .decoder(ConfigSyncPacket::new)
                .consumerMainThread(ConfigSyncPacket::handle)
                .add();

        // 冰冻状态同步包
        CHANNEL.messageBuilder(SetFrozenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SetFrozenPacket::encode)
                .decoder(SetFrozenPacket::new)
                .consumerMainThread(SetFrozenPacket::handle)
                .add();
    }

    public static void sendChainLightning(ServerLevel level, BlockPos center, List<Integer> entityIds) {
        CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(
                center.getX() + 0.5,
                center.getY() + 0.5,
                center.getZ() + 0.5,
                DragonBlessingsConfig.CHAIN_SYNC_DISTANCE.get(),
                level.dimension())),
                new ChainLightningPacket(entityIds));
    }

    /**
     * 把服务端的 COMMON 配置发送给指定玩家
     */
    public static void sendConfigSync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ConfigSyncPacket());
    }

    /**
     * 把冰冻状态同步给所有追踪该实体的客户端 + 实体自己
     */
    public static void sendFrozen(Entity entity, int frozenTicks) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new SetFrozenPacket(entity.getId(), frozenTicks));
    }
}
