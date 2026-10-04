package com.himi.examplemod.event;

import java.util.List;

import com.himi.examplemod.archaeology.StoneBallRewardTable;
import com.himi.examplemod.infinityminecraft;
import com.himi.examplemod.loot.SetSuperEnchantmentsFunction;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * 战利品表注入：把“神秘石球”与“考古产物（模组部分）”加入到原版考古战利品表中。
 *
 * <p>凡是命名空间为 {@code minecraft} 且路径以 {@code archaeology/} 开头的战利品表
 * （desert_pyramid、desert_well、ocean_ruins_cold/warm、trail_ruins_common/rare），都会追加战利品池：
 * 约 35% 概率产出一个神秘石球；另有约 6% 概率产出一个随机「无合成表模组物品」，
 * 且按强度分档加权（强度越高权重越低，见 {@link StoneBallRewardTable#weightOf}）；
 * 还有约 6%（{@link StoneBallRewardTable#LOST_BOOK_CHANCE}）概率额外产出一本已附魔形态的失落古籍。</p>
 *
 * <p>考古产物与失落古籍均受 {@link StoneBallRewardTable#isBlacklisted} 全局黑名单过滤（配置文件可剔除）；
 * 神秘石球本身作为入口物品不受黑名单影响。因战利品表在加载时构建，刷子黑名单的改动需 /reload 或重启生效。</p>
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class ArchaeologyLootHandler {

    private static final float DROP_CHANCE = 0.35F;
    /** 刷子额外掉落一个随机「考古产物（模组部分）」的概率。 */
    private static final float MOD_ITEM_CHANCE = 0.06F;

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (!"minecraft".equals(name.getNamespace()) || !name.getPath().startsWith("archaeology/")) {
            return;
        }

        event.getTable().addPool(LootPool.lootPool()
                .name("infinityminecraft:mysterious_stone_ball")
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(DROP_CHANCE))
                .add(LootItem.lootTableItem(infinityminecraft.MYSTERIOUS_STONE_BALL.get()))
                .build());

        // 失落古籍：约 6% 概率额外产出一本已附魔（超限）形态的古籍（若未被全局黑名单剔除）
        if (!StoneBallRewardTable.isBlacklisted(infinityminecraft.LOST_ANCIENT_BOOK.get())) {
            event.getTable().addPool(LootPool.lootPool()
                    .name("infinityminecraft:lost_ancient_book")
                    .setRolls(ConstantValue.exactly(1.0F))
                    .when(LootItemRandomChanceCondition.randomChance(StoneBallRewardTable.LOST_BOOK_CHANCE))
                    .add(LootItem.lootTableItem(infinityminecraft.LOST_ANCIENT_BOOK.get())
                            .apply(SetSuperEnchantmentsFunction.builder()))
                    .build());
        }

        // 考古产物（模组部分）：约 6% 概率额外掉落 1 个随机「无合成表模组物品」，按强度分档加权，
        // 并剔除全局黑名单中的物品（逐个 LootItem + setWeight 实现加权，而非 expandTag 的均等权重）。
        HolderLookup.Provider registries = event.getRegistries();
        if (registries != null) {
            List<Item> products = registries.lookupOrThrow(Registries.ITEM)
                    .getOrThrow(StoneBallRewardTable.ARCHAEOLOGY_PRODUCTS)
                    .stream().map(Holder::value)
                    .filter(item -> !StoneBallRewardTable.isBlacklisted(item))
                    .toList();
            if (!products.isEmpty()) {
                LootPool.Builder productPool = LootPool.lootPool()
                        .name("infinityminecraft:archaeology_products")
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(MOD_ITEM_CHANCE));
                for (Item item : products) {
                    productPool.add(LootItem.lootTableItem(item).setWeight(StoneBallRewardTable.weightOf(item)));
                }
                event.getTable().addPool(productPool.build());
            }
            // products 为空（标签为空或全被黑名单剔除）时不注入该池
        } else {
            // 兜底：事件未携带注册表时，退回均等权重的标签展开（此路径无法过滤黑名单，极罕见）
            event.getTable().addPool(LootPool.lootPool()
                    .name("infinityminecraft:archaeology_products")
                    .setRolls(ConstantValue.exactly(1.0F))
                    .when(LootItemRandomChanceCondition.randomChance(MOD_ITEM_CHANCE))
                    .add(TagEntry.expandTag(StoneBallRewardTable.ARCHAEOLOGY_PRODUCTS))
                    .build());
        }
    }
}
