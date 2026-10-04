package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 考古勘探附魔处理器：用带此附魔的镐子破坏「镐子适应的方块」（{@code minecraft:mineable/pickaxe}）时，
 * 按附魔等级有概率额外掉落一个神秘石球。
 *
 * <ul>
 *   <li>掉率：1 级 2% / 2 级 4% / 3 级 6%（每级 +2%，常规最高 3 级）。</li>
 *   <li>触发条件：破坏方块的主手工具带有本附魔、被破坏方块属于 {@code minecraft:mineable/pickaxe}、
 *       玩家非创造模式；仅服务端结算（客户端 Level 非 ServerLevel，自动跳过）。</li>
 * </ul>
 *
 * <p>附魔本体在 1.21 为数据驱动（{@code data/infinityminecraft/enchantment/archaeological_prospecting.json}，
 * 适用于镐子 {@code #minecraft:pickaxes}，最高 3 级）。神秘石球在方块中心作为额外掉落物生成。</p>
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class ProspectingHandler {

    // 每级神秘石球掉率增量：1 级 2% / 2 级 4% / 3 级 6%
    private static final double CHANCE_PER_LEVEL = 0.02;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return; // 仅服务端结算
        Player player = event.getPlayer();
        if (player.isCreative()) return; // 创造模式不产出

        BlockState state = event.getState();
        if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return; // 仅「镐子适应的方块」

        ItemStack tool = player.getMainHandItem();
        Holder<Enchantment> prospecting = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(infinityminecraft.ARCHAEOLOGICAL_PROSPECTING);
        int lvl = EnchantmentHelper.getItemEnchantmentLevel(prospecting, tool);
        if (lvl <= 0) return;

        if (level.getRandom().nextDouble() < CHANCE_PER_LEVEL * lvl) {
            BlockPos pos = event.getPos();
            level.addFreshEntity(new ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    new ItemStack(infinityminecraft.MYSTERIOUS_STONE_BALL.get())));
        }
    }
}
