package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 粗制辣味糖果事件处理器。
 * 装备在 Curios 护符栏位时：
 * - 免疫冰冻伤害：取消所有 {@link DamageTypeTags#IS_FREEZING} 类型的伤害；
 * - 免疫体温过低：持续将冰冻计时（ticksFrozen）清零，使玩家永远不会进入/维持冰冻状态，
 *   从而既不会累积冰冻进度，也不会触发原版每 40 ticks 的冰冻伤害。
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class CrudeSpicyCandyHandler {

    /**
     * 免疫冰冻伤害：任何带 IS_FREEZING 标签的伤害直接取消。
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypeTags.IS_FREEZING)) return;
        if (!hasCrudeSpicyCandy(player)) return;
        event.setCanceled(true);
    }

    /**
     * 免疫体温过低：将冰冻计时清零，阻止冰冻进度累积与冰冻覆盖层显示。
     * 客户端与服务端均处理，保证覆盖层平滑消失。
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.getTicksFrozen() <= 0) return;
        if (!hasCrudeSpicyCandy(player)) return;
        player.setTicksFrozen(0);
    }

    /**
     * 检查玩家是否在 Curios 护符栏位装备了粗制辣味糖果。
     */
    private static boolean hasCrudeSpicyCandy(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.CRUDE_SPICY_CANDY.get())))
                .isPresent();
    }
}
