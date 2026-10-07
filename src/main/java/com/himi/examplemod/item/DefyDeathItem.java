package com.himi.examplemod.item;

import com.himi.examplemod.event.DefyDeathHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 撼动死亡！：胸饰饰品。
 * <ul>
 *   <li>拿在手上时右键记录当前坐标（存于玩家持久化数据，死亡/重生/重登后仍保留）；</li>
 *   <li>记录后在物品效果栏（tooltip）与通知栏显示「已记录」；</li>
 *   <li>玩家死亡时死亡界面出现「死亡回归」选项，点击后在记录坐标复活（极限模式亦生效），
 *       见 {@code network.ModNetwork} 的 DeathReturnPayload 与 {@code client.DefyDeathClientGui}。</li>
 * </ul>
 */
public class DefyDeathItem extends Item {

    public DefyDeathItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        // 客户端仅播放手持动画；记录由服务端完成后再经同步包回传客户端
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos point = player.blockPosition();
        DefyDeathHandler.setRecordedPoint(player, point);
        if (player instanceof ServerPlayer serverPlayer) {
            DefyDeathHandler.syncToClient(serverPlayer);
        }

        // 通知栏（action bar）告知已记录
        player.displayClientMessage(
                Component.translatable("message.infinitycraft.death_recorded",
                        point.getX(), point.getY(), point.getZ()),
                true);

        return InteractionResultHolder.consume(stack);
    }
}
