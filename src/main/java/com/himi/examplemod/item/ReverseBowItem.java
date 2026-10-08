package com.himi.examplemod.item;

import com.himi.examplemod.infinitycraft;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.himi.examplemod.entity.BlockProjectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 反之弓：一把「反过来」的弓。
 *
 * <p>操作与原版弓一致（按住右键蓄力、松开释放），复用弓的蓄力贴图与拉弓动画；
 * 但不消耗、也不能发射箭——它发射的是<b>当前副手持有的方块</b>：</p>
 * <ul>
 *   <li>飞出的方块<b>不受重力</b>，沿瞄准方向直线飞行；</li>
 *   <li>命中<b>方块</b>时在目标面外侧<b>放置</b>该方块；</li>
 *   <li>命中<b>生物</b>时造成伤害，伤害随副手方块的<b>硬度</b>不同而不同；</li>
 *   <li>发射<b>岩浆块</b>时使目标燃烧；发射<b>雪块</b>时对目标造成冰冻伤害并附加缓慢；</li>
 *   <li>副手为空或不是可放置方块时，无法开始蓄力并在动作栏提示。</li>
 * </ul>
 *
 * <p>每发消耗副手 1 个方块（创造模式不消耗）。伤害公式集中在 {@link #computeDamage}。</p>
 */
public class ReverseBowItem extends BowItem {

    /** 弹射物初速系数：最终速度 = 蓄力比例 × 该值（与原版箭 power×3.0 类似量级）。 */
    private static final float PROJECTILE_SPEED = 2.6F;

    public ReverseBowItem(Item.Properties properties) {
        super(properties);
    }

    /** 副手是否为可发射的方块（有方块物品的非空气方块）。 */
    private static boolean isFirableBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() != Blocks.AIR;
    }

    /**
     * 开始蓄力：仅当副手持有可能放置的方块时才允许进入使用状态，否则失败并提示。
     * 两端都会调用（客户端播拉动动画，服务端做实际校验）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack offhand = player.getOffhandItem();
        if (!isFirableBlock(offhand)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("副手需要拿着一个方块，反之弓才能发射它"), true);
                player.playSound(SoundEvents.ITEM_BREAK, 0.5F, 0.6F);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 松开释放：根据蓄力时长计算威力，发射副手方块（服务端权威，客户端提前返回）。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || level.isClientSide) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!isFirableBlock(offhand)) {
            return;
        }
        int duration = Math.max(1, this.getUseDuration(stack, entity) - timeLeft);
        float power = BowItem.getPowerForTime(duration);
        if (power < 0.1F) {
            return;
        }

        BlockState state = ((BlockItem) offhand.getItem()).getBlock().defaultBlockState();
        float damage = computeDamage(power, state);

        BlockProjectile projectile = new BlockProjectile(infinitycraft.BLOCK_PROJECTILE.get(), level);
        projectile.setOwner(player);
        var eye = player.getEyePosition();
        projectile.moveTo(eye.x, eye.y, eye.z, player.getYRot(), player.getXRot());
        projectile.configure(state, damage);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                power * PROJECTILE_SPEED, 0.0F);
        level.addFreshEntity(projectile);

        // 每发消耗副手 1 个方块（创造不消耗）
        if (!player.getAbilities().instabuild) {
            offhand.shrink(1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /**
     * 依据蓄力比例与方块硬度换算伤害：{@code 伤害 = power × (2 + 1.2 × 硬度)}，硬度钳制到 [0,15]。
     * 集中在此便于调整。岩浆块/雪块等硬度低，主要靠命中特效。
     */
    public static float computeDamage(float power, BlockState state) {
        float hardness = state.getBlock().defaultDestroyTime();
        if (hardness < 0.0F) {
            hardness = 15.0F; // 不可破坏方块（如基岩）按最高档处理
        }
        hardness = Math.min(hardness, 15.0F);
        return power * (2.0F + 1.2F * hardness);
    }
}
