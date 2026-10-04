package com.himi.examplemod.mixin;

import com.himi.examplemod.item.LostAncientBookItem;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让“失落古籍”在附魔读写时被当作附魔书处理。
 *
 * <p>{@link EnchantmentHelper#getComponentType(ItemStack)} 原版只对 {@code Items.ENCHANTED_BOOK}
 * 返回 {@link DataComponents#STORED_ENCHANTMENTS}，否则返回 {@link DataComponents#ENCHANTMENTS}。
 * 铁砧正是靠此方法判定“书类”物品（{@code AnvilMenu.createResult} 读取 STORED_ENCHANTMENTS
 * 作为可转移的附魔来源）。这里追加：失落古籍同样使用 STORED_ENCHANTMENTS，从而：</p>
 * <ul>
 *   <li>附魔台的 {@code stack.enchant} 会把超限附魔写入 STORED_ENCHANTMENTS；</li>
 *   <li>铁砧把失落古籍当作附魔书，将其中的附魔转移给目标物品。</li>
 * </ul>
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getComponentType", at = @At("HEAD"), cancellable = true)
    private static void infinityminecraft$lostBookUsesStored(ItemStack stack,
            CallbackInfoReturnable<DataComponentType<ItemEnchantments>> cir) {
        if (stack.getItem() instanceof LostAncientBookItem) {
            cir.setReturnValue(DataComponents.STORED_ENCHANTMENTS);
        }
    }
}
