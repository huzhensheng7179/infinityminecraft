package com.himi.examplemod.network;

import com.himi.examplemod.infinitycraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 「死亡回归」复活特效包（服务端 → 客户端）。
 *
 * <p>死亡回归复活成功后由服务端发给当事玩家；客户端据此播放不死图腾弹窗动画
 * （显示「死亡回归复活特效」沙漏贴图），见 {@code client.DefyDeathClientGui#playRevivePop}。
 * 无携带数据，故用 {@link StreamCodec#unit} 编解码。</p>
 */
public record ReviveEffectPayload() implements CustomPacketPayload {

    public static final Type<ReviveEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "revive_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReviveEffectPayload> STREAM_CODEC =
            StreamCodec.unit(new ReviveEffectPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
