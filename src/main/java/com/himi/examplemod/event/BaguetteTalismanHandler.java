package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 法棍护符事件处理器。
 * 装备在 Curios 护符栏位时：
 * - 永久提供饥饿 I 效果
 * - 受伤时叠加抗性提升（I→II→III），持续25秒
 * - 达到抗性提升 III 时不再触发，等待其消失后重新开始
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class BaguetteTalismanHandler {

    private static final int RESISTANCE_DURATION = 500; // 25秒 = 500 ticks

    /**
     * 玩家 Tick 事件：持续施加饥饿 I
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (hasBaguetteTalisman(player)) {
            // 每 tick 刷新饥饿 I（持续40ticks=2秒），确保装备期间永久生效
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 0, false, true));
        }
    }

    /**
     * 受伤事件：叠加抗性提升
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        if (!hasBaguetteTalisman(player)) return;

        // 检查当前抗性提升等级
        MobEffectInstance currentResistance = player.getEffect(MobEffects.DAMAGE_RESISTANCE);

        if (currentResistance == null) {
            // 没有抗性提升 → 施加 I
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_DURATION, 0, false, true));
        } else if (currentResistance.getAmplifier() == 0) {
            // 抗性提升 I → 升级为 II
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_DURATION, 1, false, true));
        } else if (currentResistance.getAmplifier() == 1) {
            // 抗性提升 II → 升级为 III
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_DURATION, 2, false, true));
        }
        // 抗性提升 III (amplifier == 2) 时不触发，等待自然消失后重新从 I 开始
    }

    /**
     * 检查玩家是否在 Curios 护符栏位装备了法棍护符
     */
    private static boolean hasBaguetteTalisman(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinitycraft.BAGUETTE_TALISMAN.get())))
                .isPresent();
    }
}
