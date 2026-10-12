package com.himi.examplemod.client.renderer;

import com.himi.examplemod.client.model.ScorchingEyeModel;
import com.himi.examplemod.entity.ScorchingEye;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 炽热邪眼渲染器：以 GeckoLib {@link GeoEntityRenderer} 播放骨骼动画并渲染 3D 飞行模型。
 *
 * <p>几何体以 16 像素 = 1 方块为尺度，躯干约 10 像素宽、悬停于脚下约 6~16 像素处，
 * 与 0.6×0.6 的碰撞箱匹配，无需额外缩放。</p>
 */
public class ScorchingEyeRenderer extends GeoEntityRenderer<ScorchingEye> {

    public ScorchingEyeRenderer(EntityRendererProvider.Context context) {
        super(context, new ScorchingEyeModel());
    }
}
