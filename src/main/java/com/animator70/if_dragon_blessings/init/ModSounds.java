package com.animator70.if_dragon_blessings.init;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;

// Minecraft 类
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

// Forge 类
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 音效
 * ModSounds
 */
public class ModSounds {
    // 注册模组音频 ID
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS,
            IfDragonBlessings.MODID);

    // 【电龙】雷电打击音效
    @SuppressWarnings("removal")
    public static final RegistryObject<SoundEvent> LIGHTNING_STRIKE = SOUND_EVENTS.register(
            "lightning_strike",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(IfDragonBlessings.MODID, "lightning_strike")));

    // 【元素反应】超导音效（三个音频随机播放）
    @SuppressWarnings("removal")
    public static final RegistryObject<SoundEvent> SUPERCONDUCT = SOUND_EVENTS.register(
            "superconduct",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(IfDragonBlessings.MODID, "superconduct")));
}
