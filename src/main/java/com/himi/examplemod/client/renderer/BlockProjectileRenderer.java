package com.himi.examplemod.client.renderer;

import com.himi.examplemod.entity.BlockProjectile;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 方块弹射物渲染器：用 {@link ItemRenderer} 把携带的方块以其物品模型渲染成一个立方体，
 * 沿飞行方向直线运动时保持朝向相机（与掉落物类似的 billboard 方块观感）。
 */
public class BlockProjectileRenderer extends EntityRenderer<BlockProjectile> {

    private final ItemRenderer itemRenderer;

    public BlockProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(BlockProjectile entity, float entityYaw, float partialTicks,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ItemStack stack = new ItemStack(entity.getStoredBlockState().getBlock());
        poseStack.pushPose();
        poseStack.scale(0.9F, 0.9F, 0.9F);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BlockProjectile entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
