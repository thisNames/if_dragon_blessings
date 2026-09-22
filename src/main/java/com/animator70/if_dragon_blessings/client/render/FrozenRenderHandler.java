package com.animator70.if_dragon_blessings.client.render;

// 我的类
import com.animator70.if_dragon_blessings.IfDragonBlessings;
import com.animator70.if_dragon_blessings.capability.FrozenCapability;

// MJ 类
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

// Minecraft 类
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

// Forge 类
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 【冰龙】冰块视觉：当生物处于“冰冻”状态（FrozenCapability）时，在其身上渲染一个半透明的冰块立方体。
 * 纯视觉、无碰撞箱、不阻挡玩家。
 * 冰冻状态由 FrozenCapability + SetFrozenPacket 同步，对远程实体可靠。
 */
@Mod.EventBusSubscriber(modid = IfDragonBlessings.MODID, value = Dist.CLIENT)
public class FrozenRenderHandler {

    // 冰块纹理（原版 Frost Walker 的冰纹理）
    @SuppressWarnings("removal")
    private static final ResourceLocation ICE_TEXTURE = new ResourceLocation("textures/block/frosted_ice_0.png");

    @SubscribeEvent
    public static void onPostRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        // 未处于冰冻状态则跳过
        boolean frozen = FrozenCapability.get(entity)
                .map(FrozenCapability::isFrozen)
                .orElse(false);

        if (!frozen) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();

        // 实体局部包围盒（相对实体脚部），并向外扩展一圈，形成“包裹”效果
        AABB box = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
        box = box.inflate(0.25D);

        // 半透明带纹理渲染类型
        RenderType renderType = RenderType.entityTranslucent(ICE_TEXTURE);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();
        renderIceCube(poseStack, consumer, box, packedLight);
        poseStack.popPose();
    }

    /**
     * 画一个轴对齐的带纹理立方体（6 个面）。
     */
    private static void renderIceCube(PoseStack poseStack, VertexConsumer buffer, AABB box, int packedLight) {
        PoseStack.Pose pose = poseStack.last();
        float minX = (float) box.minX;
        float maxX = (float) box.maxX;
        float minY = (float) box.minY;
        float maxY = (float) box.maxY;
        float minZ = (float) box.minZ;
        float maxZ = (float) box.maxZ;

        // 底面（法线向下）
        quad(buffer, pose, packedLight, 0, -1, 0, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY,
                maxZ);

        // 顶面（法线向上）
        quad(buffer, pose, packedLight, 0, 1, 0, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY,
                minZ);

        // 前面（法线 -z）
        quad(buffer, pose, packedLight, 0, 0, -1, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY,
                minZ);

        // 后面（法线 +z）
        quad(buffer, pose, packedLight, 0, 0, 1, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY,
                maxZ);

        // 左面（法线 -x）
        quad(buffer, pose, packedLight, -1, 0, 0, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY,
                maxZ);

        // 右面（法线 +x）
        quad(buffer, pose, packedLight, 1, 0, 0, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY,
                minZ);
    }

    /**
     * 写一个四边形面（4 个顶点）。
     * 顶点格式需与 RenderType.entityTranslucent 的 NEW_ENTITY 格式一致：
     * 位置 + 颜色 + UV + 覆盖层 + 光照 + 法线。
     */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int packedLight,
            float nx, float ny, float nz,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4) {

        buffer.vertex(pose.pose(), x1, y1, z1).color(255, 255, 255, 255).uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(nx, ny, nz).endVertex();
        buffer.vertex(pose.pose(), x2, y2, z2).color(255, 255, 255, 255).uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(nx, ny, nz).endVertex();
        buffer.vertex(pose.pose(), x3, y3, z3).color(255, 255, 255, 255).uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(nx, ny, nz).endVertex();
        buffer.vertex(pose.pose(), x4, y4, z4).color(255, 255, 255, 255).uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(nx, ny, nz).endVertex();
    }
}
