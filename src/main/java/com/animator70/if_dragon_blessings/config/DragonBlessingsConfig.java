package com.animator70.if_dragon_blessings.config;

// Forge 类
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 龙之祝福模组的通用（COMMON）配置。
 * 
 * 把闪电链（电龙攻击）里原来硬编码的数值都集中到这里，便于在
 * config/if_dragon_blessings-common.toml 中直接调整
 */
public class DragonBlessingsConfig {
    // 配置规格：构建完成后交给主类注册
    public static final ForgeConfigSpec SPEC;

    // —— 闪电链 ——
    // 搜索半径上限（格）
    public static final ForgeConfigSpec.IntValue CHAIN_RANGE;
    // 搜索半径起始值（等级 0）
    public static final ForgeConfigSpec.IntValue CHAIN_RANGE_BASE;
    // 受到闪电链伤害的实体数量上限（不含中心目标）
    public static final ForgeConfigSpec.IntValue CHAIN_MAX_TARGETS;
    // 目标数量起始值（等级 0）
    public static final ForgeConfigSpec.IntValue CHAIN_MAX_TARGETS_BASE;
    // 电龙之力 buff 的最大有效等级
    public static final ForgeConfigSpec.IntValue LIGHTNING_ATTACK_MAX_LEVEL;
    // 中心目标伤害
    public static final ForgeConfigSpec.DoubleValue CHAIN_CENTER_DAMAGE;
    // 被链目标的基础伤害（后续逐级递减）
    public static final ForgeConfigSpec.DoubleValue CHAIN_DAMAGE_BASE;
    // 每个后续被链目标的伤害递减比例
    public static final ForgeConfigSpec.DoubleValue CHAIN_DAMAGE_DECAY;
    // 闪电链外层光晕颜色
    public static final ForgeConfigSpec.IntValue CHAIN_OUTER_COLOR;
    // 闪电链内芯颜色
    public static final ForgeConfigSpec.IntValue CHAIN_INNER_COLOR;
    // 闪电链外层光晕不透明度
    public static final ForgeConfigSpec.DoubleValue CHAIN_OUTER_OPACITY;
    // 闪电链内芯不透明度
    public static final ForgeConfigSpec.DoubleValue CHAIN_INNER_OPACITY;
    // 闪电链触发冷却（tick）
    public static final ForgeConfigSpec.IntValue CHAIN_COOLDOWN;
    // 闪电链粒子同步半径（格）
    public static final ForgeConfigSpec.DoubleValue CHAIN_SYNC_DISTANCE;
    // 电龙攻击是否使用原版雷击机制（变体转换 + 点燃），否则使用魔法电
    public static final ForgeConfigSpec.BooleanValue USE_VANILLA_LIGHTNING;
    // 魔法电模式下是否把苦力怕转换成闪电苦力怕（小彩蛋，不带火）
    public static final ForgeConfigSpec.BooleanValue MAGIC_CREEPER_CONVERSION;

    // —— 火龙 / 冰龙攻击 ——
    // 火龙之力 buff 的最大有效等级
    public static final ForgeConfigSpec.IntValue FIRE_ATTACK_MAX_LEVEL;
    // 冰龙之力 buff 的最大有效等级
    public static final ForgeConfigSpec.IntValue ICE_ATTACK_MAX_LEVEL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // 电龙攻击
        builder.comment("电龙攻击（闪电链）相关配置").push("chainLightning");

        CHAIN_RANGE = builder
                .comment("闪电链搜索半径上限（格）：等级 0 即为起始半径，每升一级 +2 格，不超过此值。")
                .defineInRange("searchRange", 14, 1, 48);

        CHAIN_RANGE_BASE = builder
                .comment("闪电链搜索半径起始值（等级 0 时的半径），每升一级 +2 格，封顶 searchRange。")
                .defineInRange("searchRangeBase", 8, 1, 48);

        CHAIN_MAX_TARGETS = builder
                .comment("最多受到闪电链伤害的实体数量上限（不包含被击中的中心目标）：等级 0 即为起始数量，每升一级 +1，不超过此值。")
                .defineInRange("maxTargets", 9, 1, 32);

