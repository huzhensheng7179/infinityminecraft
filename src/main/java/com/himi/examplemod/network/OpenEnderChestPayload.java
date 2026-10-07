package com.himi.examplemod.network;

import com.himi.examplemod.infinitycraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 打开末影箱的网络包（客户端 → 服务端）。
 *
 * <p>装备贝质素时按下 C 键，客户端发送此空包；服务端校验玩家确实装备了贝质素后打开末影箱界面。
 * 无携带数据，故用 {@link StreamCodec#unit} 编解码。</p>
 */
public record OpenEnderChestPayload() implements CustomPacketPayload {

    public static final Type<OpenEnderChestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "open_ender_chest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEnderChestPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenEnderChestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
