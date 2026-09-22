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
    // 搜索半径（格）
    public static final ForgeConfigSpec.IntValue CHAIN_RANGE;
    // 受到闪电链伤害的实体数量（不含中心目标）
    public static final ForgeConfigSpec.IntValue CHAIN_MAX_TARGETS;
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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("电龙攻击（闪电链）相关配置").push("chainLightning");

        CHAIN_RANGE = builder
                .comment("闪电链搜索半径（格）：以被击中的目标为中心，向外寻找可被链到的实体。")
                .defineInRange("searchRange", 8, 1, 32);

        CHAIN_MAX_TARGETS = builder
                .comment("最多受到闪电链伤害的实体数量（不包含被击中的中心目标）。")
                .defineInRange("maxTargets", 6, 1, 32);

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

        builder.pop();

        SPEC = builder.build();
    }

    private DragonBlessingsConfig() {
    }
}
