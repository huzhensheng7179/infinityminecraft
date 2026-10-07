package com.himi.examplemod.network;

import com.himi.examplemod.infinitycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 「撼动死亡！」记录点同步包（服务端 → 客户端）。
 *
 * <p>携带是否已记录与记录坐标；客户端据此在物品效果栏显示「已记录」并在死亡界面显示「死亡回归」按钮。</p>
 */
public record DefyDeathSyncPayload(boolean has, BlockPos point) implements CustomPacketPayload {

    public static final Type<DefyDeathSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "defy_death_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DefyDeathSyncPayload> STREAM_CODEC =
            StreamCodec.of(DefyDeathSyncPayload::write, DefyDeathSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, DefyDeathSyncPayload payload) {
        buf.writeBoolean(payload.has());
        if (payload.has()) {
            buf.writeBlockPos(payload.point());
        }
    }

    private static DefyDeathSyncPayload read(RegistryFriendlyByteBuf buf) {
        boolean has = buf.readBoolean();
        BlockPos point = has ? buf.readBlockPos() : null;
        return new DefyDeathSyncPayload(has, point);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
