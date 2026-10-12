package com.himi.examplemod.client.renderer;

import com.himi.examplemod.client.model.FlameSalamanderModel;
import com.himi.examplemod.entity.FlameSalamander;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 烈焰蝾螈渲染器：以 GeckoLib {@link GeoEntityRenderer} 播放骨骼动画并渲染 3D 模型。
 *
 * <p>几何体以 16 像素 = 1 方块为尺度，躯干约 16 像素高（≈1 方块），无需额外缩放即可与碰撞箱匹配。</p>
 */
public class FlameSalamanderRenderer extends GeoEntityRenderer<FlameSalamander> {

    public FlameSalamanderRenderer(EntityRendererProvider.Context context) {
        super(context, new FlameSalamanderModel());
    }
}
