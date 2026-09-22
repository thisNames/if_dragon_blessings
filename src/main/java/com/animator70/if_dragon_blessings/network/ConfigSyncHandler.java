package com.animator70.if_dragon_blessings.network;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;

// Minecraft 类
import net.minecraft.server.level.ServerPlayer;

// Forge 类
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 配置同步：玩家登录时，服务端把 COMMON 配置发送给该玩家
 * 实现“全服权威统一”——所有客户端都使用服务端的配置值
 */
@Mod.EventBusSubscriber(modid = IfDragonBlessings.MODID)
public class ConfigSyncHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendConfigSync(player);
        }
    }
}
