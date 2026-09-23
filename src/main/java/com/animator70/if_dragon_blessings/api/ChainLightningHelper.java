package com.animator70.if_dragon_blessings.api;

// 我的类
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;
import com.animator70.if_dragon_blessings.init.ModMobEffects;
import com.animator70.if_dragon_blessings.init.ModSounds;
import com.animator70.if_dragon_blessings.network.ModNetwork;

// Minecraft 类
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Java 类
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 【电龙】闪电链逻辑：以被击中的目标为中心，向四周方向均衡地发散闪电，
 * 被链到的目标也会受到电击伤害。
 * 
 * 整体流程：
 * 1. 被击中的目标（中心）先受到闪电伤害，并播放一次雷声；
 * 2. 找出中心周围 RANGE 范围内所有可被链到的目标；
 * 3. 用“方向均衡”算法挑出最多 MAX_CHAIN_TARGETS 个，避免都朝一个方向；
 * 4. 把「中心 + 被链目标」的实体 ID 通过包发给客户端，由客户端渲染闪电链粒子。
 * 
 * 所有数值均从 {@link DragonBlessingsConfig} 读取，可在配置文件中调整。
 */
public class ChainLightningHelper {
    // 每级固定成长量：半径每级 +2 格，目标数每级 +1（起始值与上限可配置）
    private static final int RANGE_PER_LEVEL = 2;
    private static final int TARGETS_PER_LEVEL = 1;

    /**
     * 以被击中的目标为中心生成闪电链。
     *
     * @param level     当前世界
     * @param target    被击中的目标（闪电链的中心）
     * @param attacker  攻击者（拥有电龙之力效果的实体）
     * @param amplifier 攻击者电龙之力 buff 的等级，用于增强伤害并镜像为感电的等级
     */
    public static void createChainLightning(Level level, LivingEntity target, LivingEntity attacker, int amplifier) {
        // 目标已死或无敌则直接返回
        if (target.isDeadOrDying() || target.isInvulnerable()) {
            return;
        }

        // —— 从配置读取参数 ——
        int maxLevel = DragonBlessingsConfig.LIGHTNING_ATTACK_MAX_LEVEL.get();
        // 有效等级不超过配置上限；等级越高伤害越高（每级 +25%，与火/冰共用倍率算法）
        int effectiveLevel = Math.min(amplifier, maxLevel);
        // 伤害倍率
        double damageMultiplier = AttackMultipliers.of(amplifier, maxLevel);

        // 搜索半径 / 目标数量随等级独立成长：起始值 + 等级 × 固定增量，封顶配置的最大值
        int range = Math.min(
                DragonBlessingsConfig.CHAIN_RANGE.get(),
                DragonBlessingsConfig.CHAIN_RANGE_BASE.get() + effectiveLevel * RANGE_PER_LEVEL);

        // 最大链接数量
        int maxTargets = Math.min(
                DragonBlessingsConfig.CHAIN_MAX_TARGETS.get(),
                DragonBlessingsConfig.CHAIN_MAX_TARGETS_BASE.get() + effectiveLevel * TARGETS_PER_LEVEL);

        // 中心伤害
        float centerDamage = (float) (DragonBlessingsConfig.CHAIN_CENTER_DAMAGE.get() * damageMultiplier);
        // 链式基础伤害
        float chainDamageBase = (float) (DragonBlessingsConfig.CHAIN_DAMAGE_BASE.get() * damageMultiplier);
        // 链式衰减伤害
        float chainDamageDecay = DragonBlessingsConfig.CHAIN_DAMAGE_DECAY.get().floatValue();

        // 实体集合
        List<LivingEntity> chain = new ArrayList<>();
        Set<LivingEntity> visited = new HashSet<>();

        // 中心目标被电；雷声只在中心播放一次，避免多条闪电音效叠加爆音
        chain.add(target);
        visited.add(target);

        // 目标受到电链攻击（首个被击中实体）
        hurtWithLightning(level, target, attacker, centerDamage, amplifier);

        // 被攻击到的身上播放雷声
        target.playSound(ModSounds.LIGHTNING_STRIKE.get(), 1.0F, 1.0F);

        // 以中心为球心、range 为半径的包围盒内查找可被链到的目标
        AABB box = target.getBoundingBox().inflate(range);

        // PVP 是否允许（读取服务器 server.properties 的 pvp 设置）
        boolean pvpAllowed = level instanceof ServerLevel serverLevel && serverLevel.getServer().isPvpAllowed();

        // 查找所有可被链到的目标
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> canChainTo(e, attacker, visited, pvpAllowed));