        CHAIN_MAX_TARGETS_BASE = builder
                .comment("闪电链目标数量起始值（等级 0），每升一级 +1，封顶 maxTargets。")
                .defineInRange("maxTargetsBase", 6, 1, 32);

        LIGHTNING_ATTACK_MAX_LEVEL = builder
                .comment("电龙之力（lightning_attack）buff 的最大有效等级，超过部分按此上限计算。")
                .defineInRange("lightningAttackMaxLevel", 3, 1, 10);

        CHAIN_CENTER_DAMAGE = builder
                .comment("中心目标（被击中的实体）受到的闪电伤害。")
                .defineInRange("centerDamage", 5.0D, 0.0D, 100.0D);

        CHAIN_DAMAGE_BASE = builder
                .comment("第一个被链目标受到的基础闪电伤害，后续目标按 chainDamageDecay 逐级递减。")
                .defineInRange("chainDamageBase", 4.0D, 0.0D, 100.0D);

        CHAIN_DAMAGE_DECAY = builder
                .comment("每个后续被链目标的伤害递减比例（0~1）。例如 0.125 表示每级递减 12.5%，基础伤害越大递减越明显。")
                .defineInRange("chainDamageDecay", 0.125D, 0.0D, 1.0D);

        CHAIN_OUTER_COLOR = builder
                .comment("闪电链外层光晕颜色（十六进制 0xRRGGBB）。默认亮紫 0xB44DFF。")
                .defineInRange("outerColor", 0xB44DFF, 0x000000, 0xFFFFFF);

        CHAIN_INNER_COLOR = builder
                .comment("闪电链内芯颜色（十进制整数）。例如白色 0xFFFFFF = 16777215。")
                .defineInRange("innerColor", 0xFFFFFF, 0x000000, 0xFFFFFF);

        CHAIN_OUTER_OPACITY = builder
                .comment("闪电链外层光晕的不透明度系数（0~1）。1.0 为完全不透明。")
                .defineInRange("outerOpacity", 0.9D, 0.0D, 1.0D);

        CHAIN_INNER_OPACITY = builder
                .comment("闪电链内芯的不透明度系数（0~1）。1.0 为完全不透明。")
                .defineInRange("innerOpacity", 1.0D, 0.0D, 1.0D);

        CHAIN_COOLDOWN = builder
                .comment("闪电链触发的冷却时间（tick）。防止手速过快时闪电链频繁叠加。")
                .defineInRange("cooldown", 10, 0, 200);

        CHAIN_SYNC_DISTANCE = builder
                .comment("闪电链粒子效果的同步半径（格）：只向该范围内玩家发送粒子包。建议不小于闪电链搜索半径，否则远处玩家看不到粒子。")
                .defineInRange("syncDistance", 64.0D, 8.0D, 256.0D);

        USE_VANILLA_LIGHTNING = builder
                .comment(
                        "电龙攻击的伤害方式：true = 原版雷击电（附带变体转换与 8 秒点燃；火龙：这样显得我很没面子啊）；false = 魔法电（魔法伤害，不点燃不转换，默认）。")
                .define("useVanillaLightning", false);

        MAGIC_CREEPER_CONVERSION = builder
                .comment("魔法电模式下是否把苦力怕转换成闪电苦力怕（只有苦力怕受伤的世界达成！）。true = 转换（默认）；false = 不转换。")
                .define("magicCreeperConversion", true);

        builder.pop();

        // 火龙/冰龙攻击
        builder.comment("火龙 / 冰龙攻击相关配置").push("dragonAttacks");

        FIRE_ATTACK_MAX_LEVEL = builder
                .comment("火龙之力（fire_attack）buff 的最大有效等级，超过部分按此上限计算。每级 +25% 燃烧/烈焰标记时长与击退强度。")
                .defineInRange("fireAttackMaxLevel", 3, 1, 10);

        ICE_ATTACK_MAX_LEVEL = builder
                .comment("冰龙之力（ice_attack）buff 的最大有效等级，超过部分按此上限计算。每级 +25% 冰封/缓慢/挖掘疲劳时长。")
                .defineInRange("iceAttackMaxLevel", 3, 1, 10);

        builder.pop();

        SPEC = builder.build();
    }

    private DragonBlessingsConfig() {
    }
}
