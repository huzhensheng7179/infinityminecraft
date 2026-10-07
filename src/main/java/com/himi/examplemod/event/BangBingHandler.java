package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 棒冰掉落处理器：破坏「冰块」（{@link Blocks#ICE}）时，有 1.2% 概率额外掉落一个「棒冰」胸饰。
 *
 * <ul>
 *   <li>触发条件：被破坏方块为冰块、玩家非创造模式；仅服务端结算（客户端 Level 非 ServerLevel，自动跳过）；</li>
 *   <li>与方块本身的掉落（是否精准采集）无关，只要破坏即按概率判定；</li>
 *   <li>掉落物在方块中心作为额外物品生成，沿用 {@code ProspectingHandler} 的范式。</li>
 * </ul>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class BangBingHandler {

    // 破坏冰块掉落棒冰的概率：1.2%
    private static final double DROP_CHANCE = 0.012;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return; // 仅服务端结算
        Player player = event.getPlayer();
        if (player.isCreative()) return; // 创造模式不产出

        BlockState state = event.getState();
        if (!state.is(Blocks.ICE)) return; // 仅「冰块」

        if (level.getRandom().nextDouble() < DROP_CHANCE) {
            BlockPos pos = event.getPos();
            level.addFreshEntity(new ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    new ItemStack(infinitycraft.BANG_BING.get())));
        }
    }
}
