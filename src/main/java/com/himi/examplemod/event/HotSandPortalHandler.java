package com.himi.examplemod.event;

import java.util.Optional;

import com.himi.examplemod.infinitycraft;
import com.himi.examplemod.worldgen.portal.HotSandPortalShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 热砂世界传送门点燃处理器：手持「无暇辰星」右键「哭泣的黑曜石」框架时，
 * 若构成合法矩形框架（内部为空气）则填充传送门方块并消耗一颗无暇辰星。
 *
 * <p>制造方式与原版下界传送门一致（框架 + 内部空气），仅框架材质改为哭泣的黑曜石、
 * 点火物改为无暇辰星；不修改 {@code flawless_star} 物品类本身，避免影响其锻造用途。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class HotSandPortalHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return; // 仅主手触发，避免双手重复
        Level level = event.getLevel();
        if (level.isClientSide()) return; // 仅服务端结算方块生成与消耗

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(infinitycraft.FLAWLESS_STAR.get())) return; // 手持无暇辰星

        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN)) return; // 右键哭泣的黑曜石框架

        // 检测矩形框架（先 X 轴后 Z 轴），要求内部无传送门方块
        Optional<HotSandPortalShape> shape = HotSandPortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X);
        if (shape.isEmpty()) return;

        shape.get().createPortalBlocks();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1); // 激活消耗一颗无暇辰星
        }

        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    40, 0.6, 0.8, 0.6, 0.02);
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
