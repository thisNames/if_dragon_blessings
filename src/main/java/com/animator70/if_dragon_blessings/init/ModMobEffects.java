package com.animator70.if_dragon_blessings.init;

import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.effect.FireAttackEffect;
import com.animator70.if_dragon_blessings.effect.FrozenEffect;
import com.animator70.if_dragon_blessings.effect.IceAttackEffect;
import com.animator70.if_dragon_blessings.effect.LightningAttackEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, IfDragonBlessings.MODID);

    // 施加给攻击者的“攻击方式”buff：拥有对应效果时，攻击触发对应方式
    public static final RegistryObject<MobEffect> FIRE_ATTACK = MOB_EFFECTS.register("fire_attack", FireAttackEffect::new);
    public static final RegistryObject<MobEffect> ICE_ATTACK = MOB_EFFECTS.register("ice_attack", IceAttackEffect::new);
    public static final RegistryObject<MobEffect> LIGHTNING_ATTACK = MOB_EFFECTS.register("lightning_attack", LightningAttackEffect::new);

    // 施加给目标的状态：冰封（语义标记，减速/挖掘疲劳由原版效果承担）
    public static final RegistryObject<MobEffect> FROZEN = MOB_EFFECTS.register("frozen", FrozenEffect::new);
}
