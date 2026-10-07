package com.himi.examplemod.mixin;

import com.himi.examplemod.item.LostAncientBookItem;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧菜单 mixin：当牺牲物（右侧输入，槽位 1）是“失落古籍”时，放宽附魔等级上限钳制。
 *
 * <p>{@code AnvilMenu.createResult} 在合并附魔时会执行
 * {@code if (j2 > enchantment.getMaxLevel()) j2 = enchantment.getMaxLevel();}，
 * 这会把失落古籍携带的超限等级（如锋利 VIII）钳回原版上限（锋利 V）。
 * 这里在牺牲物为失落古籍时，把 {@link Enchantment#getMaxLevel()} 的返回值临时提高
 * {@link LostAncientBookItem#MAX_OVER_LIMIT} 级，使超限等级得以保留；其它情况完全保持原版行为。</p>
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuLostBookMixin {

    @Unique
    private boolean infinitycraft$superLimit;

    // 每次计算结果前，检测牺牲物是否为失落古籍
    @Inject(method = "createResult", at = @At("HEAD"))
    private void infinitycraft$detectLostBook(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        this.infinitycraft$superLimit = self.getSlot(1).getItem().getItem() instanceof LostAncientBookItem;
    }

    // 放宽等级钳制：牺牲物为失落古籍时，上限提升 MAX_OVER_LIMIT 级
    @Redirect(method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
    private int infinitycraft$raiseMaxLevel(Enchantment enchantment) {
        int maxLevel = enchantment.getMaxLevel();
        return this.infinitycraft$superLimit ? maxLevel + LostAncientBookItem.MAX_OVER_LIMIT : maxLevel;
    }
}
