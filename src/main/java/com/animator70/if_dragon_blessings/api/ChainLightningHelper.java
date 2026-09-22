package com.animator70.if_dragon_blessings.api;

// 我的类
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;
import com.animator70.if_dragon_blessings.init.ModSounds;
import com.animator70.if_dragon_blessings.network.ModNetwork;

// Minecraft 类
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

// Java 类
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 闪电链逻辑：以被击中的目标为中心，向四周方向均衡地发散闪电，
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

    /**
     * 以被击中的目标为中心生成闪电链。
     *
     * @param level     当前世界
     * @param target    被击中的目标（闪电链的中心）
     * @param attacker  攻击者（拥有电龙之力效果的实体）
     * @param amplifier 攻击者电龙之力 buff 的等级，用于增强伤害
     */
    public static void createChainLightning(Level level, LivingEntity target, LivingEntity attacker, int amplifier) {
        // 目标已死或无敌则直接返回
        if (target.isDeadOrDying() || target.isInvulnerable()) {
            return;
        }

        // —— 从配置读取参数 ——
        int range = DragonBlessingsConfig.CHAIN_RANGE.get();
        int maxTargets = DragonBlessingsConfig.CHAIN_MAX_TARGETS.get();
        int maxLevel = DragonBlessingsConfig.LIGHTNING_ATTACK_MAX_LEVEL.get();

        // 有效等级不超过配置上限；等级越高伤害越高（每级 +25%）
        int effectiveLevel = Math.min(amplifier, maxLevel);
        double damageMultiplier = 1.0D + effectiveLevel * 0.25D;

        float centerDamage = (float) (DragonBlessingsConfig.CHAIN_CENTER_DAMAGE.get() * damageMultiplier);
        float chainDamageBase = (float) (DragonBlessingsConfig.CHAIN_DAMAGE_BASE.get() * damageMultiplier);
        float chainDamageDecay = DragonBlessingsConfig.CHAIN_DAMAGE_DECAY.get().floatValue();

        List<LivingEntity> chain = new ArrayList<>();
        Set<LivingEntity> visited = new HashSet<>();

        // 中心目标被电；雷声只在中心播放一次，避免多条闪电音效叠加爆音
        chain.add(target);
        visited.add(target);
        hurtWithLightning(level, target, centerDamage);
        target.playSound(ModSounds.LIGHTNING_STRIKE.get(), 1.0F, 1.0F);

        // 以中心为球心、range 为半径的包围盒内查找可被链到的目标
        AABB box = target.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> canChainTo(e, attacker, visited));

        // 方向均衡地选择最多 maxTargets 个目标
        List<LivingEntity> selected = selectBalanced(candidates, target, maxTargets);

        // 被链到的目标逐个受到电击伤害（伤害逐级递减，不再播放音效）
        for (int i = 0; i < selected.size(); i++) {
            LivingEntity chained = selected.get(i);
            chain.add(chained);

            // 伤害 = 基础伤害 * (1 - 递减比例 * 序号)，最低保留 10%
            float damage = chainDamageBase * Math.max(1.0F - i * chainDamageDecay, 0.1F);
            hurtWithLightning(level, chained, damage);
        }

        // 仅在服务端发送包（客户端由包处理器生成闪电链粒子）
        if (!level.isClientSide && chain.size() >= 2 && level instanceof ServerLevel serverLevel) {
            List<Integer> ids = new ArrayList<>();
            for (LivingEntity e : chain) {
                ids.add(e.getId());
            }
            ModNetwork.sendChainLightning(serverLevel, target.blockPosition(), ids);
        }
    }

    /**
     * 判断某个实体是否能被闪电链链到。
     * 排除：玩家、攻击者自己、已被处理过的、已死或无敌的实体。
     */
    private static boolean canChainTo(LivingEntity entity, LivingEntity attacker, Set<LivingEntity> visited) {
        if (entity instanceof Player) {
            return false;
        }
        if (entity == attacker) {
            return false;
        }
        if (visited.contains(entity)) {
            return false;
        }
        return !entity.isDeadOrDying() && !entity.isInvulnerable();
    }

    /**
     * 方向均衡选择：从候选目标中挑出最多 maxCount 个，让它们相对中心的方向尽量分散。
     * <p>
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
     * 用闪电（雷电）伤害源对实体造成伤害。
     */
    private static void hurtWithLightning(Level level, LivingEntity entity, float damage) {
        entity.hurt(level.damageSources().lightningBolt(), damage);
    }
}
