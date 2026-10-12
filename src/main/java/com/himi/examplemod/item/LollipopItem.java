package com.himi.examplemod.item;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 棒棒糖——糖果食物。
 *
 * <p>三重用途：</p>
 * <ul>
 *   <li>作为普通食物食用（对空气右键）：恢复饱食度与饱和度，吃完后清除食用者身上的<b>所有负面效果</b>。</li>
 *   <li>对着其他玩家右键：把棒棒糖喂给对方，清除<b>对方</b>的全部负面效果。</li>
 *   <li>对着成年动物右键：喂给对方，动物冒出爱心粒子并进入繁殖状态（等价于原版喂食求爱）。</li>
 * </ul>
 *
 * <p>后两种「喂给别人」的用法每次消耗 1 个棒棒糖（创造模式不消耗）；负面效果以原版
 * {@link MobEffectCategory#HARMFUL} 判定（缓慢、中毒、凋零这类有害状态），瞬间型效果本就不驻留、无需处理。</p>
 */
public class LollipopItem extends Item {
    public LollipopItem(Properties properties) {
        super(properties);
    }

    /** 吃完（自己食用）后清除自身所有负面效果。 */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide) {
            clearHarmfulEffects(livingEntity);
        }
        return result;
    }

    /** 对 living 实体右键：喂给玩家清负面、喂给成年动物触发繁殖。 */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();
        boolean didSomething = false;

        if (target instanceof Player targetPlayer) {
            // 喂给另一名玩家：清除其所有负面效果
            if (!level.isClientSide) {
                clearHarmfulEffects(targetPlayer);
            }
            didSomething = true;
        } else if (target instanceof Animal animal && !animal.isBaby() && !animal.isInLove()) {
            // 喂给成年且尚未求爱的动物：进入繁殖状态（setInLove 会广播实体事件 18，客户端据此渲染爱心粒子）
            if (!level.isClientSide) {
                animal.setInLove(player);
            }
            didSomething = true;
        }

        if (!didSomething) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && !player.isCreative()) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 移除实体身上所有分类为 HARMFUL 的状态效果（先快照再遍历，避免并发修改）。 */
    private static void clearHarmfulEffects(LivingEntity entity) {
        for (MobEffectInstance instance : List.copyOf(entity.getActiveEffects())) {
            Holder<MobEffect> effect = instance.getEffect();
            if (effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                entity.removeEffect(effect);
            }
        }
    }
}
