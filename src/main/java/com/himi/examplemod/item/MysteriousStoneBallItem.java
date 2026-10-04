package com.himi.examplemod.item;

import com.himi.examplemod.archaeology.StoneBallRewardTable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 神秘石球：考古获得的谜之石球，手持右键“敲开”。
 *
 * <p>开球需要背包/副手中有一把任意品质的镐子（{@link ItemTags#PICKAXES}，含其它模组镐），
 * 消耗该镐子 3 点耐久并消耗 1 个石球，随机产出考古产物/矿物/经验（常见）或锻造模板/附魔金苹果/
 * 本模组物品/其它模组最高稀有度物品（极小概率）。奖励池见 {@link StoneBallRewardTable}。</p>
 */
public class MysteriousStoneBallItem extends Item {

    private static final int DURABILITY_COST = 3;

    public MysteriousStoneBallItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack ball = player.getItemInHand(usedHand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(ball);
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // 需要一把镐子才能敲开
        ItemStack pickaxe = findPickaxe(player, usedHand);
        if (pickaxe.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("\u00A7b\u795E\u79D8\u77F3\u7403\u00A7r \u9700\u8981\u4E00\u628A\u9550\u5B50\u624D\u80FD\u6572\u5F00"),
                    true);
            return InteractionResultHolder.fail(ball);
        }

        // 消耗镐子 3 点耐久（正确处理耐久附魔/损坏）
        pickaxe.hurtAndBreak(DURABILITY_COST, serverLevel, player, broken -> {
        });

        // 随机产出
        RandomSource random = serverLevel.getRandom();
        StoneBallRewardTable.Reward reward = StoneBallRewardTable.roll(serverLevel, random);

        Vec3 pos = player.position().add(0.0D, 0.5D, 0.0D);
        for (ItemStack drop : reward.items()) {
            if (!drop.isEmpty()) {
                player.spawnAtLocation(drop);
            }
        }
        if (reward.experience() > 0) {
            ExperienceOrb.award(serverLevel, pos, reward.experience());
        }

        // 消耗 1 个石球
        ball.shrink(1);

        // 粒子 + 音效反馈
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 24, 0.3D, 0.3D, 0.3D, 0.02D);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F);

        // 动作栏告知本次产出
        player.displayClientMessage(buildFeedback(reward), true);

        return InteractionResultHolder.consume(ball);
    }

    /** 在另一只手与背包主栏中查找任意品质的镐子。 */
    private static ItemStack findPickaxe(Player player, InteractionHand usedHand) {
        InteractionHand other = usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack offHand = player.getItemInHand(other);
        if (offHand.is(ItemTags.PICKAXES)) {
            return offHand;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ItemTags.PICKAXES)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 组织动作栏提示文本。 */
    private static Component buildFeedback(StoneBallRewardTable.Reward reward) {
        if (reward.experience() > 0) {
            return Component.literal("\u00A7b\u795E\u79D8\u77F3\u7403\u00A7r \u8D4B\u4E88\u4E86 \u00A7a"
                    + reward.experience() + "\u00A7r \u70B9\u7ECF\u9A8C");
        }
        if (!reward.items().isEmpty()) {
            ItemStack first = reward.items().get(0);
            String name = first.getHoverName().getString();
            int count = reward.items().stream().mapToInt(ItemStack::getCount).sum();
            return Component.literal("\u00A7b\u795E\u79D8\u77F3\u7403\u00A7r \u5F00\u51FA\u4E86 \u00A7e"
                    + name + (count > 1 ? " x" + count : "") + "\u00A7r");
        }
        return Component.literal("\u00A7b\u795E\u79D8\u77F3\u7403\u00A7r \u4EC0\u4E48\u4E5F\u6CA1\u6709\u5F00\u51FA\u6765");
    }
}
