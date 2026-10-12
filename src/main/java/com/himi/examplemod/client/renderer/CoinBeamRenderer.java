package com.himi.examplemod.client.renderer;

import com.himi.examplemod.client.model.CoinBeamModel;
import com.himi.examplemod.entity.CoinBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 神秘硬币 3D 长方体光束渲染器。
 *
 * <p>朝向策略：进入 {@link #applyRotations} 的 {@link PoseStack} 带着 GeckoLib/原版注入的一段
 * 基础旋转（对非 Living 实体并非世界对齐，直接套 yaw/pitch 会让沿 geo +Z 的光束「立起来」）。
 * 因此先把当前矩阵的纯旋转部分求逆乘回，将朝向帧归零到世界对齐，再按目标偏移向量把模型 +Z 轴
 * 精确瞄准到命中方向，最后对长度轴（Z）按距离×生长系数缩放、截面（X/Y）按粗细缩放。</p>
 *
 * <p>若实测方向上下/前后仍反，翻转 {@link #YAW_FLIP}/{@link #PITCH_FLIP} 即可。</p>
 */
public class CoinBeamRenderer extends GeoEntityRenderer<CoinBeamEntity> {

    /** 光束模型参考长度（方块）：geo 中 cube 长 256 单位 = 16 方块。 */
    private static final float REF_LENGTH_BLOCKS = 16.0F;
    /** 光束粗细（方块）：模型截面 16 单位 = 1 方块，再乘此系数。 */
    private static final float BEAM_THICKNESS = 0.2F;
    /** 朝向修正符号（实测方向相反时改 -1）。 */
    private static final float YAW_FLIP = 1.0F;
    private static final float PITCH_FLIP = 1.0F;

    public CoinBeamRenderer(EntityRendererProvider.Context context) {
        super(context, new CoinBeamModel());
    }

    @Override
    protected void applyRotations(CoinBeamEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        Vec3 d = animatable.targetOffset();
        double len = d.length();
        if (len < 1.0E-3D) {
            return;
        }

        // 1) 归零 inherited 旋转：取当前矩阵去掉平移得到纯旋转(含 nativeScale)部分，求逆乘回。
        //    纯旋转矩阵不含平移，乘回去只改变朝向、不会移动光束起点。
        Matrix4f base = new Matrix4f(poseStack.last().pose());
        base.m03(0.0F);
        base.m13(0.0F);
        base.m23(0.0F);
        base.invert();
        poseStack.mulPose(base);

        // 2) 在世界对齐帧里把模型 +Z 瞄准到方向 d：yaw 绕 Y、pitch 绕 X。
        double nx = d.x / len;
        double ny = d.y / len;
        double nz = d.z / len;
        float yaw = (float) (Math.atan2(nx, nz) * 180.0D / Math.PI) * YAW_FLIP;
        float pitch = (float) (-Math.asin(Mth.clamp(ny, -1.0D, 1.0D)) * 180.0D / Math.PI) * PITCH_FLIP;
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // 3) 长度沿模型 Z 按 距离/参考长度 × 生长进度 缩放；截面 X/Y 固定粗细。
        float grow = Math.min(1.0F, (animatable.tickCount + partialTick) / (float) CoinBeamEntity.GROW_TICKS);
        float lengthScale = (float) (len / REF_LENGTH_BLOCKS) * grow;
        poseStack.scale(BEAM_THICKNESS, BEAM_THICKNESS, lengthScale);
    }

    @Override
    public boolean shouldShowName(CoinBeamEntity animatable) {
        return false;
    }
}
