package com.himi.examplemod.network;

import com.himi.examplemod.infinityminecraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 「死亡回归」请求包（客户端 → 服务端）。
 *
 * <p>玩家在死亡界面点击「死亡回归」按钮时发送；服务端校验确已死亡且有记录点后，在记录坐标复活。
 * 无携带数据，故用 {@link StreamCodec#unit} 编解码。</p>
 */
public record DeathReturnPayload() implements CustomPacketPayload {

    public static final Type<DeathReturnPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(infinityminecraft.MODID, "death_return"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathReturnPayload> STREAM_CODEC =
            StreamCodec.unit(new DeathReturnPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
