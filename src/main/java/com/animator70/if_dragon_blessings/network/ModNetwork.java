package com.animator70.if_dragon_blessings.network;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;

// Minecraft 类
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

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

        CHANNEL.messageBuilder(ChainLightningPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ChainLightningPacket::encode)
                .decoder(ChainLightningPacket::new)
                .consumerMainThread(ChainLightningPacket::handle)
                .add();
    }

    public static void sendChainLightning(ServerLevel level, BlockPos center, List<Integer> entityIds) {
        CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(
                center.getX() + 0.5,
                center.getY() + 0.5,
                center.getZ() + 0.5,
                64.0,
                level.dimension())),
                new ChainLightningPacket(entityIds));
    }
}
