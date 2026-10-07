package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;

/**
 * 淬火之刃事件处理器。
 * 只要背包（含快捷栏/副手/盔甲栏）内存在淬火之刃：
 * - 玩家使用铁砧不消耗经验、且不会“过于昂贵”（具体绕过逻辑见 {@link com.himi.examplemod.mixin.AnvilMenuMixin}）；
 * - 每从铁砧取出一次成品（含改名/修复/合成/附魔书），淬火之刃的计数 +1；
 * - 计数达到 30 时，淬火之刃就地变形为“在烈焰中永恒”剑。
 *
 * 计数以 minecraft:custom_data 组件（键 {@value #KEY_USES}）保存在淬火之刃物品上，随物品一同存储。
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class TemperedBladeHandler {

    public static final int TRANSFORM_USES = 30;      // 变形所需的铁砧使用次数
    private static final String KEY_USES = "AnvilUses"; // custom_data 中记录次数的键

    /**
     * 玩家从铁砧取出成品时触发：为背包中的淬火之刃累计使用次数，满 30 次则变形为剑。
     */
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.is(infinitycraft.TEMPERED_BLADE.get())) continue;

            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            int uses = tag.getInt(KEY_USES) + 1;

            if (uses >= TRANSFORM_USES) {
                // 变形：用“在烈焰中永恒”剑替换淬火之刃
                inv.setItem(i, new ItemStack(infinitycraft.ETERNAL_IN_FLAMES.get()));
                if (player.level() instanceof ServerLevel sl) {
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    sl.sendParticles(ParticleTypes.FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 1.0, 0.5, 0.05);
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 1.0, 0.5, 0.3);
                }
            } else {
                tag.putInt(KEY_USES, uses);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                inv.setItem(i, stack);
            }
            break; // 只处理背包中找到的第一把淬火之刃
        }
    }

    /**
     * 判断玩家背包（含快捷栏/副手/盔甲栏）内是否存在淬火之刃。
     * 供 mixin 在铁砧逻辑中查询是否启用“免费铁砧”效果。
     */
    public static boolean hasTemperedBlade(Player player) {
        if (player == null) return false;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(infinitycraft.TEMPERED_BLADE.get())) {
                return true;
            }
        }
        return false;
    }
}
