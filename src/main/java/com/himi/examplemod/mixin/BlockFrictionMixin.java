package com.himi.examplemod.mixin;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 方块摩擦 mixin：让带有「巧乐兹风暴」效果的实体在任意方块上都像在冰面上一样滑行。
 *
 * 原理（经反编译源码核验，NeoForge 21.1.252 / MC 1.21.1）：
 * - {@code LivingEntity.travel} 计算地面摩擦时调用
 *   {@code blockState.getFriction(level, pos, entity)}（IBlockStateExtension 默认方法），
 *   后者委托到 {@code Block.getFriction(state, level, pos, entity)}（IBlockExtension 默认方法），
 *   默认返回方块自身的基础摩擦（普通方块 0.6，冰 0.98）。
 * - 该 4 参方法是带实体上下文的唯一收敛点。此处注入：当实体带有巧乐兹风暴效果时，
 *   直接返回冰面摩擦 0.98，使所有方块对该实体都变得湿滑。
 *
 * 注：这是接口默认方法注入，mixin 类须为接口、注入器须为 default 方法。
 */
@Mixin(IBlockExtension.class)
public interface BlockFrictionMixin {

    @Inject(method = "getFriction(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F",
            at = @At("HEAD"), cancellable = true)
    default void infinityminecraft$chocoSlippery(BlockState state, LevelReader level, BlockPos pos, Entity entity,
                                                 CallbackInfoReturnable<Float> cir) {
        if (entity instanceof LivingEntity living && living.hasEffect(infinityminecraft.CHOCO_STORM)) {
            cir.setReturnValue(0.98F);
        }
    }
}
