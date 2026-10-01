package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 火与钢事件处理器。
 * 装备在 Curios 胸饰（body）栏位时：
 * - 免疫火焰伤害（IS_FIRE：火焰、岩浆等），并 extinguish 自身火焰；
 * - 近战攻击命中时，为目标附加火焰（点燃）；
 * - 攻击“已经处于燃烧状态”的目标时，无视护甲且伤害提升为原来的 5 倍；
 * - 每次近战攻击额外附加目标最大生命值 5% 的固定伤害（无视护甲/抗性）。
 *
 * 伤害管线要点（经反编译源码核验，NeoForge 21.1.252 / MC 1.21.1）：
 * 1. 无视护甲必须在 {@link LivingIncomingDamageEvent} 阶段通过
 *    {@code addReductionModifier(Reduction.ARMOR, ...)} 将护甲减免归零——
 *    这是管线中唯一能让减免修改器生效的阶段；
 * 2. {@link LivingDamageEvent.Pre} 在护甲/附魔/抗性减免之后、吸收之前触发，
 *    此处 {@code setNewDamage} 追加的固定伤害天然绕过护甲与抗性；
 * 3. “已燃烧”判定必须在点燃之前捕获，故点燃放在 Pre 末尾（读取 wasBurning 之后）。
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class FireAndSteelHandler {

    private static final float FIRE_SECONDS = 4.0F;         // 攻击附加火焰的秒数（≈火焰附加 I）
    private static final float BURNING_DAMAGE_MULTIPLIER = 5.0F; // 攻击燃烧目标的伤害倍率
    private static final float MAX_HEALTH_FIXED_RATIO = 0.05F;   // 每次攻击附加的目标最大生命固定伤害比例

    /**
     * 进攻端（无视护甲）+ 防御端（火焰免疫）均在伤害进入管线最早期处理。
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        // 防御端：佩戴火与钢的玩家免疫火焰伤害
        if (victim instanceof Player defender && hasFireAndSteel(defender)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }

        // 进攻端：佩戴火与钢的玩家近战攻击“已燃烧”目标时无视护甲
        if (event.getSource().getEntity() instanceof Player attacker
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && hasFireAndSteel(attacker)
                && victim.isOnFire()) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reductionIn) -> 0.0F);
        }
    }

    /**
     * 伤害结算：攻击燃烧目标 ×5，并每次攻击附加目标 5% 最大生命的固定伤害。
     * 固定伤害在护甲/抗性减免之后追加，故无视护甲与抗性。
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return; // 仅近战攻击
        if (!hasFireAndSteel(attacker)) return;

        LivingEntity victim = event.getEntity();
        boolean wasBurning = victim.isOnFire();

        float base = event.getNewDamage();
        float fixed = victim.getMaxHealth() * MAX_HEALTH_FIXED_RATIO;
        if (wasBurning) {
            event.setNewDamage(base * BURNING_DAMAGE_MULTIPLIER + fixed);
        } else {
            event.setNewDamage(base + fixed);
        }

        // 附加火焰（点燃），放在读取 wasBurning 之后，避免影响本次“已燃烧”判定
        victim.igniteForSeconds(FIRE_SECONDS);
    }

    /**
     * 佩戴火与钢时，持续熄灭自身火焰，实现彻底的火焰免疫表现。
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.getRemainingFireTicks() <= 0) return;
        if (!hasFireAndSteel(player)) return;
        player.clearFire();
    }

    private static boolean hasFireAndSteel(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.FIRE_AND_STEEL.get())))
                .isPresent();
    }
}
