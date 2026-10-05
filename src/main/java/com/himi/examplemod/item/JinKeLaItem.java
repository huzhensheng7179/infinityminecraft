package com.himi.examplemod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 金坷垃：右键使用，兼具「作物催熟」与「骨粉的全部效果」。
 * <ul>
 *   <li>对作物方块（{@link CropBlock}，如小麦/胡萝卜/马铃薯/甜菜根）随机催熟 2~4 个生长阶段，
 *       不超过该作物的最大阶段；</li>
 *   <li>对其余任何可施骨粉的方块（{@link BonemealableBlock}），等同于对其使用骨粉：
 *       对草方块生成草与花、对甜浆果丛催熟、对树苗/甘蔗/仙人掌等催长……
 *       具体行为由各方块自身的 {@code performBonemeal} 决定；</li>
 *   <li>可无限使用——不消耗物品、无耐久；</li>
 *   <li>已成熟的作物、无法再施骨粉的方块无效果。</li>
 * </ul>
 */
public class JinKeLaItem extends Item {

    private static final int MIN_GROW = 2;
    private static final int MAX_GROW = 4;

    public JinKeLaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 可作用目标判定：①未成熟的作物（走特殊催熟 2~4 阶段）；②任意可施骨粉的方块（走通用骨粉逻辑）。
        boolean cropTarget = block instanceof CropBlock crop && crop.getAge(state) < crop.getMaxAge();
        boolean bonemealTarget = block instanceof BonemealableBlock bm
                && bm.isValidBonemealTarget(level, pos, state);
        if (!cropTarget && !bonemealTarget) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            RandomSource random = level.getRandom();
            if (block instanceof CropBlock crop) {
                // 作物：随机推进 2~4 个生长阶段
                int maxAge = crop.getMaxAge();
                int age = crop.getAge(state);
                int grow = random.nextIntBetweenInclusive(MIN_GROW, MAX_GROW);
                int newAge = Math.min(age + grow, maxAge);
                level.setBlock(pos, state.setValue(CropBlock.AGE, newAge), Block.UPDATE_ALL);
            } else {
                // 其余可施骨粉方块：等同原版骨粉（草方块生草/花、甜浆果催熟等）
                BonemealableBlock bm = (BonemealableBlock) block;
                if (bm.isBonemealSuccess(level, random, pos, state)) {
                    bm.performBonemeal(serverLevel, random, pos, state);
                }
            }
            // 催熟反馈：骨粉音效 + 绿色生长粒子（堆肥粒子）
            level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                    12, 0.4D, 0.4D, 0.4D, 0.0D);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
