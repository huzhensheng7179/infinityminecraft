package com.himi.examplemod.client.renderer;

import com.himi.examplemod.client.model.CoinBlastModel;
import com.himi.examplemod.entity.CoinBlastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 神秘硬币终点 3D 爆炸渲染器。
 *
 * <p>按同步半径整体缩放，并随时间自旋；核心的鼓胀/尖刺迸出由 explode 动画驱动，
 * 动画末端 scale 收回到 0 形成消散效果。</p>
 */
public class CoinBlastRenderer extends GeoEntityRenderer<CoinBlastEntity> {

    /** 爆炸模型基准半径（方块）：geo 尖刺最远约 50 单位 ≈ 3 方块，此处再乘半径系数。 */
    private static final float BLAST_BASE = 0.5F;

    public CoinBlastRenderer(EntityRendererProvider.Context context) {
        super(context, new CoinBlastModel());
    }

    @Override
    protected void applyRotations(CoinBlastEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        float s = Math.max(0.2F, animatable.scaleRadius() * BLAST_BASE);
        poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * 24.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ageInTicks * 12.0F));
        poseStack.scale(s, s, s);
    }

    @Override
    public boolean shouldShowName(CoinBlastEntity animatable) {
        return false;
    }
}
