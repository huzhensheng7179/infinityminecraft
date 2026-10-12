package com.himi.examplemod.client.renderer;

import com.himi.examplemod.client.model.ScytheReaperModel;
import com.himi.examplemod.entity.ScytheReaper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 镰恶魔渲染器：以 GeckoLib {@link GeoEntityRenderer} 播放骨骼动画并渲染 3D 飘浮模型。
 *
 * <p>几何体以 16 像素 = 1 方块为尺度，人形躯干约 1.5~2 方块高，与 0.6×1.8 的碰撞箱匹配，
 * 无需额外缩放。</p>
 */
public class ScytheReaperRenderer extends GeoEntityRenderer<ScytheReaper> {

    public ScytheReaperRenderer(EntityRendererProvider.Context context) {
        super(context, new ScytheReaperModel());
    }
}
