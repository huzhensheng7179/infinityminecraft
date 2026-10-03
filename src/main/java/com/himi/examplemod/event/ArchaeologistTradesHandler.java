package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;
import java.util.Optional;

/**
 * 考古学者村民交易处理器。
 * <p>
 * 交易分三档（对应村民 1-5 级）：
 * <ul>
 *   <li>基础档（1-2 级）：饰纹碎片(1-3) 换资源——绿宝石/青金石/金/钻石/下界合金碎片，成本与数量均在区间内随机。</li>
 *   <li>中档（3 级）：钻石(12-32) 或绿宝石(50-64) 换锻造模板，含下界合金升级模板与随机饰纹模板。</li>
 *   <li>顶档（4-5 级）：钻石/绿宝石/饰纹碎片（可组合）换本模组物品。</li>
 * </ul>
 * 每笔交易在生成时随机确定陶片种类与各数量，故不同村民/不同刷新呈现出多样报价。
 * getOffer 返回普通 {@link MerchantOffer}（而非子类），避免持久化后丢失自定义逻辑。
 * <p>
 * 事件在数据重载（TagsUpdated）时对每个职业触发一次，故此处仅在职业为考古学者时填充交易表。
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class ArchaeologistTradesHandler {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != infinityminecraft.ARCHAEOLOGIST.get()) {
            return;
        }

        // 货币/货物池：从物品标签解析全部饰纹碎片与饰纹锻造模板
        List<Item> sherds = resolveTag(event, ItemTags.DECORATED_POT_SHERDS);
        List<Item> trimTemplates = resolveTag(event, ItemTags.TRIM_TEMPLATES);
        if (sherds.isEmpty()) {
            return;
        }

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        // ===== 基础档 · 1 级（新手）：碎片 → 基础资源 =====
        trades.get(1).add(new Builder().costPool(sherds, 1, 3).reward(Items.EMERALD, 7, 12).uses(16).xp(3).build());
        trades.get(1).add(new Builder().costPool(sherds, 1, 3).reward(Items.LAPIS_LAZULI, 24, 48).uses(16).xp(3).build());
        trades.get(1).add(new Builder().costPool(sherds, 1, 3).reward(Items.GOLD_INGOT, 12, 24).uses(16).xp(3).build());

        // ===== 基础档 · 2 级（老手）：碎片 → 珍贵资源 =====
        trades.get(2).add(new Builder().costPool(sherds, 1, 3).reward(Items.DIAMOND, 4, 8).uses(12).xp(8).build());
        trades.get(2).add(new Builder().costPool(sherds, 1, 3).reward(Items.NETHERITE_SCRAP, 1, 3).uses(8).xp(12).build());

        // ===== 中档 · 3 级（专家）：钻石/绿宝石 → 锻造模板 =====
        trades.get(3).add(new Builder().cost(Items.DIAMOND, 12, 32)
                .reward(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, 1).uses(4).xp(20).build());
        if (!trimTemplates.isEmpty()) {
            trades.get(3).add(new Builder().cost(Items.EMERALD, 50, 64)
                    .rewardPool(trimTemplates, 1, 1).uses(4).xp(20).build());
            trades.get(3).add(new Builder().cost(Items.DIAMOND, 12, 32)
                    .rewardPool(trimTemplates, 1, 1).uses(4).xp(20).build());
        }

        // ===== 顶档 · 4 级（大师）：钻石/绿宝石/碎片 → 模组食物·饮品·饰品 =====
        trades.get(4).add(new Builder().cost(Items.EMERALD, 16, 32)
                .reward(infinityminecraft.QIAOLEZI.get(), 1, 1).uses(4).xp(20).build());
        trades.get(4).add(new Builder().cost(Items.EMERALD, 16, 32)
                .reward(infinityminecraft.XUEBI.get(), 1, 1).uses(4).xp(20).build());
        trades.get(4).add(new Builder().costPool(sherds, 8, 16)
                .reward(infinityminecraft.CRUDE_SPICY_CANDY.get(), 1, 1).uses(4).xp(20).build());
        trades.get(4).add(new Builder().cost(Items.DIAMOND, 16, 24).costPoolB(sherds, 6, 10)
                .reward(infinityminecraft.NAILONG_MASK.get(), 1, 1).uses(2).xp(30).build());

        // ===== 顶档 · 5 级（宗师）：钻石/绿宝石 + 碎片 → 强力饰品 =====
        trades.get(5).add(new Builder().cost(Items.DIAMOND, 24, 32).costPoolB(sherds, 8, 12)
                .reward(infinityminecraft.BING_BING_BING.get(), 1, 1).uses(2).xp(30).build());
        trades.get(5).add(new Builder().cost(Items.EMERALD, 48, 64).costPoolB(sherds, 8, 12)
                .reward(infinityminecraft.MYSTERIOUS_COIN.get(), 1, 1).uses(2).xp(30).build());
        trades.get(5).add(new Builder().cost(Items.DIAMOND, 24, 32).costPoolB(sherds, 8, 12)
                .reward(infinityminecraft.NUT_WALL.get(), 1, 1).uses(2).xp(30).build());
    }

    /** 从物品标签解析出具体物品列表。 */
    private static List<Item> resolveTag(VillagerTradesEvent event, net.minecraft.tags.TagKey<Item> tag) {
        return event.getRegistryAccess()
                .lookupOrThrow(Registries.ITEM)
                .getOrThrow(tag)
                .stream()
                .map(Holder::value)
                .toList();
    }

    /**
     * 可变成本/货物的交易条目：成本 A、可选成本 B、货物均支持"固定物品"或"从池中随机"，
     * 且数量在 [min, max] 区间内随机。每次 getOffer 现算，返回普通 MerchantOffer。
     */
    private static final class Listing implements VillagerTrades.ItemListing {
        private final Item costAItem;
        private final List<Item> costAPool;
        private final int costAMin;
        private final int costAMax;
        private final Item costBItem;
        private final List<Item> costBPool;
        private final int costBMin;
        private final int costBMax;
        private final Item rewardItem;
        private final List<Item> rewardPool;
        private final int rewardMin;
        private final int rewardMax;
        private final int maxUses;
        private final int xp;
        private final float priceMult;

        Listing(Builder b) {
            this.costAItem = b.costAItem;
            this.costAPool = b.costAPool;
            this.costAMin = b.costAMin;
            this.costAMax = b.costAMax;
            this.costBItem = b.costBItem;
            this.costBPool = b.costBPool;
            this.costBMin = b.costBMin;
            this.costBMax = b.costBMax;
            this.rewardItem = b.rewardItem;
            this.rewardPool = b.rewardPool;
            this.rewardMin = b.rewardMin;
            this.rewardMax = b.rewardMax;
            this.maxUses = b.maxUses;
            this.xp = b.xp;
            this.priceMult = b.priceMult;
        }

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            ItemCost costA = makeCost(costAItem, costAPool, costAMin, costAMax, random);
            Optional<ItemCost> costB = (costBItem == null && costBPool == null)
                    ? Optional.empty()
                    : Optional.of(makeCost(costBItem, costBPool, costBMin, costBMax, random));
            Item reward = (rewardPool != null && !rewardPool.isEmpty())
                    ? rewardPool.get(random.nextInt(rewardPool.size()))
                    : rewardItem;
            ItemStack result = new ItemStack(reward, roll(rewardMin, rewardMax, random));
            return new MerchantOffer(costA, costB, result, maxUses, xp, priceMult);
        }

        private static ItemCost makeCost(Item fixed, List<Item> pool, int min, int max, RandomSource random) {
            Item item = (pool != null && !pool.isEmpty()) ? pool.get(random.nextInt(pool.size())) : fixed;
            return new ItemCost(item, roll(min, max, random));
        }

        private static int roll(int min, int max, RandomSource random) {
            return max > min ? min + random.nextInt(max - min + 1) : min;
        }
    }

    /** {@link Listing} 的流式构造器。 */
    private static final class Builder {
        private Item costAItem;
        private List<Item> costAPool;
        private int costAMin = 1;
        private int costAMax = 1;
        private Item costBItem;
        private List<Item> costBPool;
        private int costBMin = 1;
        private int costBMax = 1;
        private Item rewardItem;
        private List<Item> rewardPool;
        private int rewardMin = 1;
        private int rewardMax = 1;
        private int maxUses = 8;
        private int xp = 5;
        private float priceMult = 0.05F;

        Builder cost(Item item, int min, int max) {
            this.costAItem = item;
            this.costAMin = min;
            this.costAMax = max;
            return this;
        }

        Builder costPool(List<Item> pool, int min, int max) {
            this.costAPool = pool;
            this.costAMin = min;
            this.costAMax = max;
            return this;
        }

        Builder costB(Item item, int min, int max) {
            this.costBItem = item;
            this.costBMin = min;
            this.costBMax = max;
            return this;
        }

        Builder costPoolB(List<Item> pool, int min, int max) {
            this.costBPool = pool;
            this.costBMin = min;
            this.costBMax = max;
            return this;
        }

        Builder reward(Item item, int min, int max) {
            this.rewardItem = item;
            this.rewardMin = min;
            this.rewardMax = max;
            return this;
        }

        Builder rewardPool(List<Item> pool, int min, int max) {
            this.rewardPool = pool;
            this.rewardMin = min;
            this.rewardMax = max;
            return this;
        }

        Builder uses(int maxUses) {
            this.maxUses = maxUses;
            return this;
        }

        Builder xp(int xp) {
            this.xp = xp;
            return this;
        }

        Listing build() {
            return new Listing(this);
        }
    }
}
