package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import com.himi.examplemod.loot.SetSuperEnchantmentsFunction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * 战利品表注入：把“失落古籍（超限附魔形态）”加入到所有原版奖励箱中。
 *
 * <p>凡是命名空间为 {@code minecraft} 且路径以 {@code chests/} 开头的战利品表，都会被追加一个战利品池，
 * 保证开启任意原版奖励箱时都会额外掉落一本已附带超限附魔的失落古籍（与附魔书在箱中出现类似）。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class LostAncientBookLootHandler {

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (!"minecraft".equals(name.getNamespace()) || !name.getPath().startsWith("chests/")) {
            return;
        }

        event.getTable().addPool(LootPool.lootPool()
                .name("infinitycraft:lost_ancient_book")
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(infinitycraft.LOST_ANCIENT_BOOK.get())
                        .apply(SetSuperEnchantmentsFunction.builder()))
                .build());
    }
}
