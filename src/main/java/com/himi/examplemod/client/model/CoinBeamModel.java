package com.himi.examplemod.client.model;

import com.himi.examplemod.entity.CoinBeamEntity;
import com.himi.examplemod.infinitycraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 神秘硬币 3D 光束的 GeckoLib 模型资源绑定：几何体、动画、贴图。
 * 贴图为完全不透明的蓝色能量条纹（textures/entity/coin_beam.png），避免 alpha 剔除导致不可见。
 */
public class CoinBeamModel extends GeoModel<CoinBeamEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "geo/coin_beam.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "animations/coin_beam.animation.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "textures/entity/coin_beam.png");

    @Override
    public ResourceLocation getModelResource(CoinBeamEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CoinBeamEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CoinBeamEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(CoinBeamEntity animatable, ResourceLocation texture) {
        // 不剔除背面：光束为空心长方体，需能看到全部面形成体积感
        return RenderType.entityCutoutNoCull(texture);
    }
}
