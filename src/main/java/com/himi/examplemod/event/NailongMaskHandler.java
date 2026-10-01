package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 奶龙面具事件处理器。
 * 装备在 Curios 头饰栏位时：
 * - 看向敌对生物时，使其无法移动（施加极速缓慢效果）
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class NailongMaskHandler {

    private static final double REACH_DISTANCE = 32.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (!hasNailongMask(player)) return;

        // 从玩家视线方向进行射线检测
        HitResult hitResult = player.pick(REACH_DISTANCE, 1.0F, false);
        if (hitResult instanceof EntityHitResult entityHitResult) {
            var target = entityHitResult.getEntity();
            // 检查是否为敌对生物
            if (target instanceof Mob mob && mob.getType().getCategory() == MobCategory.MONSTER) {
                // 施加极速缓慢效果，使其无法移动（持续5ticks，每tick刷新）
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 255, false, false));
            }
        }
    }

    private static boolean hasNailongMask(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.NAILONG_MASK.get())))
                .isPresent();
    }
}
