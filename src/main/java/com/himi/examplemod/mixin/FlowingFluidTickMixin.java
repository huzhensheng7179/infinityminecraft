package com.himi.examplemod.mixin;

import com.himi.examplemod.infinitycraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 热砂世界流体速度 mixin：仅当流体所在维度为热砂世界（{@link infinitycraft#HOT_SAND_DIM}）时改写流动 tick 间隔。
 *
 * <p>原理（经反编译源码核验，NeoForge 21.1.252 / MC 1.21.1）：
 * {@code FlowingFluid} 在扩散/下降时通过 {@code getTickDelay(LevelReader)} 决定下次流体 tick 的延迟，
 * 其中 {@code LavaFluid.getTickDelay} 返回 {@code ultraWarm ? 10 : 30}（下界 10、主世界 30），
 * {@code WaterFluid.getTickDelay} 返回 {@code 5}。数值越小流动越快。</p>
 *
 * <p>本 mixin 同时注入两个子类的 {@code getTickDelay} 头部：热砂世界维度下
 * 岩浆返回 4（快于下界的 10）、水返回 20（慢于默认的 5），实现「岩浆更快、水更慢」。
 * 因两子类各自 override 了该方法，故用多目标 mixin + {@code instanceof} 区分。</p>
 */
@Mixin({ LavaFluid.class, WaterFluid.class })
public abstract class FlowingFluidTickMixin {

    @Inject(method = "getTickDelay(Lnet/minecraft/world/level/LevelReader;)I", at = @At("HEAD"), cancellable = true)
    private void infinitycraft$hotSandFluidSpeed(LevelReader level, CallbackInfoReturnable<Integer> cir) {
        if (level instanceof Level l && l.dimension() == infinitycraft.HOT_SAND_DIM) {
            Object self = this;
            if (self instanceof LavaFluid) {
                cir.setReturnValue(4);  // 岩浆：快于下界(10)
            } else if (self instanceof WaterFluid) {
                cir.setReturnValue(20); // 水：慢于默认(5)
            }
        }
    }
}
