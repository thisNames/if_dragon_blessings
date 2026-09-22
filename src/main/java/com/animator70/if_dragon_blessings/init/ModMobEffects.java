package com.animator70.if_dragon_blessings.init;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.effect.BlazeEffect;
import com.animator70.if_dragon_blessings.effect.FireAttackEffect;
import com.animator70.if_dragon_blessings.effect.FrozenEffect;
import com.animator70.if_dragon_blessings.effect.IceAttackEffect;
import com.animator70.if_dragon_blessings.effect.LightningAttackEffect;
import com.animator70.if_dragon_blessings.effect.ShockedEffect;

// Minecraft 类
import net.minecraft.world.effect.MobEffect;

// Forge 类
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 状态效果注册表
 * ModMobEffects
 */
public class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
            IfDragonBlessings.MODID);

    // 施加给攻击者的“攻击方式”buff：拥有对应效果时，攻击触发对应方式
    public static final RegistryObject<MobEffect> FIRE_ATTACK = MOB_EFFECTS.register(
            "fire_attack",
            FireAttackEffect::new);

    public static final RegistryObject<MobEffect> ICE_ATTACK = MOB_EFFECTS.register("ice_attack", IceAttackEffect::new);

    public static final RegistryObject<MobEffect> LIGHTNING_ATTACK = MOB_EFFECTS.register(
            "lightning_attack",
            LightningAttackEffect::new);

    // 施加给目标的状态：冰封（语义标记，减速/挖掘疲劳由原版效果承担）
    public static final RegistryObject<MobEffect> FROZEN = MOB_EFFECTS.register("frozen", FrozenEffect::new);

    // 施加给目标的状态：烈焰（语义标记，实际点燃由原版 setSecondsOnFire 承担）
    public static final RegistryObject<MobEffect> BLAZE = MOB_EFFECTS.register("blaze", BlazeEffect::new);

    // 施加给目标的状态：感电（定身，核心效果，替代原模组的麻痹）
    public static final RegistryObject<MobEffect> SHOCKED = MOB_EFFECTS.register("shocked", ShockedEffect::new);
}
