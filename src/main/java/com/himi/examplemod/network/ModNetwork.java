package com.himi.examplemod.network;

import com.himi.examplemod.client.ClientDefyDeathState;
import com.himi.examplemod.client.DefyDeathClientGui;
import com.himi.examplemod.event.BeiZhiSuHandler;
import com.himi.examplemod.event.DefyDeathHandler;

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
        // 「死亡回归」：客户端死亡界面按钮 → 服务端在记录坐标复活
        registrar.playToServer(
                DeathReturnPayload.TYPE,
                DeathReturnPayload.STREAM_CODEC,
                ModNetwork::handleDeathReturn);
        // 「撼动死亡！」记录点同步：服务端 → 客户端
        registrar.playToClient(
                DefyDeathSyncPayload.TYPE,
                DefyDeathSyncPayload.STREAM_CODEC,
                ModNetwork::handleDefyDeathSync);
        // 「死亡回归」复活特效：服务端 → 客户端播放不死图腾弹窗（沙漏贴图）
        registrar.playToClient(
                ReviveEffectPayload.TYPE,
                ReviveEffectPayload.STREAM_CODEC,
                ModNetwork::handleReviveEffect);
    }

    /** 服务端处理：死亡界面点击「死亡回归」后，登记本次重生回标记点（实际重生由客户端的 PERFORM_RESPAWN 触发）。 */
    private static void handleDeathReturn(final DeathReturnPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DefyDeathHandler.requestDeathReturn(player);
            }
        });
    }

    /** 客户端处理：更新记录点状态，供 tooltip 与死亡界面按钮使用。 */
    private static void handleDefyDeathSync(final DefyDeathSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> ClientDefyDeathState.apply(payload.has(), payload.point()));
    }

    /** 客户端处理：播放不死图腾弹窗（死亡回归复活特效）。仅客户端执行，故延迟加载客户端类安全。 */
    private static void handleReviveEffect(final ReviveEffectPayload payload, final IPayloadContext context) {
        context.enqueueWork(DefyDeathClientGui::playRevivePop);
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
