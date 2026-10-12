package com.himi.examplemod.client.model;

import com.himi.examplemod.entity.CoinBlastEntity;
import com.himi.examplemod.infinitycraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 神秘硬币终点 3D 爆炸的 GeckoLib 模型资源绑定。贴图为完全不透明的蓝色能量爆发（textures/entity/coin_blast.png）。
 */
public class CoinBlastModel extends GeoModel<CoinBlastEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "geo/coin_blast.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "animations/coin_blast.animation.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "textures/entity/coin_blast.png");

    @Override
    public ResourceLocation getModelResource(CoinBlastEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CoinBlastEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CoinBlastEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(CoinBlastEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
