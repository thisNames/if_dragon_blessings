package com.animator70.if_dragon_blessings.client.render;

// 我的类
import com.animator70.if_dragon_blessings.client.particle.LightningChainParticle;

// Minecraft 类
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

// Forge 类
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// Java 类
import java.util.List;

/**
 * 【闪电】粒子绘制代理
 * ClientLightningRenderer
 */
@OnlyIn(Dist.CLIENT)
public final class ClientLightningRenderer {
    private ClientLightningRenderer() {
    }

    /**
     * 创建闪电粒子
     */
    public static void spawn(List<Vec3> positions) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        // 至少两个点：中心点 + 目标点
        if (level == null || positions.size() < 2) {
            return;
        }

        // 第一个坐标是闪电链的中心，从中心向其余每个点放射状发散
        Vec3 centerPos = positions.get(0);

        for (int i = 1; i < positions.size(); i++) {
            mc.particleEngine.add(new LightningChainParticle(level, centerPos, positions.get(i)));
        }
    }
}
