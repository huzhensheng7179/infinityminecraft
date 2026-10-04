package com.himi.examplemod.network;

import com.himi.examplemod.event.BeiZhiSuHandler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 模组网络包注册与处理。
 *
 * <p>当前注册一个客户端 → 服务端的空包 {@link OpenEnderChestPayload}：装备贝质素时按 C 键发送，
 * 服务端校验玩家确实在腰带栏装备了贝质素后，打开其末影箱界面。</p>
 */
public class ModNetwork {

    /** 在主模组类的 mod 事件总线上注册（{@code modEventBus.addListener(ModNetwork::register)}）。 */
    public static void register(final RegisterPayloadHandlersEvent event) {
        // 版本号用于协议协商；首版用 "1"
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                OpenEnderChestPayload.TYPE,
                OpenEnderChestPayload.STREAM_CODEC,
                ModNetwork::handleOpenEnderChest);
    }

    /** 服务端处理：仅当玩家装备了贝质素时打开末影箱。 */
    private static void handleOpenEnderChest(final OpenEnderChestPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && BeiZhiSuHandler.hasBeiZhiSu(player)) {
                openEnderChest(player);
            }
        });
    }

    private static void openEnderChest(final ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // 末影箱开启音效（服务端广播给周围所有玩家，含开启者本人）
        level.playSound(null, x, y, z, SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 1.0F, 1.0F);
        // 紫色末影粒子：绕玩家上升的传送门粒子柱 + 向内汇聚的反向传送门粒子
        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0D, z, 60, 0.6D, 1.2D, 0.6D, 0.25D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 0.3D, z, 30, 0.8D, 0.5D, 0.8D, 0.15D);

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) ->
                        ChestMenu.threeRows(containerId, inventory, player.getEnderChestInventory()),
                Component.translatable("container.enderchest")));
    }
}