        // 方向均衡地选择最多 maxTargets 个目标
        List<LivingEntity> selected = selectBalanced(candidates, target, maxTargets);

        // 被链到的目标逐个受到电击伤害（伤害逐级递减，不再播放音效）
        for (int i = 0; i < selected.size(); i++) {
            LivingEntity chained = selected.get(i);
            chain.add(chained);

            // 伤害 = 基础伤害 * (1 - 递减比例 * 序号)，最低保留 10%
            float damage = chainDamageBase * Math.max(1.0F - i * chainDamageDecay, 0.1F);
            // 被链到实体
            hurtWithLightning(level, chained, attacker, damage, amplifier);
        }

        // 仅在服务端发送包（客户端由包处理器生成闪电链粒子）
        // 发送坐标而非实体 ID：实体可能已被转换/移除，坐标不受影响，闪电视觉不会丢失
        if (!level.isClientSide && chain.size() >= 2 && level instanceof ServerLevel serverLevel) {
            List<Vec3> positions = new ArrayList<>();

            for (LivingEntity e : chain) {
                positions.add(e.position().add(0.0D, e.getBbHeight() / 2.0D, 0.0D));
            }

            ModNetwork.sendChainLightning(serverLevel, target.blockPosition(), positions);
        }
    }

    /**
     * 判断某个实体是否能被闪电链链到（是否受到电击伤害与感电）
     * 
     * 规则：
     * - 永远排除攻击者自己；
     * - 已处理过、已死亡或无敌的实体一律排除；
     * - 攻击者是玩家时：PVP 开启 → 允许链到其他玩家；PVP 关闭 → 额外排除所有玩家；
     * - 攻击者非玩家时：仅排除自己，其余实体（含玩家）均可被链到。
     */
    private static boolean canChainTo(
            LivingEntity entity,
            LivingEntity attacker,
            Set<LivingEntity> visited,
            boolean pvpAllowed) {
        // code
        // 实体是攻击者
        if (entity == attacker) {
            return false;
        }
        // 实体是不是已经在链里了
        if (visited.contains(entity)) {
            return false;
        }
        // 是否已经死亡、无敌
        if (entity.isDeadOrDying() || entity.isInvulnerable()) {
            return false;
        }

        return !shouldExcludePlayerTarget(attacker, entity, pvpAllowed);
    }

    /**
     * 判断“目标玩家”是否应被排除在闪电链之外。
     * 仅当攻击者是玩家、目标也是玩家、且服务器关闭 PVP 时才排除（玩家之间不互相伤害）。
     */
    private static boolean shouldExcludePlayerTarget(LivingEntity attacker, LivingEntity target, boolean pvpAllowed) {
        return attacker instanceof Player && target instanceof Player && !pvpAllowed;
    }

    /**
     * 方向均衡选择：从候选目标中挑出最多 maxCount 个，让它们相对中心的方向尽量分散。
     * 
     * 贪心策略：
     * 1. 先选离中心最近的一个；
     * 2. 之后每轮，选「与所有已选目标方向的最小夹角」最大的那个候选，
     * 这样每多选一个，都能尽量覆盖一个尚未被覆盖的方向。
     */
    private static List<LivingEntity> selectBalanced(List<LivingEntity> candidates, LivingEntity center, int maxCount) {
        List<LivingEntity> selected = new ArrayList<>();

        if (candidates.isEmpty()) {
            return selected;
        }

        // 复制一份候选，按到中心的距离由近到远排序
        List<LivingEntity> remaining = new ArrayList<>(candidates);

        remaining.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        // 第一个直接取最近的
        selected.add(remaining.remove(0));

        // 贪心补选，直到选满或没有剩余候选
        while (selected.size() < maxCount && !remaining.isEmpty()) {
            int bestIndex = -1;
            double bestDiversity = -1.0D;

            for (int i = 0; i < remaining.size(); i++) {
                double diversity = directionDiversity(remaining.get(i), center, selected);

                if (diversity > bestDiversity) {
                    bestDiversity = diversity;
                    bestIndex = i;
                }
            }

            if (bestIndex < 0) {
                break;
            }

            selected.add(remaining.remove(bestIndex));
        }

        return selected;
    }

    /**
     * 计算候选目标相对中心的方向，与所有已选目标方向的最小夹角
     * 返回值越大，说明该候选所在的方向越“空”，越值得被选中
     */
    private static double directionDiversity(LivingEntity candidate, LivingEntity center, List<LivingEntity> selected) {
        double angle = horizontalAngle(center, candidate);
        double minDiff = Double.MAX_VALUE;

        for (LivingEntity s : selected) {
            minDiff = Math.min(minDiff, angleDiff(angle, horizontalAngle(center, s)));
        }

        return minDiff;
    }

    /**
     * 计算目标相对中心在水平面（XZ 平面）上的方向角，范围 [-PI, PI]
     * 使用 atan2 可以直接得到象限正确的角度
     */
    private static double horizontalAngle(LivingEntity center, LivingEntity entity) {
        double dx = entity.getX() - center.getX();
        double dz = entity.getZ() - center.getZ();

        return Math.atan2(dz, dx);
    }

    /**
     * 计算两个方向角的角度差，并处理角度环绕问题
     * （例如 -PI 与 PI 实际只差一点点，不能简单用 |a-b|）
     * 返回值为 [0, PI]
     */
    private static double angleDiff(double a, double b) {
        double diff = Math.abs(a - b);

        return diff > Math.PI ? 2.0D * Math.PI - diff : diff;
    }

    /**
     * 对实体造成电击伤害并施加感电。
     * 伤害方式由配置 useVanillaLightning 决定：
     * - false（默认）= 魔法电：魔法伤害，不点燃、不变体转换；
     * - true = 原版雷击电：变体转换（苦力怕→闪电苦力怕等）+ 雷击伤害（附带 8 秒点燃）。
     * 感电（shocked）的等级镜像攻击者电龙之力的等级（例：3 级电龙之力 → 3 级感电）。
     */
    private static void hurtWithLightning(
            Level level,
            LivingEntity entity,
            LivingEntity attacker,
            float damage,
            int amplifier) {
        // code
        // 元素反应：电 + 冰(超导) / 电 + 火(超载)。反应伤害合并进本次电击伤害一次结算，
        // 避免原版无敌帧吞掉"电击 + 反应"多段叠加的伤害。
        double reactionDamage = ElementalReactionHelper.applyShockedReactions(entity, amplifier);
        float totalDamage = damage + (float) reactionDamage;

        // 是否使用原版雷击
        if (DragonBlessingsConfig.USE_VANILLA_LIGHTNING.get()) {
            // 原版雷击电：先雷击伤害（附带点燃），再变体转换。
            // 顺序必须"先伤害后转换"：替换型转换（猪→僵尸猪灵等）会移除旧实体，
            // 若先转换，后续 hurt 落在已移除的旧实体上，闪电伤害就丢失了。
            entity.hurt(level.damageSources().lightningBolt(), totalDamage);

            applyThunderConversion(level, entity);
        } else {
            // 魔法电（默认）：只造成魔法伤害，不点燃、不转换；伤害归属攻击者（经验/掉落/击杀统计）
            entity.hurt(level.damageSources().indirectMagic(attacker, null), totalDamage);

            // 魔法电也能把苦力怕变成闪电苦力怕（不走 thunderHit，不附带点燃）
            if (DragonBlessingsConfig.MAGIC_CREEPER_CONVERSION.get()) {
                applyMagicCreeperConversion(entity);
            }
        }

        // 感电定身 3 秒（60 tick）：电龙攻击的核心效果，替代原模组的麻痹
        entity.addEffect(new MobEffectInstance(ModMobEffects.SHOCKED.get(), 60, amplifier));
    }

    /**
     * 只有苦力怕受伤的世界达成！
     * 只把苦力怕转换成闪电苦力怕，不走原版 thunderHit（避免附带 8 秒点燃）。
     * 通过 Access Transformer 访问 Creeper 私有的 DATA_IS_POWERED 同步字段直接设置。
     */
    private static void applyMagicCreeperConversion(LivingEntity entity) {
        if (entity instanceof Creeper creeper && !creeper.isPowered()) {
            // AT 将私有字段强行变为公有
            creeper.getEntityData().set(Creeper.DATA_IS_POWERED, true);
        }
    }

    /**
     * 原版雷击变体转换：复用原版 {@link LivingEntity#thunderHit} 逻辑，
     * 苦力怕→闪电苦力怕、猪→僵尸猪灵、村民→女巫、红色哞菇→棕色哞菇等。
     * 构造一个「不真正生成」的 LightningBolt 仅用于触发转换。
     * 注意：原版 thunderHit 会强制 8 秒点燃目标（与伤害类型无关），仅在开启原版雷击电时使用。
     */
    private static void applyThunderConversion(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);

        bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
        entity.thunderHit(serverLevel, bolt);
    }
}
