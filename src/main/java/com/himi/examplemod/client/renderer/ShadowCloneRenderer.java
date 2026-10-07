package com.himi.examplemod.client.renderer;

import java.util.UUID;

import com.himi.examplemod.entity.ShadowClone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * 影流分身渲染器：复用原版玩家模型（{@link PlayerModel}），并绑定召唤者（owner）的皮肤贴图，
 * 使分身外观与玩家完全一致；无法取到召唤者皮肤时回退到默认皮肤。始终不显示名字。
 */
public class ShadowCloneRenderer extends LivingEntityRenderer<ShadowClone, PlayerModel<ShadowClone>> {

    public ShadowCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    // 影流分身不显示任何名字
    @Override
    protected boolean shouldShowName(ShadowClone entity) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowClone entity) {
        UUID owner = entity.getOwnerUUID();
        if (owner != null) {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                PlayerInfo info = connection.getPlayerInfo(owner);
                if (info != null) {
                    return info.getSkin().texture();
                }
            }
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
