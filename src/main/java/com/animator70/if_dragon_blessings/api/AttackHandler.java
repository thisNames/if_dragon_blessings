package com.animator70.if_dragon_blessings.api;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;
import com.animator70.if_dragon_blessings.init.ModMobEffects;

// Minecraft 类
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// Forge 类
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Java 类
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 攻击方式与物品无关：只要攻击者持有对应的“攻击方式”buff
 * 命中目标时就触发对应的攻击特效（火龙 / 冰龙 / 电龙 三种攻击方式）
 */
@Mod.EventBusSubscriber(modid = IfDragonBlessings.MODID)
public class AttackHandler {
    // 记录每个攻击者上次触发闪电链的世界时间（tick），用于冷却，防止手速过快导致闪电链鬼畜。
    // 实体离开世界时（EntityLeaveLevelEvent）自动清理，玩家与非玩家一视同仁，不会累积。
    private static final Map<UUID, Long> LAST_TRIGGER_TIME = new HashMap<>();

    /**
     * 攻击事件：判断攻击者是否持有对应攻击方式效果，持有才触发对应攻击。
     * 「是否有效果」的判断收敛在这里，各攻击方法只负责“打出攻击”，
     * 因此攻击方式不再与效果强绑定（将来可从物品等其他来源触发）。
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        // 攻击者必须是活体实体
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }
        // 客户端不处理
        if (attacker.level().isClientSide) {
            return;
        }

        // 获取实体目标
        LivingEntity target = event.getEntity();

        // 火龙：持有火龙之力才打出火龙攻击
        MobEffectInstance fireEffect = attacker.getEffect(ModMobEffects.FIRE_ATTACK.get());
        if (fireEffect != null) {
            performFireAttack(attacker, target, fireEffect);
        }

        // 冰龙：持有冰龙之力才打出冰龙攻击
        MobEffectInstance iceEffect = attacker.getEffect(ModMobEffects.ICE_ATTACK.get());
        if (iceEffect != null) {
            performIceAttack(attacker, target, iceEffect);
        }

        // 电龙：持有电龙之力才打出电龙攻击
        MobEffectInstance lightningEffect = attacker.getEffect(ModMobEffects.LIGHTNING_ATTACK.get());
        if (lightningEffect != null) {
            performLightningAttack(attacker, target, lightningEffect);
        }
    }

    /**
     * 【火龙】打出火龙攻击：点燃 + 烈焰标记 + 击退（等级越高持续越久、击退越强）
     */
    private static void performFireAttack(LivingEntity attacker, LivingEntity target, MobEffectInstance fireEffect) {
        double fireMultiplier = AttackMultipliers.of(
                fireEffect.getAmplifier(),
                DragonBlessingsConfig.FIRE_ATTACK_MAX_LEVEL.get());

        // 烈焰标记的等级镜像火龙之力的等级（例：3 级火龙之力 → 3 级烈焰）
        int blazeAmplifier = fireEffect.getAmplifier();

        // 燃烧时间随等级怎加
        target.setSecondsOnFire((int) Math.round(5 * fireMultiplier));
        // 添加烈焰标记
        target.addEffect(new MobEffectInstance(
                ModMobEffects.BLAZE.get(),
                (int) Math.round(100 * fireMultiplier), blazeAmplifier));

        // 元素反应：火 + 冰(融化) / 火 + 电(超载)，合并成一次伤害结算（避免无敌帧吞叠加）
        double reactionDamage = ElementalReactionHelper.applyBlazeReactions(target, fireEffect.getAmplifier());
        if (reactionDamage > 0.0D) {
            target.hurt(target.level().damageSources().indirectMagic(attacker, null), (float) reactionDamage);
        }

        // 击退
        knockback(target, attacker, (float) fireMultiplier);
    }

    /**
     * 【冰龙】打出冰龙攻击：冰封 + 缓慢 III + 挖掘疲劳 III（持续 10 秒，等级越高持续越久）
     * 冰块渲染由 FrozenEvents 监听 MobEffectEvent 自动同步（任何方式施加 FROZEN 效果都生效）
     */
    private static void performIceAttack(LivingEntity attacker, LivingEntity target, MobEffectInstance iceEffect) {
        double iceMultiplier = AttackMultipliers.of(
                iceEffect.getAmplifier(),
                DragonBlessingsConfig.ICE_ATTACK_MAX_LEVEL.get());

        // 冰封标记的等级镜像冰龙之力的等级（缓慢/挖掘疲劳保持固定 III）
        int frozenAmplifier = iceEffect.getAmplifier();
        int duration = (int) Math.round(200 * iceMultiplier);

        // 添加效果
        target.addEffect(new MobEffectInstance(ModMobEffects.FROZEN.get(), duration, frozenAmplifier));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2));

        // 元素反应：冰 + 火(融化) / 冰 + 电(超导)，合并成一次伤害结算（避免无敌帧吞叠加）
        double reactionDamage = ElementalReactionHelper.applyFrozenReactions(target, iceEffect.getAmplifier());
        if (reactionDamage > 0.0D) {
            target.hurt(target.level().damageSources().indirectMagic(attacker, null), (float) reactionDamage);
        }
    }

    /**
     * 【电龙】打出电龙攻击：闪电链（带冷却，防止手速过快导致鬼畜）
     */
    private static void performLightningAttack(
            LivingEntity attacker,
            LivingEntity target,
            MobEffectInstance lightningEffect) {
        // lightning attack code
        long now = attacker.level().getGameTime();
        long cooldown = DragonBlessingsConfig.CHAIN_COOLDOWN.get();
        Long lastTrigger = LAST_TRIGGER_TIME.get(attacker.getUUID());

        // 冷却过了才触发
        if (lastTrigger == null || now - lastTrigger >= cooldown) {
            LAST_TRIGGER_TIME.put(attacker.getUUID(), now);

            // 创建闪电链效果
            ChainLightningHelper.createChainLightning(
                    attacker.level(),
                    target,
                    attacker,
                    lightningEffect.getAmplifier());
        }
    }

    /**
     * 击退目标
     */
    private static void knockback(LivingEntity target, LivingEntity attacker, float strength) {
        double dx = attacker.getX() - target.getX();
        double dz = attacker.getZ() - target.getZ();

        target.knockback(strength, dx, dz);
    }

    /**
     * 实体离开世界（死亡 / 消失 / 区块卸载 / 换维度 / 玩家退出）时清理其冷却记录，
     * 防止 LAST_TRIGGER_TIME 长期累积（防止轻微内存泄漏）。
     * 覆盖所有移除路径，比只监听玩家退出或生物死亡更完整。
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        LAST_TRIGGER_TIME.remove(event.getEntity().getUUID());
    }
}
