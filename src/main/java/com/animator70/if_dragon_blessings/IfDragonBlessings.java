package com.animator70.if_dragon_blessings;

// 我的类
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;
import com.animator70.if_dragon_blessings.init.ModMobEffects;
import com.animator70.if_dragon_blessings.init.ModSounds;
import com.animator70.if_dragon_blessings.network.ModNetwork;

// Forge 类
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 龙之祝福模组入口
 * IfDragonBlessings
 */
@Mod(IfDragonBlessings.MODID)
public class IfDragonBlessings {
    // 模组 ID
    public static final String MODID = "if_dragon_blessings";

    /**
     * 构造函数
     */
    public IfDragonBlessings(FMLJavaModLoadingContext context) {
        // 注册事件总线
        IEventBus modEventBus = context.getModEventBus();

        // 效果注册
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        // 声音注册
        ModSounds.SOUND_EVENTS.register(modEventBus);
        // 网络注册
        ModNetwork.register();

        // 注册通用配置（COMMON）
        context.registerConfig(ModConfig.Type.COMMON, DragonBlessingsConfig.SPEC);
    }
}
