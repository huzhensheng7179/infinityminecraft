package com.himi.examplemod.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.himi.examplemod.archaeology.StoneBallRewardTable;
import com.himi.examplemod.infinityminecraft;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * 流浪嗅探兽商人的交易生成器。
 *
 * <p>交易数量：初始生成 {@value #INITIAL_TRADES} 条；随玩家完成交易可成长，最多 {@value #MAX_TRADES} 条
 * （成长逻辑在实体 {@code WanderingSnifferMerchant} 中，通过 {@link #generateSingle} 追加）。</p>
 *
 * <p>交易物品池：扫描物品注册表，仅收录稀有度为 COMMON/UNCOMMON/RARE（排除 EPIC）且通过
 * {@link StoneBallRewardTable#isObtainableItem} 过滤（剔除管理员/生存不可获得/刷怪蛋/黑名单）的物品；
 * 额外排除「无意义物品」（成书、无附魔的附魔书、已填充地图等，见 {@link #EXCLUDED_MEANINGLESS}）；
 * 按命名空间分为「原版 / 本模组 / 其他模组」三组，其中其他模组权重较低（约 15%）。</p>
 *
 * <p>货币为火把花（torchflower）与瓶子草荚（pitcher_pod），单笔数量随稀有度升高：
 * COMMON 4~24 / UNCOMMON 16~64 / RARE 48~128，钳制在 [4,128]（最少 4、最多两组）。</p>
 *
 * <p>「以物易物」条目约占 {@value #BARTER_PERCENT}%：成本不限于火把花/瓶子草，而是从一组多样的
 * 常见物资（农作物、矿物、材料、考古陶片等，见 {@link #BARTER_COST_ITEMS}）中随机选取，换回一件
 * 较高稀有度（RARE 优先）物品；保证每次生成至少 2 条以物易物。</p>
 */
public final class SnifferMerchantTrades {

    private SnifferMerchantTrades() {
    }

    /** 交易货币（火把花、瓶子草荚）。 */
    private static final Item[] CURRENCIES = { Items.TORCHFLOWER, Items.PITCHER_POD };

    /** 无意义物品黑名单：成书、无附魔的附魔书、已填充地图等，单独作为货物没有意义。 */
    private static final Set<Item> EXCLUDED_MEANINGLESS = Set.of(
            Items.WRITTEN_BOOK,
            Items.ENCHANTED_BOOK,
            Items.FILLED_MAP);

    /** 初始生成的交易条数（最少 12）。 */
    public static final int INITIAL_TRADES = 12;
    /** 交易条数上限（随交易成长到最多 16）。 */
    public static final int MAX_TRADES = 16;
    /** 以物易物条目占比（%）。 */
    private static final int BARTER_PERCENT = 30;
    /** 每次生成至少保证的以物易物条数。 */
    private static final int MIN_BARTER = 2;

    /** 三组命名空间的抽取权重：原版 + 本模组为主，其他模组约 15%。 */
    private static final int WEIGHT_VANILLA = 50;
    private static final int WEIGHT_MOD = 35;
    private static final int WEIGHT_OTHER = 15;

    /** 货币数量下限/上限（最少 4、最多两组=128）。 */
    private static final int MIN_COST = 4;
    private static final int MAX_COST = 128;

    /** 以物易物的多样化成本物品（农作物/矿物/材料等，不限于火把花与瓶子草）。 */
    private static final Item[] BARTER_COST_ITEMS = {
            Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD,
            Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT,
            Items.SUGAR_CANE, Items.CACTUS, Items.MELON_SLICE, Items.PUMPKIN,
            Items.CLAY_BALL, Items.FLINT, Items.FEATHER, Items.LEATHER, Items.STRING,
            Items.COAL, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD,
            Items.AMETHYST_SHARD, Items.QUARTZ, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.LAPIS_LAZULI,
            Items.BONE, Items.SPIDER_EYE, Items.GUNPOWDER, Items.SLIME_BALL, Items.ROTTEN_FLESH,
            Items.NETHER_WART, Items.PRISMARINE_SHARD, Items.INK_SAC, Items.GLOW_INK_SAC,
            Items.HONEYCOMB, Items.EGG, Items.APPLE, Items.SWEET_BERRIES, Items.GLOW_BERRIES,
            Items.COCOA_BEANS, Items.BAMBOO, Items.KELP, Items.MOSS_BLOCK };

    /** 候选物品池：按命名空间分三组，另汇总所有 RARE 物品供以物易物奖励优先使用。 */
    private record Pools(List<Item> vanilla, List<Item> mod, List<Item> other, List<Item> rare) {
        boolean isEmpty() {
            return vanilla.isEmpty() && mod.isEmpty() && other.isEmpty();
        }
    }

    /** 生成初始一组随机交易（{@value #INITIAL_TRADES} 条，含至少 {@value #MIN_BARTER} 条以物易物）。 */
    public static MerchantOffers generate(RandomSource random, RegistryAccess registryAccess) {
        MerchantOffers offers = new MerchantOffers();
        Pools pools = buildPools(registryAccess);
        Set<Item> used = new HashSet<>();

        // 预随机选定以物易物的下标，保证至少 MIN_BARTER 条
        Set<Integer> barterIndex = new HashSet<>();
        int barterCount = Math.max(MIN_BARTER, INITIAL_TRADES * BARTER_PERCENT / 100);
        while (barterIndex.size() < barterCount) {
            barterIndex.add(random.nextInt(INITIAL_TRADES));
        }

        for (int i = 0; i < INITIAL_TRADES; i++) {
            MerchantOffer offer = barterIndex.contains(i)
                    ? makeBarter(pools, random, registryAccess, used)
                    : makeCurrencyTrade(pools, random, used);
            if (offer != null) {
                offers.add(offer);
            }
        }

        // 兜底：极端情况下（物品池为空）至少提供一条固定交易，避免打开空白交易界面
        if (offers.isEmpty()) {
            offers.add(new MerchantOffer(new ItemCost(Items.TORCHFLOWER, 8), Optional.empty(),
                    new ItemStack(Items.BREAD, 4), 8, 2, 0.0F));
        }
        return offers;
    }

    /** 生成单条交易，供实体在玩家完成交易后追加（成长到 {@value #MAX_TRADES} 条）。 */
    public static MerchantOffer generateSingle(RandomSource random, RegistryAccess registryAccess) {
        Pools pools = buildPools(registryAccess);
        Set<Item> used = new HashSet<>();
        MerchantOffer offer = random.nextInt(100) < BARTER_PERCENT
                ? makeBarter(pools, random, registryAccess, used)
                : makeCurrencyTrade(pools, random, used);
        if (offer == null) {
            offer = new MerchantOffer(new ItemCost(Items.TORCHFLOWER, 8), Optional.empty(),
                    new ItemStack(Items.BREAD, 4), 8, 2, 0.0F);
        }
        return offer;
    }

    /** 扫描注册表构建候选池（稀有度 ≤ RARE，剔除黑名单/刷怪蛋/货币本身/无意义物品）。 */
    private static Pools buildPools(RegistryAccess registryAccess) {
        List<Item> vanilla = new ArrayList<>();
        List<Item> mod = new ArrayList<>();
        List<Item> other = new ArrayList<>();
        List<Item> rare = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            // 货币本身不作为货物出售
            if (item == Items.TORCHFLOWER || item == Items.PITCHER_POD) {
                continue;
            }
            // 排除无意义物品（成书/无附魔附魔书/已填充地图等）
            if (EXCLUDED_MEANINGLESS.contains(item)) {
                continue;
            }
            if (!StoneBallRewardTable.isObtainableItem(item)) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }
            Rarity rarity = stack.getRarity();
            if (rarity == Rarity.EPIC) {
                continue; // 仅收录蓝色及以下（COMMON/UNCOMMON/RARE）
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String namespace = id.getNamespace();
            if ("minecraft".equals(namespace)) {
                vanilla.add(item);
            } else if (infinityminecraft.MODID.equals(namespace)) {
                mod.add(item);
            } else {
                other.add(item);
            }
            if (rarity == Rarity.RARE) {
                rare.add(item);
            }
        }
        return new Pools(vanilla, mod, other, rare);
    }

    /** 货币交易：随机货物 + 按稀有度定货币数量。 */
    private static MerchantOffer makeCurrencyTrade(Pools pools, RandomSource random, Set<Item> used) {
        Item sell = pickSellItem(pools, random, used);
        if (sell == null) {
            return null;
        }
        used.add(sell);
        Rarity rarity = new ItemStack(sell).getRarity();
        Item currency = CURRENCIES[random.nextInt(CURRENCIES.length)];
        int cost = priceFor(rarity, random);
        int maxUses = 4 + random.nextInt(5); // 4~8
        return new MerchantOffer(new ItemCost(currency, cost), Optional.empty(),
                new ItemStack(sell, 1), maxUses, xpFor(rarity), 0.0F);
    }

    /** 以物易物：多样化成本物品（含考古陶片）换一件 RARE 优先物品。 */
    private static MerchantOffer makeBarter(Pools pools, RandomSource random, RegistryAccess registryAccess, Set<Item> used) {
        List<Item> sherds = tagItems(registryAccess, ItemTags.DECORATED_POT_SHERDS);
        Item costItem;
        int costCount;
        if (!sherds.isEmpty() && random.nextInt(4) == 0) {
            // 25% 概率用考古陶片作成本
            costItem = sherds.get(random.nextInt(sherds.size()));
            costCount = 1 + random.nextInt(3); // 陶片 1~3
        } else {
            costItem = BARTER_COST_ITEMS[random.nextInt(BARTER_COST_ITEMS.length)];
            costCount = costCountFor(costItem, random);
        }

        Item reward = pickBarterReward(pools, random, used);
        if (reward == null) {
            return makeCurrencyTrade(pools, random, used); // 无可用奖励时退回普通货币交易
        }
        used.add(reward);
        int maxUses = 2 + random.nextInt(3); // 2~4
        return new MerchantOffer(new ItemCost(costItem, costCount), Optional.empty(),
                new ItemStack(reward, 1), maxUses, 8, 0.0F);
    }

    /** 以物易物成本数量：按成本物品自身稀有度（越稀有数量越少）。 */
    private static int costCountFor(Item costItem, RandomSource random) {
        Rarity rarity = new ItemStack(costItem).getRarity();
        return switch (rarity) {
            case UNCOMMON -> 2 + random.nextInt(5);  // 2~6
            case RARE -> 1 + random.nextInt(3);      // 1~3
            default -> 3 + random.nextInt(7);        // 3~9
        };
    }

    /** 以物易物奖励：优先 RARE 物品（避开已用），无则退回任意货物。 */
    private static Item pickBarterReward(Pools pools, RandomSource random, Set<Item> used) {
        Item r = pickUnused(pools.rare, random, used);
        if (r != null) {
            return r;
        }
        return pickSellItem(pools, random, used);
    }

    /** 按命名空间权重抽取一件未使用过的货物，命中组为空或重复时顺延/重试。 */
    private static Item pickSellItem(Pools pools, RandomSource random, Set<Item> used) {
        for (int attempt = 0; attempt < 10; attempt++) {
            Item item = pickSellItemRaw(pools, random);
            if (item != null && !used.contains(item)) {
                return item;
            }
        }
        // 重试仍重复则放宽：允许重复
        return pickSellItemRaw(pools, random);
    }

    /** 从列表中取一个未使用过的元素，全部用过则返回 null。 */
    private static Item pickUnused(List<Item> list, RandomSource random, Set<Item> used) {
        if (list.isEmpty()) {
            return null;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            Item item = list.get(random.nextInt(list.size()));
            if (!used.contains(item)) {
                return item;
            }
        }
        return null;
    }

    /** 按命名空间权重（原版 + 本模组为主，其他模组约 15%）抽取一件货物，命中组为空时顺延。 */
    private static Item pickSellItemRaw(Pools pools, RandomSource random) {
        int r = random.nextInt(WEIGHT_VANILLA + WEIGHT_MOD + WEIGHT_OTHER);
        List<Item> first;
        List<Item> second;
        List<Item> third;
        if (r < WEIGHT_VANILLA) {
            first = pools.vanilla;
            second = pools.mod;
            third = pools.other;
        } else if (r < WEIGHT_VANILLA + WEIGHT_MOD) {
            first = pools.mod;
            second = pools.vanilla;
            third = pools.other;
        } else {
            first = pools.other;
            second = pools.vanilla;
            third = pools.mod;
        }
        if (!first.isEmpty()) {
            return first.get(random.nextInt(first.size()));
        }
        if (!second.isEmpty()) {
            return second.get(random.nextInt(second.size()));
        }
        if (!third.isEmpty()) {
            return third.get(random.nextInt(third.size()));
        }
        return null;
    }

    /** 稀有度 → 货币数量区间（COMMON 4~24 / UNCOMMON 16~64 / RARE 48~128），钳制 [4,128]。 */
    private static int priceFor(Rarity rarity, RandomSource random) {
        int min;
        int max;
        switch (rarity) {
            case UNCOMMON -> {
                min = 16;
                max = 64;
            }
            case RARE -> {
                min = 48;
                max = 128;
            }
            default -> {
                min = 4;
                max = 24;
            }
        }
        return Mth.clamp(min + random.nextInt(max - min + 1), MIN_COST, MAX_COST);
    }

    /** 稀有度 → 交易奖励经验。 */
    private static int xpFor(Rarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> 4;
            case RARE -> 8;
            default -> 2;
        };
    }

    /** 从物品标签解析具体物品列表。 */
    private static List<Item> tagItems(RegistryAccess registryAccess, TagKey<Item> tag) {
        return registryAccess.lookupOrThrow(Registries.ITEM).getOrThrow(tag).stream()
                .map(Holder::value)
                .toList();
    }
}
