package com.animator70.if_dragon_blessings.client.particle;

// MJ 类
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

// Minecraft 类
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.Vec3;

// Java 类 
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 闪电链粒子：在两个实体之间生成锯齿状闪电
 * 
 * 视觉结构：
 * - 主链：一条锯齿状的链条（中点位移分形生成）
 * - 分支：主链上随机长出侧枝，侧枝再继续分叉，形成类似菌类根茎的发散形状
 * 
 * 渲染为“紫色光晕 + 白色亮芯”双层，加色混合叠加，随时间淡出
 * 每个闪电使用独立随机种子，保证每次生成的形状都不同
 */
public class LightningChainParticle extends Particle {
    // 紫色光晕
    private static final int OUTER_COLOR = 0xA929EE;
    // 白色亮芯
    private static final int INNER_COLOR = 0xFFFFFF;

    /**
     * 自定义粒子渲染类型
     * 用加色混合（SRC_ALPHA + ONE）画纯色四边形，关闭深度写入让闪电半透明叠加
     */
    private static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textureManager) {
            // 开启加色混合（发光效果），关闭深度写入（半透明叠加）
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);

            // 关键：手动渲染必须绑定 shader，否则 GPU 没有着色器程序，什么都不会画
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            // 使用 QUADS 模式 + POSITION_COLOR 顶点格式（位置 + 颜色）
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();

            // 恢复渲染状态
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    };

    // 主链与分支的分段（每段为 [起点, 终点]）
    private final List<Vec3[]> mainSegments = new ArrayList<>();
    private final List<Vec3[]> branchSegments = new ArrayList<>();

    // 独立随机源：每个闪电用世界随机播种，保证每次形状不同
    private final Random rand;

    // 闪电链的总长度，用于按长度缩放宽度（短链更细）
    private final double totalLength;

    public LightningChainParticle(ClientLevel level, Vec3 start, Vec3 end) {
        super(level, start.x, start.y, start.z);

        // 用世界随机源播种独立随机数，避免多个粒子共享同一个随机流导致形状雷同
        this.rand = new Random(level.random.nextLong());
        this.totalLength = end.subtract(start).length();

        // 存活时长随机化，让每条闪电的淡出节奏略有不同
        this.lifetime = 10 + this.rand.nextInt(5);
        this.age = 0;

        generateBolt(start, end);
    }

    @Override
    public void tick() {
        // 记录上一帧坐标（粒子系统需要）
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // 根据剩余寿命计算整体透明度（逐渐淡出）
        float lifeFraction = this.age / (float) this.lifetime;
        int alpha = (int) (255 * (1.0F - lifeFraction));

        if (alpha <= 0) {
            return;
        }

        Vec3 cam = camera.getPosition();

        // 主链：全亮度
        renderSegments(buffer, cam, this.mainSegments, 1.0D, alpha);
        // 分支：更细、透明度减半，形成“根须”的层次感
        renderSegments(buffer, cam, this.branchSegments, 0.6D, alpha / 2);
    }

    /**
     * 把一批分段画成“外紫内白”的双层发光四边形
     * 核心：把每段都展开成一个朝向相机的窄四边形（billboard）
     * 这样无论从哪个角度看，闪电都有宽度、不会变成一条细线
     */
    private void renderSegments(VertexConsumer buffer, Vec3 cam, List<Vec3[]> segments, double widthScale, int alpha) {
        if (alpha <= 0) {
            return;
        }

        // 外层光晕更透明，内层亮芯更亮
        int outerAlpha = Math.max(1, alpha / 2);
        int innerAlpha = alpha;

        for (Vec3[] segment : segments) {
            // 转成相机相对坐标（粒子渲染的标准做法：顶点坐标减相机位置）
            Vec3 a = segment[0].subtract(cam);
            Vec3 c = segment[1].subtract(cam);
            Vec3 dir = c.subtract(a);

            double len = dir.length();

            if (len < 1.0E-4D) {
                continue;
            }

            Vec3 mid = a.add(dir.scale(0.5D));

            // 求“面向相机”的宽度方向：
            // 相机相对坐标下相机位于原点，视线方向 = 从段中点指向原点 = -mid
            // 用 段方向 × 视线方向 得到同时垂直于两者的向量，即 billboard 的宽度轴
            Vec3 perpendicular = dir.cross(mid.scale(-1.0D)).normalize();

            // 宽度 = 相机距离因子 × 链总长度因子：
            // 相机距离因子让远处闪电也看得清；总长度因子让贴很近的短链变细
            double distFactor = mid.length() / 5.0D + 1.0D;
            double lengthFactor = 0.4D + 0.6D * Math.min(this.totalLength / 3.0D, 1.0D);
            double widthBase = 0.012D * distFactor * lengthFactor;

            // 外层紫色光晕（宽）
            emitQuad(buffer, a, c, perpendicular, widthBase * 2.5D * widthScale, OUTER_COLOR, outerAlpha);
            // 内层白色亮芯（细）
            emitQuad(buffer, a, c, perpendicular, widthBase * widthScale, INNER_COLOR, innerAlpha);
        }
    }

    /**
     * 以 a、c 为两端点，向宽度方向（perpendicular）两侧各展开 width
     * 生成一个四边形。顶点顺序为逆时针，保证正面朝向相机
     */
    private void emitQuad(
            VertexConsumer buffer,
            Vec3 a,
            Vec3 c,
            Vec3 perpendicular,
            double width,
            int color,
            int alpha) {
        // code...
        Vec3 offset = perpendicular.scale(width);

        // 从 int 颜色值里拆出 RGB 分量
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        buffer.vertex(a.x - offset.x, a.y - offset.y, a.z - offset.z).color(r, g, b, alpha).endVertex();
        buffer.vertex(a.x + offset.x, a.y + offset.y, a.z + offset.z).color(r, g, b, alpha).endVertex();
        buffer.vertex(c.x + offset.x, c.y + offset.y, c.z + offset.z).color(r, g, b, alpha).endVertex();
        buffer.vertex(c.x - offset.x, c.y - offset.y, c.z - offset.z).color(r, g, b, alpha).endVertex();
    }

    /**
     * 生成整条闪电的几何：先做主链分形，再从主链中间点长出分支
     */
    private void generateBolt(Vec3 start, Vec3 end) {
        // 短链（实体贴很近）用更少的分形层数，避免在小空间里生成过于细密的闪电
        int levels = this.totalLength < 2.0D ? 2 : 4;
        double roughness = 0.3D + this.rand.nextDouble() * 0.2D;
        List<Vec3> mainPoints = displace(start, end, levels, roughness);
        addChain(mainPoints, this.mainSegments);

        // 分支概率随链长度缩放：短链少分叉，避免密集空间里形成“雷暴”网状
        double lengthScale = Math.min(this.totalLength / 3.0D, 1.0D);
        double branchChance = (0.4D + this.rand.nextDouble() * 0.3D) * lengthScale;

        for (int i = 1; i < mainPoints.size() - 1; i++) {
            if (this.rand.nextFloat() < branchChance) {
                double spreadAngle = 55.0D + this.rand.nextDouble() * 20.0D;
                double lengthFactor = 0.3D + this.rand.nextDouble() * 0.2D;
                growBranch(mainPoints, i, totalLength, spreadAngle, lengthFactor, 1);
            }
        }
    }

    /**
     * 从父链的某个中间点长出一条分支：
     * 分支方向 = 父链在该点的切向随机偏转一个角度，长度按比例缩放，
     * 分支自身再做一层中点位移分形，并按概率继续长出下一级分支（递归）。
     */
    private void growBranch(
            List<Vec3> parentPoints,
            int index,
            double parentLength,
            double spreadAngle,
            double lengthFactor,
            int depth) {
        // code...
        // 用前后两个点求该点处的切向
        Vec3 from = parentPoints.get(index);
        Vec3 prev = parentPoints.get(index - 1);
        Vec3 next = parentPoints.get(index + 1);
        Vec3 tangent = next.subtract(prev).normalize();

        // 分支方向 = 沿主链切向随机偏转
        Vec3 branchDir = deviate(tangent, spreadAngle);
        double branchLength = parentLength * lengthFactor * (0.5D + this.rand.nextDouble());
        Vec3 to = from.add(branchDir.scale(branchLength));

        // 分支自身也做锯齿分形
        List<Vec3> branchPoints = displace(from, to, 2, 0.5D);
        addChain(branchPoints, this.branchSegments);

        // 以一定概率继续长下一级分支（depth 控制递归深度，防止无限递归）
        if (depth > 0) {
            for (int j = 1; j < branchPoints.size() - 1; j++) {
                if (this.rand.nextFloat() < 0.35F) {
                    growBranch(branchPoints, j, branchLength, spreadAngle + 15.0D, 0.5D, depth - 1);
                }
            }
        }
    }

    /**
     * 中点位移分形（Midpoint Displacement）：
     * 反复把每条线段在中点处一分为二，并给中点加一个随机的垂直偏移，
     * 迭代 levels 次后得到一条自然的锯齿状折线。
     * 返回从 start 到 end 的点列（含首尾）。
     */
    private List<Vec3> displace(Vec3 start, Vec3 end, int levels, double roughness) {
        List<Vec3> pts = new ArrayList<>();
        pts.add(start);
        pts.add(end);

        double rough = roughness;
        for (int level = 0; level < levels; level++) {
            List<Vec3> next = new ArrayList<>();

            for (int i = 0; i < pts.size() - 1; i++) {
                Vec3 a = pts.get(i);
                Vec3 b = pts.get(i + 1);
                next.add(a);

                Vec3 dir = b.subtract(a);
                Vec3 mid = a.add(dir.scale(0.5D));

                // 偏移方向垂直于线段，偏移量随线段长度和粗糙度变化
                Vec3 perpendicular = randomPerpendicular(dir);
                double offsetMagnitude = dir.length() * rough * (this.rand.nextDouble() * 2.0D - 1.0D);

                next.add(mid.add(perpendicular.scale(offsetMagnitude)));
            }

            next.add(pts.get(pts.size() - 1));
            pts = next;
            // 每细分一层，粗糙度递减，保证越细的段越平滑
            rough *= 0.7D;
        }
        return pts;
    }

    /**
     * 把一条点列按相邻两点拆成线段，追加到 segments 列表。
     */
    private void addChain(List<Vec3> points, List<Vec3[]> segments) {
        for (int i = 0; i < points.size() - 1; i++) {
            segments.add(new Vec3[] { points.get(i), points.get(i + 1) });
        }
    }

    /**
     * 将方向向量绕一个随机垂直轴旋转 [0, maxAngleDeg] 度（罗德里格斯旋转公式）。
     * 用于让分支方向在主链切向附近随机偏转。
     */
    private Vec3 deviate(Vec3 dir, double maxAngleDeg) {
        // 取一个垂直于 dir 的随机旋转轴
        Vec3 axis = randomPerpendicular(dir);

        double angle = this.rand.nextDouble() * Math.toRadians(maxAngleDeg);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // 罗德里格斯旋转：v' = v·cosθ + (axis × v)·sinθ
        // （因为 axis ⊥ dir，axis·dir = 0，第三项为 0，可省略）
        return dir.scale(cos).add(axis.cross(dir).scale(sin)).normalize();
    }

    /**
     * 返回一个随机的、垂直于 dir 的单位向量。
     * 做法：先找一个与 dir 不平行的参考轴，叉乘得到垂直向量，
     * 再绕 dir 随机旋转一个角度，得到随机方向的垂直向量。
     */
    private Vec3 randomPerpendicular(Vec3 dir) {
        Vec3 n = dir.normalize();

        // 选一个与 n 不平行的参考轴（优先 Y 轴，若 n 几乎平行于 Y 则用 X 轴）
        Vec3 axis = n.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (axis.lengthSqr() < 1.0E-4D) {
            axis = n.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        axis = axis.normalize();

        // 绕 n 随机旋转一个角度，得到任意方向的垂直向量
        double angle = this.rand.nextDouble() * Math.PI * 2.0D;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return axis.scale(cos).add(n.cross(axis).scale(sin)).normalize();
    }
}
