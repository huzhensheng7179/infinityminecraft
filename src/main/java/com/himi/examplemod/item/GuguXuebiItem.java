package com.himi.examplemod.item;

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 「鼓鼓的雪碧」——一瓶被气撑得圆滚滚的汽水，本质是一枚烟花。
 *
 * <p>继承原版 {@link FireworkRocketItem}，并注册时自带 FIREWORKS 数据组件
 * （结构见 {@link #FIREWORKS}），因此原版的烟花行为全部白拿：</p>
 * <ul>
 *   <li>对着方块右键 → 沿用原版 {@code useOn}，在点击处发射；</li>
 *   <li>鞘翅滑行时右键 → 沿用原版 {@code use}，消耗 1 个加速；</li>
 *   <li>发射器/泼洒瓶等 {@code ProjectileItem} 通道同样可用。</li>
 * </ul>
 *
 * <p>本类只补了原版缺的一小块：<b>对空右键</b>时原版什么都不做（必须瞄着方块），
 * 这里改为直接从玩家眼前把汽水“喷”上天，手感更符合“一瓶憋不住的汽水”。</p>
 *
 * <p>飞行贴图无需额外处理：原版 {@code FireworkEntityRenderer} 渲染火箭时调的是
 * {@code itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, ...)}，
 * 即拿火箭携带的<b>物品自身模型/贴图</b>来画，所以手上、地上、天上都是同一张
 * {@code textures/item/gugu_xuebi.png}。</p>
 */
public class GuguXuebiItem extends FireworkRocketItem {

    /**
     * 固定的烟花结构：飞行 2 级（约 1.5 秒）后炸成一团大气泡——
     * 薄荷汽水色渐变为深瓶绿，带尾迹与闪烁。注册物品时作为默认组件写入。
     */
    public static final Fireworks FIREWORKS = new Fireworks(2, List.of(
            new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL,
                    IntList.of(0xBFF7E8, 0x66D9B8, 0xFFFFFF),
                    IntList.of(0x2F6E58),
                    true, true)));

    public GuguXuebiItem(Properties properties) {
        super(properties);
    }

    /** 对空右键：从眼前发射；鞘翅滑行时交回原版逻辑（当火箭加速用）。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isFallFlying()) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            FireworkRocketEntity rocket = new FireworkRocketEntity(level, player,
                    eye.x + look.x * 0.5, eye.y + look.y * 0.5, eye.z + look.z * 0.5, stack.copyWithCount(1));
            level.addFreshEntity(rocket);
            stack.shrink(1);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    /**
     * 刻意不调用 {@code super}：原版会把 FIREWORKS 组件摊成「飞行时间 / 形状 / 颜色 / 尾迹」等灰色行，
     * 与本模组统一的说明风格冲突；效果说明统一走 lang 的 item.infinitycraft.gugu_xuebi.desc。
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
    }
}
