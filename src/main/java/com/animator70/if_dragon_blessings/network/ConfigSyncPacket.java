package com.animator70.if_dragon_blessings.network;

// 我的类
import com.animator70.if_dragon_blessings.config.DragonBlessingsConfig;

// Minecraft 类
import net.minecraft.network.FriendlyByteBuf;

// Forge 类
import net.minecraftforge.network.NetworkEvent;

// Java 类
import java.util.function.Supplier;

/**
 * 配置同步包：服务端把 COMMON 配置发给客户端，保证全服配置权威统一
 * 
 * 服务端在玩家登录时发送本包，客户端接收后把各 ConfigValue 更新为服务端值
 * 之后客户端渲染（如闪电链颜色）读到的就是服务端统一后的配置
 */
public class ConfigSyncPacket {
    private final int searchRange;
    private final int searchRangeBase;
    private final int maxTargets;
    private final int maxTargetsBase;
    private final int lightningAttackMaxLevel;
    private final int fireAttackMaxLevel;
    private final int iceAttackMaxLevel;
    private final double centerDamage;
    private final double chainDamageBase;
    private final double chainDamageDecay;
    private final int outerColor;
    private final int innerColor;
    private final double outerOpacity;
    private final double innerOpacity;
    private final int cooldown;
    private final double syncDistance;
    private final boolean useVanillaLightning;
    private final boolean magicCreeperConversion;

    /**
     * 服务端构造：从本地（权威）配置读取当前值
     */
    public ConfigSyncPacket() {
        this.searchRange = DragonBlessingsConfig.CHAIN_RANGE.get();
        this.searchRangeBase = DragonBlessingsConfig.CHAIN_RANGE_BASE.get();
        this.maxTargets = DragonBlessingsConfig.CHAIN_MAX_TARGETS.get();
        this.maxTargetsBase = DragonBlessingsConfig.CHAIN_MAX_TARGETS_BASE.get();
        this.lightningAttackMaxLevel = DragonBlessingsConfig.LIGHTNING_ATTACK_MAX_LEVEL.get();
        this.fireAttackMaxLevel = DragonBlessingsConfig.FIRE_ATTACK_MAX_LEVEL.get();
        this.iceAttackMaxLevel = DragonBlessingsConfig.ICE_ATTACK_MAX_LEVEL.get();
        this.centerDamage = DragonBlessingsConfig.CHAIN_CENTER_DAMAGE.get();
        this.chainDamageBase = DragonBlessingsConfig.CHAIN_DAMAGE_BASE.get();
        this.chainDamageDecay = DragonBlessingsConfig.CHAIN_DAMAGE_DECAY.get();
        this.outerColor = DragonBlessingsConfig.CHAIN_OUTER_COLOR.get();
        this.innerColor = DragonBlessingsConfig.CHAIN_INNER_COLOR.get();
        this.outerOpacity = DragonBlessingsConfig.CHAIN_OUTER_OPACITY.get();
        this.innerOpacity = DragonBlessingsConfig.CHAIN_INNER_OPACITY.get();
        this.cooldown = DragonBlessingsConfig.CHAIN_COOLDOWN.get();
        this.syncDistance = DragonBlessingsConfig.CHAIN_SYNC_DISTANCE.get();
        this.useVanillaLightning = DragonBlessingsConfig.USE_VANILLA_LIGHTNING.get();
        this.magicCreeperConversion = DragonBlessingsConfig.MAGIC_CREEPER_CONVERSION.get();
    }

    /**
     * 客户端解码
     */
    public ConfigSyncPacket(FriendlyByteBuf buf) {
        this.searchRange = buf.readInt();
        this.searchRangeBase = buf.readInt();
        this.maxTargets = buf.readInt();
        this.maxTargetsBase = buf.readInt();
        this.lightningAttackMaxLevel = buf.readInt();
        this.fireAttackMaxLevel = buf.readInt();
        this.iceAttackMaxLevel = buf.readInt();
        this.centerDamage = buf.readDouble();
        this.chainDamageBase = buf.readDouble();
        this.chainDamageDecay = buf.readDouble();
        this.outerColor = buf.readInt();
        this.innerColor = buf.readInt();
        this.outerOpacity = buf.readDouble();
        this.innerOpacity = buf.readDouble();
        this.cooldown = buf.readInt();
        this.syncDistance = buf.readDouble();
        this.useVanillaLightning = buf.readBoolean();
        this.magicCreeperConversion = buf.readBoolean();
    }

    /**
     * 编码
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.searchRange);
        buf.writeInt(this.searchRangeBase);
        buf.writeInt(this.maxTargets);
        buf.writeInt(this.maxTargetsBase);
        buf.writeInt(this.lightningAttackMaxLevel);
        buf.writeInt(this.fireAttackMaxLevel);
        buf.writeInt(this.iceAttackMaxLevel);
        buf.writeDouble(this.centerDamage);
        buf.writeDouble(this.chainDamageBase);
        buf.writeDouble(this.chainDamageDecay);
        buf.writeInt(this.outerColor);
        buf.writeInt(this.innerColor);
        buf.writeDouble(this.outerOpacity);
        buf.writeDouble(this.innerOpacity);
        buf.writeInt(this.cooldown);
        buf.writeDouble(this.syncDistance);
        buf.writeBoolean(this.useVanillaLightning);
        buf.writeBoolean(this.magicCreeperConversion);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            // 客户端：把服务端权威配置写入本地 ConfigValue（只改内存，不写文件）
            DragonBlessingsConfig.CHAIN_RANGE.set(this.searchRange);
            DragonBlessingsConfig.CHAIN_RANGE_BASE.set(this.searchRangeBase);
            DragonBlessingsConfig.CHAIN_MAX_TARGETS.set(this.maxTargets);
            DragonBlessingsConfig.CHAIN_MAX_TARGETS_BASE.set(this.maxTargetsBase);
            DragonBlessingsConfig.LIGHTNING_ATTACK_MAX_LEVEL.set(this.lightningAttackMaxLevel);
            DragonBlessingsConfig.FIRE_ATTACK_MAX_LEVEL.set(this.fireAttackMaxLevel);
            DragonBlessingsConfig.ICE_ATTACK_MAX_LEVEL.set(this.iceAttackMaxLevel);
            DragonBlessingsConfig.CHAIN_CENTER_DAMAGE.set(this.centerDamage);
            DragonBlessingsConfig.CHAIN_DAMAGE_BASE.set(this.chainDamageBase);
            DragonBlessingsConfig.CHAIN_DAMAGE_DECAY.set(this.chainDamageDecay);
            DragonBlessingsConfig.CHAIN_OUTER_COLOR.set(this.outerColor);
            DragonBlessingsConfig.CHAIN_INNER_COLOR.set(this.innerColor);
            DragonBlessingsConfig.CHAIN_OUTER_OPACITY.set(this.outerOpacity);
            DragonBlessingsConfig.CHAIN_INNER_OPACITY.set(this.innerOpacity);
            DragonBlessingsConfig.CHAIN_COOLDOWN.set(this.cooldown);
            DragonBlessingsConfig.CHAIN_SYNC_DISTANCE.set(this.syncDistance);
            DragonBlessingsConfig.USE_VANILLA_LIGHTNING.set(this.useVanillaLightning);
            DragonBlessingsConfig.MAGIC_CREEPER_CONVERSION.set(this.magicCreeperConversion);
        });

        context.setPacketHandled(true);
    }
}
