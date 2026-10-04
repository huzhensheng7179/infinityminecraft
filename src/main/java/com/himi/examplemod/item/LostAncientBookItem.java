package com.himi.examplemod.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 失落古籍——一本可以进行“超限附魔”的古书。
 *
 * <p>与原版附魔书的行为保持一致：</p>
 * <ul>
 *   <li>可放入附魔台附魔：{@link #isEnchantable}/{@link #getEnchantmentValue}/{@link #isPrimaryItemFor}
 *       使其像书一样接受任意附魔；</li>
 *   <li>附魔结果写入 {@link DataComponents#STORED_ENCHANTMENTS} 组件（配合
 *       {@code EnchantmentHelperMixin.getComponentType}），因此可在铁砧上像附魔书一样把附魔转移给其它物品；</li>
 *   <li>存有附魔时显示附魔光效。</li>
 * </ul>
 *
 * <p>关键区别：{@link #applyEnchantments} 会把每条附魔的等级强制提升到超限区间
 * {@code [原上限+1, 原上限+3]}（例如锋利 V → 锋利 VI~VIII），即“必定超过原版附魔上限”。</p>
 */
public class LostAncientBookItem extends Item {
    /** 超限附魔相对原版上限的最大额外等级：等级落在 [max+1, max+MAX_OVER_LIMIT]。 */
    public static final int MAX_OVER_LIMIT = 3;

    /** 附魔台完成附魔时用于随机超限等级的随机源（仅服务端调用）。 */
    private static final RandomSource RANDOM = RandomSource.create();

    public LostAncientBookItem(Properties properties) {
        super(properties);
    }

    /** 与附魔书一致：单个且尚未存有附魔时才可再次放入附魔台。 */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1 && getStoredEnchantments(stack).isEmpty();
    }

    /** 高于原版书(1)，保证附魔台能刷出较高基础等级的附魔作为超限起点。 */
    @Override
    public int getEnchantmentValue() {
        return 20;
    }

    /** 像书一样，可作为任意附魔的载体（附魔台候选池过滤依据此方法）。 */
    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return true;
    }

    /** 像附魔书一样，只要存有附魔就显示附魔光效。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || !getStoredEnchantments(stack).isEmpty();
    }

    /**
     * 附魔台完成附魔时调用：把每条附魔的等级提升到超限区间 [max+1, max+3]，
     * 再经 {@code stack.enchant} 写入 STORED_ENCHANTMENTS（由 getComponentType mixin 分流）。
     */
    @Override
    public ItemStack applyEnchantments(ItemStack stack, List<EnchantmentInstance> enchantments) {
        for (EnchantmentInstance instance : enchantments) {
            stack.enchant(instance.enchantment, overLimitLevel(instance.enchantment, RANDOM));
        }
        return stack;
    }

    /** 读取古籍已存储的（超限）附魔。 */
    public static ItemEnchantments getStoredEnchantments(ItemStack stack) {
        return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /** 为给定附魔计算一个超限等级：[原上限+1, 原上限+3]。 */
    public static int overLimitLevel(Holder<Enchantment> enchantment, RandomSource random) {
        return enchantment.value().getMaxLevel() + 1 + random.nextInt(MAX_OVER_LIMIT);
    }

    /**
     * 为给定的失落古籍随机附上 1~3 条彼此兼容的附魔台附魔，等级落在超限区间 [原上限+1, 原上限+3]，
     * 写入 STORED_ENCHANTMENTS。奖励箱战利品函数、神秘石球与考古注入共用此逻辑，
     * 确保各途径产出的古籍形态一致（非失落古籍时原样返回）。
     */
    public static ItemStack applySuperEnchantments(ItemStack stack,
            HolderLookup.RegistryLookup<Enchantment> lookup, RandomSource random) {
        if (!(stack.getItem() instanceof LostAncientBookItem)) {
            return stack;
        }
        // 候选：附魔台可用附魔（IN_ENCHANTING_TABLE 标签，天然排除宝藏类如灵魂疾行/绑定诅咒）
        List<Holder<Enchantment>> pool = new ArrayList<>(
                lookup.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE).stream().toList());
        if (pool.isEmpty()) {
            return stack;
        }
        Util.shuffle(pool, random);
        int count = 1 + random.nextInt(3); // 1~3 条附魔
        List<Holder<Enchantment>> chosen = new ArrayList<>();
        for (Holder<Enchantment> candidate : pool) {
            if (chosen.size() >= count) {
                break;
            }
            boolean compatible = true;
            for (Holder<Enchantment> picked : chosen) {
                if (!Enchantment.areCompatible(picked, candidate)) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                chosen.add(candidate);
                stack.enchant(candidate, overLimitLevel(candidate, random));
            }
        }
        return stack;
    }
}
