package com.himi.examplemod.client;

import com.himi.examplemod.infinityminecraft;
import com.himi.examplemod.network.DeathReturnPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 「撼动死亡！」客户端界面：
 * <ul>
 *   <li>死亡界面（含极限模式）在已记录坐标时追加「死亡回归」按钮，点击发送 C2S 请求包；</li>
 *   <li>物品效果栏（tooltip）在已记录时追加绿色「已记录：x, y, z」行。</li>
 * </ul>
 */
@EventBusSubscriber(modid = infinityminecraft.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class DefyDeathClientGui {

    /** 播放不死图腾弹窗动画，显示「死亡回归复活特效」沙漏贴图（仅客户端）。 */
    public static void playRevivePop() {
        // 原版重生已重建本地玩家（状态正常、可操作），这里只需播放不死图腾弹窗动画
        Minecraft.getInstance().gameRenderer
                .displayItemActivation(new ItemStack(infinityminecraft.DEFY_DEATH_EFFECT.get()));
    }

    /** 死亡界面初始化后追加「死亡回归」按钮（仅当客户端已知有记录点）。 */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof DeathScreen)) {
            return;
        }
        if (!ClientDefyDeathState.has) {
            return;
        }
        Screen screen = event.getScreen();
        int x = screen.width / 2 - 100;
        int y = screen.height / 4 + 144;
        event.addListener(Button.builder(
                        Component.translatable("gui.infinityminecraft.death_return"),
                        button -> {
                            Minecraft mc = Minecraft.getInstance();
                            // 1) 登记本次重生回标记点（自定义包走 PacketDistributor）
                            PacketDistributor.sendToServer(new DeathReturnPayload());
                            // 2) 触发原版重生（原版包走连接通道；标准流程，复活后可正常操作）
                            if (mc.getConnection() != null) {
                                mc.getConnection().send(new ServerboundClientCommandPacket(
                                        ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                            }
                            mc.setScreen(null);
                        })
                .bounds(x, y, 200, 20)
                .build());
    }

    /** 物品 tooltip：已记录时在物品名下方追加绿色「已记录」行。 */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(infinityminecraft.DEFY_DEATH.get())) {
            return;
        }
        if (ClientDefyDeathState.has && ClientDefyDeathState.point != null) {
            BlockPos p = ClientDefyDeathState.point;
            event.getToolTip().add(1, Component.translatable(
                            "tooltip.infinityminecraft.recorded", p.getX(), p.getY(), p.getZ())
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
