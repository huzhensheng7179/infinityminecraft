package com.himi.examplemod.item;

import com.himi.examplemod.event.WorldSlashHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 世界斩：手持右键使用的终极咒缚。
 *
 * <p>沿视线锁定鼠标指向的位置（最远 {@value #MAX_RANGE} 格；未命中方块时取射线终点），
 * 随后交由 {@link WorldSlashHandler} 在 5 秒内降下 15 道斩击——每斩附带多色粒子切开效果与挥砍音效，
 * 对半径 3 格内除施法者外的所有存活实体造成 100 点伤害（无视护甲/抗性/无敌帧）并使其定身；
 * 15 斩结束后清除范围内残存活物，最后施法者因咒缚付出生命。</p>
 *
 * <p>客户端仅播放手持动画，全部结算在服务端进行；同一名玩家在一次世界斩结束前无法重复施放。</p>
 */
public class WorldSlashItem extends Item {

    private static final double MAX_RANGE = 16.0D; // 鼠标指向位置的最远锁定距离

    public WorldSlashItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        // 客户端仅播放手持动画
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        // 已有一次世界斩进行中，忽略重复施放
        if (WorldSlashHandler.isActive(player)) {
            return InteractionResultHolder.fail(stack);
        }

        // 沿视线射线定位鼠标指向的位置（最远 16 格；MISS 时 getLocation() 即射线终点）
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(MAX_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getLocation();

        WorldSlashHandler.start((ServerLevel) level, player, target);
        return InteractionResultHolder.consume(stack);
    }
}
