package com.animator70.if_dragon_blessings.init;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.effect.FireAttackEffect;
import com.animator70.if_dragon_blessings.effect.IceAttackEffect;
import com.animator70.if_dragon_blessings.effect.LightningAttackEffect;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;

// Forge 类
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 效果
 * ModMobEffects
 */
public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(
            ForgeRegistries.MOB_EFFECTS,
            IfDragonBlessings.MODID);

    // 施加给攻击者的“攻击方式”buff：拥有对应效果时，攻击触发对应方式
    // 火力的祝福
    public static final RegistryObject<MobEffect> FIRE_ATTACK = MOB_EFFECTS.register(
            "fire_attack",
            FireAttackEffect::new);

    // 冰龙的恩惠
    public static final RegistryObject<MobEffect> ICE_ATTACK = MOB_EFFECTS.register(
            "ice_attack",
            IceAttackEffect::new);

    // 电龙的庇佑
    public static final RegistryObject<MobEffect> LIGHTNING_ATTACK = MOB_EFFECTS.register(
            "lightning_attack",
            LightningAttackEffect::new);
}
