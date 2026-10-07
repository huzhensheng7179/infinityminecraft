package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Map;

/**
 * 铡刃附魔处理器：击杀“可掉落头颅”的生物时，按附魔等级将头颅掉率变为固定值。
 *
 * <ul>
 *   <li>掉率：1 级 12% / 2 级 24% / 3 级 36% / 4 级 48%（每级 +12%）。</li>
 *   <li>适用生物：与原版“闪电苦力怕击杀掉头”机制一致——僵尸、骷髅、苦力怕、猪灵、凋灵骷髅
 *       （尸壳/溺尸的 getSkull() 返回 EMPTY、流浪者不掉头颅，故已排除）。</li>
 * </ul>
 *
 * <p>附魔本体在 1.21 为数据驱动（{@code data/infinitycraft/enchantment/guillotine.json}，
 * 适用于剑与斧 {@code #minecraft:enchantable/sharp_weapon}）；掉率逻辑在结算掉落物的
 * {@link LivingDropsEvent} 中处理：先移除原版可能已掉的同款头颅（如凋灵骷髅），再按附魔概率
 * 掷骰，使头颅掉率严格“变成”附魔规定值。击杀者须为玩家、读取其主手武器上的铡刃等级；仅服务端结算。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class GuillotineHandler {

    // 每级头颅掉率增量：1 级 12% / 2 级 24% / 3 级 36% / 4 级 48%
    private static final double DROP_CHANCE_PER_LEVEL = 0.12;

    // 可掉落头颅的生物 -> 对应头颅物品（与原版闪电苦力怕掉落机制保持一致）
    private static final Map<EntityType<?>, Item> HEAD_DROPS = Map.of(
            EntityType.ZOMBIE, Items.ZOMBIE_HEAD,
            EntityType.SKELETON, Items.SKELETON_SKULL,
            EntityType.CREEPER, Items.CREEPER_HEAD,
            EntityType.PIGLIN, Items.PIGLIN_HEAD,
            EntityType.WITHER_SKELETON, Items.WITHER_SKELETON_SKULL
    );

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        // 仅处理“可掉落头颅”的生物
        Item headItem = HEAD_DROPS.get(victim.getType());
        if (headItem == null) return;

        // 击杀者必须是玩家（读取其主手武器上的铡刃等级）
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player killer)) return;

        Holder<Enchantment> guillotine = victim.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(infinitycraft.GUILLOTINE);
        int level = EnchantmentHelper.getEnchantmentLevel(guillotine, killer);
        if (level <= 0) return;

        // 移除原版已掉的同款头颅（如凋灵骷髅的凋灵骷髅头颅），使掉率严格变为附魔规定值
        event.getDrops().removeIf(drop -> drop.getItem().is(headItem));

        // 按等级掷骰：命中则掉落一个头颅
        RandomSource random = victim.getRandom();
        if (random.nextDouble() < DROP_CHANCE_PER_LEVEL * level) {
            event.getDrops().add(new ItemEntity(
                    victim.level(), victim.getX(), victim.getY(), victim.getZ(), new ItemStack(headItem)));
        }
    }
}
