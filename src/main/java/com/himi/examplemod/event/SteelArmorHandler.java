package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.damagesource.DamageTypes;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 钢甲系列效果处理器。
 * 装备在 Curios body 槽位时，根据材质等级提供不同效果：
 * - 金+：猪灵不会主动攻击
 * - 钻石+：驱赶幻翼
 * - 下界合金+：永久抗火
 * - 下界之星：免疫魔法伤害
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class SteelArmorHandler {

    /**
     * 获取玩家装备的钢甲等级（0=无, 1=铁, 2=金, 3=钻石, 4=下界合金, 5=下界之星）
     */
    private static int getSteelArmorTier(Player player) {
        var optional = CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack -> isSteelArmor(stack)));
        if (optional.isEmpty()) return 0;

        Item item = optional.get().stack().getItem();
        if (item == infinityminecraft.NETHER_STAR_STEEL_ARMOR.get()) return 5;
        if (item == infinityminecraft.NETHERITE_STEEL_ARMOR.get()) return 4;
        if (item == infinityminecraft.DIAMOND_STEEL_ARMOR.get()) return 3;
        if (item == infinityminecraft.GOLD_STEEL_ARMOR.get()) return 2;
        if (item == infinityminecraft.IRON_STEEL_ARMOR.get()) return 1;
        return 0;
    }

    private static boolean isSteelArmor(ItemStack stack) {
        Item item = stack.getItem();
        return item == infinityminecraft.IRON_STEEL_ARMOR.get()
                || item == infinityminecraft.GOLD_STEEL_ARMOR.get()
                || item == infinityminecraft.DIAMOND_STEEL_ARMOR.get()
                || item == infinityminecraft.NETHERITE_STEEL_ARMOR.get()
                || item == infinityminecraft.NETHER_STAR_STEEL_ARMOR.get();
    }

    /**
     * 幻翼不会在穿戴钻石+钢甲的玩家附近生成
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Phantom phantom)) return;
        if (event.getLevel().isClientSide()) return;

        for (Player player : event.getLevel().getEntitiesOfClass(Player.class,
                phantom.getBoundingBox().inflate(80))) {
            if (getSteelArmorTier(player) >= 3) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * 玩家 Tick 事件：猪灵中立 + 抗火效果
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        int tier = getSteelArmorTier(player);

        // 金+：每秒清除猪灵对玩家的仇恨
        if (tier >= 2 && player.tickCount % 20 == 0) {
            player.level().getEntitiesOfClass(AbstractPiglin.class,
                    player.getBoundingBox().inflate(32)).forEach(piglin -> {
                if (piglin.getTarget() == player) {
                    piglin.setTarget(null);
                }
            });
        }

        // 下界合金+：永久抗火
        if (tier >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
        }
    }

    /**
     * 穿戴下界之星钢甲时免疫魔法伤害
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        if (getSteelArmorTier(player) < 5) return;

        if (event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC)) {
            event.setCanceled(true);
        }
    }

    /**
     * 受伤后增加额外无敌帧：铁+0.1s, 金+0.2s, 钻石+0.3s, 下界合金+0.4s, 下界之星+0.5s
     */
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        int tier = getSteelArmorTier(player);
        if (tier >= 1) {
            // 每级增加 2 ticks (0.1秒) 的无敌时间
            player.invulnerableTime += tier * 2;
        }
    }
}
