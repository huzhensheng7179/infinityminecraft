package com.himi.examplemod.event;

import com.himi.examplemod.entity.BlockProjectile;
import com.himi.examplemod.infinitycraft;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 「箭弓」发射黑曜石击杀恶魂的特殊奖励。
 *
 * <p>当一只<b>恶魂</b>被<b>箭弓发射的方块弹射物</b>（{@link BlockProjectile}）击杀，
 * 且该弹射物携带的方块为<b>黑曜石</b>、击杀者为<b>玩家</b>时：</p>
 * <ul>
 *   <li>在恶魂处<b>额外掉落</b> 1 个哭泣的黑曜石（原版恶魂掉落表中不含此物）；</li>
 *   <li>为击杀者授予隐藏成就「我的眼睛，我的眼睛......」
 *       （{@code advancement.infinitycraft.reverse_bow_ghast}，impossible 触发器 + 代码手动授予）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class ReverseBowGhastHandler {

    /** 隐藏成就的 advancement id（对应 data/infinitycraft/advancement/reverse_bow_ghast.json）。 */
    private static final ResourceLocation ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "reverse_bow_ghast");

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 仅处理恶魂的死亡
        if (!(event.getEntity() instanceof Ghast ghast)) {
            return;
        }
        // 击杀判定与掉落、成就授予均须服务端执行
        if (!(ghast.level() instanceof ServerLevel level)) {
            return;
        }
        // 致命伤害的直接来源必须是「箭弓」发射的方块弹射物
        if (!(event.getSource().getDirectEntity() instanceof BlockProjectile projectile)) {
            return;
        }
        // 携带方块必须是黑曜石
        if (projectile.getStoredBlockState().getBlock() != Blocks.OBSIDIAN) {
            return;
        }
        // 弹射物的主人（击杀归属）必须是玩家
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 额外掉落哭泣的黑曜石：落在恶魂处，短暂延迟拾取并抑制横向弹射，方便拾取
        ItemStack drop = new ItemStack(Items.CRYING_OBSIDIAN);
        ItemEntity itemEntity = new ItemEntity(level, ghast.getX(), ghast.getY(), ghast.getZ(), drop);
        itemEntity.setPickUpDelay(30);
        itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().multiply(0.5D, 1.0D, 0.5D));
        level.addFreshEntity(itemEntity);

        // 授予隐藏成就（impossible 触发器，仅在此处手动 award）
        AdvancementHolder advancement = level.getServer().getAdvancements().get(ADVANCEMENT);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "code");
        }
    }
}
