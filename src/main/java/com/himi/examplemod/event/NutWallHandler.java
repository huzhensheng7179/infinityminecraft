package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * 坚果墙事件处理器。
 * 装备在 Curios 腰带栏位或副手时：
 * - 强制吸引半径 20 格内敌对生物的仇恨：将其攻击目标锁定为玩家，
 *   每 tick 复查，若目标不是玩家则重新强制设定，因此仇恨不会转移到其他单位
 * - 受到伤害时，获得抗性提升 IV 与缓慢 IV，持续 20 秒
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class NutWallHandler {

    private static final double TAUNT_RADIUS = 20.0;   // 吸引仇恨半径（格）
    private static final int BUFF_DURATION = 400;       // 效果持续：20 秒 = 400 ticks
    private static final int BUFF_AMPLIFIER = 3;        // 等级 4 → 放大器 3

    /**
     * 玩家 Tick：强制将半径内的敌对生物仇恨锁定到玩家（嘲讽，不转移仇恨）
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!hasNutWall(player)) return;

        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(TAUNT_RADIUS));

        for (Mob mob : mobs) {
            if (!mob.isAlive() || mob.isNoAi()) continue;
            // 仅吸引敌对类怪物（MONSTER 类别）
            if (mob.getType().getCategory() != MobCategory.MONSTER) continue;

            // 仇恨锁定：目标不是玩家时强制设定为玩家；每 tick 复查，防止仇恨被转移
            if (mob.getTarget() != player) {
                mob.setTarget(player);
            }
        }
    }

    /**
     * 受伤事件：获得抗性提升 IV 与缓慢 IV，持续 20 秒
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!hasNutWall(player)) return;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_DURATION, BUFF_AMPLIFIER, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BUFF_DURATION, BUFF_AMPLIFIER, false, true));
    }

    /**
     * 检查玩家是否在 Curios 腰带栏位或副手装备了坚果墙
     */
    private static boolean hasNutWall(Player player) {
        // 副手
        if (player.getOffhandItem().is(infinityminecraft.NUT_WALL.get())) {
            return true;
        }
        // Curios 腰带槽位
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.NUT_WALL.get())))
                .isPresent();
    }
}
