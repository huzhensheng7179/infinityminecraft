package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * 太阳之环事件处理器。
 * 装备在 Curios 头饰栏位时：每 tick 检测玩家周围半径 2 格内的敌对生物（{@link Enemy}），
 * 将其沿远离玩家的方向击退，力度约 2.5。
 * <p>
 * 击退方向说明：{@code knockback(strength, x, z)} 会把实体推向 -(x, z) 方向，
 * 故传入 (player - entity) 即可把实体推离玩家。一次击退通常足以把生物推出 2 格半径，
 * 因此该光环表现为"敌人无法贴近"的斥力场，且不会因每 tick 重复施力而无限加速
 * （knockback 内部按 1 - 击退抗性 衰减，速度亦有上限）。
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class SunRingHandler {

    private static final double RADIUS = 2.0;
    private static final double RADIUS_SQR = RADIUS * RADIUS;
    private static final double KNOCKBACK_STRENGTH = 2.5;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (!hasSunRing(player)) return;

        // 以玩家为中心的 2 格半径球形范围（AABB 用于粗筛，distanceToSqr 精确限定半径）
        AABB area = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> mobs = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(player) <= RADIUS_SQR);

        for (LivingEntity mob : mobs) {
            mob.knockback(KNOCKBACK_STRENGTH, player.getX() - mob.getX(), player.getZ() - mob.getZ());
        }
    }

    private static boolean hasSunRing(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.SUN_RING.get())))
                .isPresent();
    }
}
