package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * 烈焰蝾螈掉落处理器（与 {@link BeiZhiSuDropHandler} 同一套 LivingDropsEvent 掷骰模式）：
 * <ul>
 *   <li>正常死亡（含被玩家击杀）：固定掉落 1 个烈焰粉；另有 0.12% 基础概率掉落「火与钢」，
 *       抢夺每级使概率相对提升 25%（+0.03 个百分点/级，抢夺 III 时 0.21%）。</li>
 *   <li>低血量自爆：致死伤害来源为烈焰蝾螈自身（{@code explosion(this, this)}），
 *       此时清空掉落——「自爆时没有掉落物」。</li>
 * </ul>
 *
 * <p>抢夺等级须自行从击杀者武器读取（本版 {@code LivingDropsEvent} 无 getLootingLevel）。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class FlameSalamanderDropHandler {

    // 「火与钢」基础掉落概率：0.12%
    private static final double FIRE_AND_STEEL_CHANCE = 0.0012D;
    // 每级抢夺的掉率增量：基础的 25% = +0.03 个百分点/级
    private static final double CHANCE_PER_LOOTING = FIRE_AND_STEEL_CHANCE * 0.25D;

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (victim.getType() != infinitycraft.FLAME_SALAMANDER.get()) return;

        DamageSource source = event.getSource();

        // 自爆致死：来源是自身，不产生任何掉落
        if (source.getEntity() == victim) {
            event.getDrops().clear();
            return;
        }

        RandomSource random = victim.getRandom();

        // 烈焰粉：常规死亡固定掉落 1 个
        event.getDrops().add(new ItemEntity(
                victim.level(), victim.getX(), victim.getY(), victim.getZ(),
                new ItemStack(Items.BLAZE_POWDER)));

        // 抢夺等级：仅当击杀者为玩家时读取其武器上的抢夺
        int lootingLevel = 0;
        if (source.getEntity() instanceof Player killer) {
            Holder<Enchantment> looting = victim.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LOOTING);
            lootingLevel = EnchantmentHelper.getEnchantmentLevel(looting, killer);
        }

        // 火与钢：0.12% 基础 + 每级抢夺 0.03%
        double chance = FIRE_AND_STEEL_CHANCE + CHANCE_PER_LOOTING * lootingLevel;
        if (random.nextDouble() < chance) {
            event.getDrops().add(new ItemEntity(
                    victim.level(), victim.getX(), victim.getY(), victim.getZ(),
                    new ItemStack(infinitycraft.FIRE_AND_STEEL.get())));
        }
    }
}
