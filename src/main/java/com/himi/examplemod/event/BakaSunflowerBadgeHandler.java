package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * baka向日葵吧唧事件处理器。
 * 装备在任意 Curios 饰品栏位时：
 * - 周围光照等级 > 7 时获得生命回复 V
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class BakaSunflowerBadgeHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (!hasBakaSunflowerBadge(player)) return;

        // 获取玩家位置的光照等级
        int lightLevel = player.level().getMaxLocalRawBrightness(player.blockPosition());

        if (lightLevel > 7) {
            // 施加生命回复 V（持续40ticks=2秒，每tick刷新）
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 4, false, true));
        }
    }

    /**
     * 检查玩家是否在任意 Curios 栏位装备了 baka向日葵吧唧
     */
    private static boolean hasBakaSunflowerBadge(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.BAKA_SUNFLOWER_BADGE.get())))
                .isPresent();
    }
}
