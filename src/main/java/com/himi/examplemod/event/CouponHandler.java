package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 优惠券事件处理器。
 * 装备在 Curios 腰带栏位时：持续获得最高级（V，amplifier = 4）的「村庄英雄」效果，
 * 使村民交易享受最大折扣，并触发村庄英雄的其它增益。
 *
 * <p>实现方式与法棍护符一致：每 tick 刷新一个短时效（40 ticks = 2 秒）的效果实例，
 * 确保装备期间效果常驻、卸下后约 2 秒内自然消失。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class CouponHandler {

    /** 村庄英雄最高等级 V → amplifier = 4。 */
    private static final int HERO_AMPLIFIER = 4;

    /** 每次刷新的效果时长：40 ticks = 2 秒（每 tick 刷新，故装备期间常驻）。 */
    private static final int EFFECT_DURATION = 40;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (hasCoupon(player)) {
            // 每 tick 刷新村庄英雄 V，装备期间永久生效
            player.addEffect(new MobEffectInstance(
                    MobEffects.HERO_OF_THE_VILLAGE, EFFECT_DURATION, HERO_AMPLIFIER, false, true));
        }
    }

    /**
     * 检查玩家是否在 Curios 腰带栏位装备了优惠券。
     */
    private static boolean hasCoupon(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinitycraft.COUPON.get())))
                .isPresent();
    }
}
