package com.himi.examplemod.mixin;

import com.himi.examplemod.event.TemperedBladeHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 铁砧菜单 mixin：当玩家背包内有“淬火之刃”时，让铁砧操作免费且不会“过于昂贵”。
 *
 * 三处改动（均以“持有淬火之刃”为前提，否则完全保持原版行为）：
 * 1. {@code createResult} 中两处 `cost.get() >= 40`（过于昂贵）判定：读取花费时若持有淬火之刃且
 *    实际花费 >= 40，则返回 39，使判定为假，成品不会被清空（避免“过于昂贵”）。
 * 2. {@code mayPickup}：持有淬火之刃且存在有效成品（cost>0）时，无视经验等级要求，允许取出。
 * 3. {@code onTake}：持有淬火之刃时跳过 giveExperienceLevels，实现不消耗经验。
 * 4. {@code createResult} 末尾把显示花费压到 1，避免客户端显示一个红色的高额数字。
 *
 * 玩家引用在菜单构造时从 {@code Inventory.player}（public）捕获，避免 shadow 父类字段。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Unique
    private Player infinityminecraft$player;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void infinityminecraft$capturePlayer(int containerId, Inventory playerInventory,
                                                 ContainerLevelAccess access, CallbackInfo ci) {
        this.infinityminecraft$player = playerInventory.player;
    }

    @Unique
    private boolean infinityminecraft$freeAnvil() {
        return TemperedBladeHandler.hasTemperedBlade(this.infinityminecraft$player);
    }

    // 绕过“过于昂贵”：createResult 中读取 cost 时，持有淬火之刃且 cost>=40 则返回 39
    @Redirect(method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;get()I"))
    private int infinityminecraft$bypassTooExpensive(DataSlot slot) {
        int cost = slot.get();
        if (cost >= 40 && infinityminecraft$freeAnvil()) {
            return 39;
        }
        return cost;
    }

    // createResult 末尾把显示花费压到 1（仅在存在有效成品时），避免红色高额数字
    @Inject(method = "createResult", at = @At("TAIL"))
    private void infinityminecraft$normalizeCost(CallbackInfo ci) {
        if (((AnvilMenu) (Object) this).getCost() > 0 && infinityminecraft$freeAnvil()) {
            ((AnvilMenu) (Object) this).setMaximumCost(1);
        }
    }

    // 允许取出成品：持有淬火之刃且有有效成品时，无视经验等级要求
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void infinityminecraft$mayPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        if (((AnvilMenu) (Object) this).getCost() > 0 && infinityminecraft$freeAnvil()) {
            cir.setReturnValue(true);
        }
    }

    // 不消耗经验：持有淬火之刃时跳过扣除经验等级
    @Redirect(method = "onTake",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperienceLevels(I)V"))
    private void infinityminecraft$noXpCost(Player player, int levels) {
        if (!infinityminecraft$freeAnvil()) {
            player.giveExperienceLevels(levels);
        }
    }
}
