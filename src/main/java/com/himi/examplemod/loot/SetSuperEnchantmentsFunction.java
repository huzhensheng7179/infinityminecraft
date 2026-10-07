package com.himi.examplemod.loot;

import java.util.List;

import com.himi.examplemod.infinitycraft;
import com.himi.examplemod.item.LostAncientBookItem;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * 战利品函数：为奖励箱中的“失落古籍”随机附上 1~3 条超限附魔（等级落在 [原上限+1, 原上限+3]）。
 *
 * <p>用于让失落古籍在原版奖励箱中以“已附魔形态”出现（与附魔书类似）。候选附魔为非宝藏类附魔，
 * 并保证彼此兼容（不出现锋利/亡灵杀手等冲突组合）。附魔写入 STORED_ENCHANTMENTS
 * （由 {@code EnchantmentHelperMixin.getComponentType} 分流）。</p>
 */
public class SetSuperEnchantmentsFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetSuperEnchantmentsFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).apply(instance, SetSuperEnchantmentsFunction::new));

    public SetSuperEnchantmentsFunction(List<LootItemCondition> predicates) {
        super(predicates);
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return infinitycraft.SET_SUPER_ENCHANTMENTS.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        // 附魔逻辑统一交给 LostAncientBookItem.applySuperEnchantments（石球/考古注入共用）
        return LostAncientBookItem.applySuperEnchantments(stack,
                context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT),
                context.getRandom());
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return simpleBuilder(SetSuperEnchantmentsFunction::new);
    }
}
