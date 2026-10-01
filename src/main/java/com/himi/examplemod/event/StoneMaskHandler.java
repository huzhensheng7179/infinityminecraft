package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 石鬼面事件处理器。
 * 装备在 Curios 头饰栏位时：
 * - 造成的伤害增加 20%
 * - 攻击生物时，回复造成伤害 50% 的生命（吸血）
 * - 吃东西（进食）速度提升
 * - 暴露在阳光下时持续扣血
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class StoneMaskHandler {

    private static final float DAMAGE_MULTIPLIER = 1.2F;   // 伤害 +20%
    private static final float LIFESTEAL_RATIO = 0.5F;      // 吸血：造成伤害的 50%
    private static final float SUN_DAMAGE = 2.0F;           // 阳光每秒扣血量
    private static final int SUN_DAMAGE_INTERVAL = 20;      // 阳光扣血间隔（20 ticks = 1 秒）

    /**
     * 伤害加成：攻击者佩戴石鬼面时，造成的最终伤害 +20%
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.level().isClientSide()) return;
            if (hasStoneMask(attacker)) {
                event.setNewDamage(event.getNewDamage() * DAMAGE_MULTIPLIER);
            }
        }
    }

    /**
     * 吸血：攻击者佩戴石鬼面时，攻击生物回复造成伤害 50% 的生命
     */
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Mob)) return; // 仅对生物生效
        if (victim.level().isClientSide()) return;

        if (event.getSource().getEntity() instanceof Player attacker) {
            if (!attacker.isAlive()) return;
            if (hasStoneMask(attacker)) {
                float healed = event.getNewDamage() * LIFESTEAL_RATIO;
                if (healed > 0) {
                    attacker.heal(healed);
                }
            }
        }
    }

    /**
     * 进食加速：佩戴石鬼面时，吃/喝类物品使用速度提升（时长缩短为原来的 40%）
     */
    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (!stack.has(DataComponents.FOOD)) return; // 仅食物类物品生效
        if (!hasStoneMask(player)) return;
        event.setDuration(Math.max(1, (int) (event.getDuration() * 0.4F)));
    }

    /**
     * 阳光扣血：佩戴石鬼面时，暴露在阳光下每秒扣血
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;
        if (player.tickCount % SUN_DAMAGE_INTERVAL != 0) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!hasStoneMask(player)) return;

        if (isExposedToSunlight(player, level)) {
            player.hurt(player.damageSources().magic(), SUN_DAMAGE);
        }
    }

    /**
     * 判断实体是否暴露在阳光下（白天、可见天空、天空亮度足够、非雨雪、不在水中）
     */
    private static boolean isExposedToSunlight(Player player, Level level) {
        if (!level.isDay()) return false;
        if (level.isRaining() || level.isThundering()) return false;
        if (player.isInWater() || player.isInLava()) return false;

        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        if (!level.canSeeSky(pos)) return false;
        // 天空原始亮度（不受方块光影响），正午地表约为 15
        return level.getBrightness(LightLayer.SKY, pos) >= 12;
    }

    /**
     * 检查玩家是否在 Curios 头饰栏位装备了石鬼面
     */
    private static boolean hasStoneMask(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.STONE_MASK.get())))
                .isPresent();
    }
}
