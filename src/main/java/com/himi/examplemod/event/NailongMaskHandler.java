package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 奶龙面具事件处理器。
 * 装备在 Curios 头饰栏位时：
 * - 看向的「非玩家生物」无法移动（施加极速缓慢效果，每 tick 刷新，视线移开即恢复）
 *
 * 注意：这里用 ProjectileUtil.getEntityHitResult 做实体射线检测。
 * 不能用 Entity#pick —— 它在服务端只返回方块命中（BlockHitResult），
 * 永远不会是 EntityHitResult，会导致定身效果根本不触发。
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class NailongMaskHandler {

    private static final double REACH_DISTANCE = 32.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (!hasNailongMask(player)) return;

        // 从玩家视线方向做实体射线检测（32 格）
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(REACH_DISTANCE));
        AABB bounds = player.getBoundingBox().expandTowards(look.scale(REACH_DISTANCE)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(), player, eye, end, bounds,
                // 只命中非玩家的生物
                entity -> entity instanceof LivingEntity && !(entity instanceof Player));

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            // 施加极速缓慢效果，使其无法移动（持续 5 ticks，每 tick 刷新）
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 255, false, false));
        }
    }

    private static boolean hasNailongMask(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinitycraft.NAILONG_MASK.get())))
                .isPresent();
    }
}
