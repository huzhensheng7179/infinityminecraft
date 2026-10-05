package com.himi.examplemod.archaeology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.himi.examplemod.Config;
import com.himi.examplemod.infinityminecraft;
import com.himi.examplemod.item.LostAncientBookItem;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 神秘石球奖励池：右键开球时按权重随机产出一类奖励。
 *
 * <p>奖励类别与权重（合计 100）：常见为考古产物（陶片）、矿物、经验；“极小概率”为锻造模板、
 * 附魔金苹果、本模组物品，以及动态扫描注册表得到的「其他模组」物品（普通/稀有/史诗 3 档，
 * 其中史诗档概率极低）。</p>
 *
 * <p>其他模组池仅收录命名空间既非 minecraft 也非本模组的物品（原版由矿物/考古/模板等类别覆盖），
 * 并严格过滤生存模式无法获得的技术性/管理员物品（基岩、命令方块、刷怪蛋、屏障、结构方块、
 * 调试棒等，见 {@link #BLACKLIST} 与 spawn_egg 规则），确保不会产出基岩/命令方块之类物品。</p>
 */
public final class StoneBallRewardTable {

    private StoneBallRewardTable() {
    }

    /** 一次开球的产出：待掉落的物品 + 待生成的经验值。 */
    public record Reward(List<ItemStack> items, int experience) {
        public boolean isEmpty() {
            return items.isEmpty() && experience <= 0;
        }
    }

    /**
     * 考古产物（模组部分）标签：收录所有「无合成表」的模组物品，刷子交互与神秘石球共用同一份清单。
     * 数据文件见 {@code data/infinityminecraft/tags/item/archaeology_products.json}，可由数据包扩展。
     */
    public static final TagKey<Item> ARCHAEOLOGY_PRODUCTS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(infinityminecraft.MODID, "archaeology_products"));

    /**
     * 各考古产物按「强度分档」的出现权重（数值越大越常见）；未列出的物品用 {@link #DEFAULT_PRODUCT_WEIGHT}。
     * 神秘石球（MOD_ITEM 类别）与刷子注入共用此权重，确保强度偏高的物品出现概率更低。
     */
    private static final Map<ResourceLocation, Integer> PRODUCT_WEIGHTS = Map.ofEntries(
            // 极强（易破坏平衡）：世界斩 / 在烈焰中永恒 / 流星一条 / 神秘硬币 / 火与钢
            Map.entry(modRL("world_slash"), 1),
            Map.entry(modRL("eternal_in_flames"), 1),
            Map.entry(modRL("meteor_streak"), 1),
            Map.entry(modRL("mysterious_coin"), 1),
            Map.entry(modRL("fire_and_steel"), 1),
            // 强：奶龙面具 / 冰冰冰 / 石鬼面 / 坚果墙 / “杰”厕灵
            //（失落古籍不在此表：改由 LOST_BOOK_CHANCE 专属概率产出已附魔形态）
            Map.entry(modRL("nailong_mask"), 4),
            Map.entry(modRL("bing_bing_bing"), 4),
            Map.entry(modRL("stone_mask"), 4),
            Map.entry(modRL("nut_wall"), 4),
            Map.entry(modRL("jie_toilet_cleaner"), 4),
            // 中：太阳之环 / 贝质素 / 淬火之刃
            Map.entry(modRL("sun_ring"), 10),
            Map.entry(modRL("bei_zhi_su"), 10),
            Map.entry(modRL("tempered_blade"), 10),
            // 弱·功能：粗制辣味糖果 / 巧乐兹 / 雪碧
            Map.entry(modRL("crude_spicy_candy"), 20),
            Map.entry(modRL("qiaolezi"), 20),
            Map.entry(modRL("xuebi"), 20));

    /** 未登记在 {@link #PRODUCT_WEIGHTS} 中的考古产物默认权重（介于“中”与“强”之间）。 */
    private static final int DEFAULT_PRODUCT_WEIGHT = 8;

    /** 返回某物品作为考古产物时的出现权重（越大越常见），供石球与刷子注入共用。 */
    public static int weightOf(Item item) {
        return PRODUCT_WEIGHTS.getOrDefault(BuiltInRegistries.ITEM.getKey(item), DEFAULT_PRODUCT_WEIGHT);
    }

    /**
     * 神秘石球与刷子交互「额外」产出失落古籍（已附魔形态）的专属概率（约 6%，落在用户要求的 4%~8% 区间，可调）。
     * 与奖励箱注入形态一致；不再通过 archaeology_products 标签的均等加权产出，避免产出未附魔形态。
     */
    public static final float LOST_BOOK_CHANCE = 0.06F;

    /** 奖励类别及其权重（合计 100；模板/附魔金苹果/本模组/其他模组等稀有类别合计约 19%，对应“极小概率”）。 */
    private enum Category {
        ARCHAEOLOGY(34),
        MINERALS(30),
        EXPERIENCE(17),
        TEMPLATE(5),
        ENCHANTED_GOLDEN_APPLE(3),
        MOD_ITEM(5),
        OTHER_MOD(6);

        final int weight;

        Category(int weight) {
            this.weight = weight;
        }
    }

    /** 生存模式无法获得/技术性物品黑名单（用于动态扫描过滤；命名空间 minecraft）。 */
    private static final Set<ResourceLocation> BLACKLIST = Set.of(
            rl("bedrock"), rl("barrier"), rl("light"),
            rl("command_block"), rl("chain_command_block"), rl("repeating_command_block"), rl("command_block_minecart"),
            rl("structure_block"), rl("structure_void"), rl("jigsaw"),
            rl("debug_stick"), rl("knowledge_book"), rl("end_portal_frame"), rl("end_portal"), rl("nether_portal"),
            rl("spawner"), rl("trial_spawner"), rl("vault"), rl("reinforced_deepslate"),
            rl("budding_amethyst"), rl("petrified_oak_slab"), rl("suspicious_sand"), rl("suspicious_gravel"),
            rl("farmland"), rl("dirt_path"), rl("fire"), rl("soul_fire"),
            rl("water"), rl("lava"), rl("air"), rl("cave_air"), rl("void_air"), rl("bubble_column"),
            rl("moving_piston"), rl("piston_head"), rl("attached_melon_stem"), rl("attached_pumpkin_stem"),
            rl("melon_stem"), rl("pumpkin_stem"));

    /** 带权重的矿物奖励条目。 */
    private record WeightedItem(Item item, int min, int max, int weight) {
    }

    private static final List<WeightedItem> MINERAL_POOL = List.of(
            new WeightedItem(Items.COAL, 1, 4, 12),
            new WeightedItem(Items.RAW_IRON, 1, 4, 10),
            new WeightedItem(Items.IRON_INGOT, 1, 3, 8),
            new WeightedItem(Items.RAW_COPPER, 1, 4, 10),
            new WeightedItem(Items.COPPER_INGOT, 1, 3, 8),
            new WeightedItem(Items.RAW_GOLD, 1, 3, 6),
            new WeightedItem(Items.GOLD_INGOT, 1, 2, 6),
            new WeightedItem(Items.LAPIS_LAZULI, 2, 6, 8),
            new WeightedItem(Items.REDSTONE, 2, 6, 8),
            new WeightedItem(Items.QUARTZ, 1, 4, 6),
            new WeightedItem(Items.AMETHYST_SHARD, 1, 4, 5),
            new WeightedItem(Items.DIAMOND, 1, 2, 4),
            new WeightedItem(Items.EMERALD, 1, 2, 4),
            new WeightedItem(Items.NETHERITE_SCRAP, 1, 1, 2),
            new WeightedItem(Items.ANCIENT_DEBRIS, 1, 1, 1));

    /**
     * 其他模组物品池，按稀有度分为普通/稀有/史诗 3 档：注册表加载后不变，惰性计算并缓存。
     * 仅收录命名空间既非 minecraft 也非本模组、且通过 {@link #isObtainable} 过滤的物品。
     */
    private record OtherModPools(List<Item> common, List<Item> rare, List<Item> epic) {
    }

    private static volatile OtherModPools otherModPools;

    /** 配置文件黑名单解析缓存（配置重载时随 otherModPools 一起失效）。 */
    private static volatile Set<ResourceLocation> cachedConfigBlacklist;

    /** OTHER_MOD 类别内部按稀有度分档的权重：普通最常见、稀有次之、史诗极低。 */
    private static final int RARITY_WEIGHT_COMMON = 74;
    private static final int RARITY_WEIGHT_RARE = 22;
    private static final int RARITY_WEIGHT_EPIC = 4;

    /** 按权重抽取一类奖励，返回待掉落物品与经验。 */
    public static Reward roll(ServerLevel level, RandomSource random) {
        Category category = pickCategory(random);
        HolderLookup.RegistryLookup<Item> lookup = level.registryAccess().lookupOrThrow(Registries.ITEM);
        List<ItemStack> items = new ArrayList<>();
        int experience = 0;

        switch (category) {
            case ARCHAEOLOGY -> {
                List<Item> sherds = tagItems(lookup, ItemTags.DECORATED_POT_SHERDS);
                if (!sherds.isEmpty()) {
                    items.add(new ItemStack(pick(sherds, random), 1 + random.nextInt(3)));
                } else {
                    items.add(new ItemStack(Items.BRICK, 1 + random.nextInt(3)));
                }
            }
            case MINERALS -> {
                WeightedItem w = pickWeighted(MINERAL_POOL, random);
                int count = w.max() > w.min() ? w.min() + random.nextInt(w.max() - w.min() + 1) : w.min();
                items.add(new ItemStack(w.item(), count));
            }
            case EXPERIENCE -> experience = 8 + random.nextInt(38); // 8~45 点经验
            case TEMPLATE -> {
                List<Item> templates = new ArrayList<>(tagItems(lookup, ItemTags.TRIM_TEMPLATES));
                templates.add(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
                templates.add(infinityminecraft.FLAWLESS_STAR_TEMPLATE.get());
                items.add(new ItemStack(pick(templates, random), 1));
            }
            case ENCHANTED_GOLDEN_APPLE ->
                    items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, random.nextInt(4) == 0 ? 2 : 1));
            case MOD_ITEM -> {
                // 考古产物（模组部分）：所有无合成表的模组物品（剔除全局黑名单）
                List<Item> products = tagItems(lookup, ARCHAEOLOGY_PRODUCTS).stream()
                        .filter(item -> !isBlacklisted(item)).toList();
                if (products.isEmpty()) {
                    products = modItems().stream().filter(item -> !isBlacklisted(item)).toList(); // 兜底：标签缺失时退回全部本模组物品
                }
                if (!products.isEmpty()) {
                    items.add(new ItemStack(pickWeightedItem(products, random), 1));
                } else {
                    items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
                }
            }
            case OTHER_MOD -> {
                Item other = pickOtherModByRarity(otherModPools(), random);
                if (other != null) {
                    items.add(new ItemStack(other, 1));
                } else {
                    // 无任何其他模组物品时回退为原版顶级稀有物
                    items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
                }
            }
        }

        // 失落古籍：约 6% 概率额外产出一本已附魔（超限）形态的古籍（若未被全局黑名单剔除）
        if (random.nextFloat() < LOST_BOOK_CHANCE
                && !isBlacklisted(infinityminecraft.LOST_ANCIENT_BOOK.get())) {
            items.add(LostAncientBookItem.applySuperEnchantments(
                    new ItemStack(infinityminecraft.LOST_ANCIENT_BOOK.get()),
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT),
                    random));
        }

        // 全局黑名单兜底：移除任何被配置文件屏蔽的产出（覆盖矿物/模板/附魔金苹果/陶片/其他模组等所有类别）
        items.removeIf(stack -> isBlacklisted(stack.getItem()));

        return new Reward(items, experience);
    }

    private static Category pickCategory(RandomSource random) {
        int total = 0;
        for (Category c : Category.values()) {
            total += c.weight;
        }
        int r = random.nextInt(total);
        for (Category c : Category.values()) {
            if (r < c.weight) {
                return c;
            }
            r -= c.weight;
        }
        return Category.MINERALS;
    }

    /** 从物品标签解析出具体物品列表。 */
    private static List<Item> tagItems(HolderLookup.RegistryLookup<Item> lookup,
            net.minecraft.tags.TagKey<Item> tag) {
        return lookup.getOrThrow(tag).stream().map(Holder::value).toList();
    }

    private static Item pick(List<Item> list, RandomSource random) {
        return list.get(random.nextInt(list.size()));
    }

    private static WeightedItem pickWeighted(List<WeightedItem> pool, RandomSource random) {
        int total = 0;
        for (WeightedItem w : pool) {
            total += w.weight();
        }
        int r = random.nextInt(total);
        for (WeightedItem w : pool) {
            if (r < w.weight()) {
                return w;
            }
            r -= w.weight();
        }
        return pool.get(0);
    }

    /** 按 {@link #weightOf} 加权从物品列表随机取一个（强度越高越不易被选中）。 */
    private static Item pickWeightedItem(List<Item> list, RandomSource random) {
        int total = 0;
        for (Item i : list) {
            total += weightOf(i);
        }
        int r = random.nextInt(total);
        for (Item i : list) {
            int w = weightOf(i);
            if (r < w) {
                return i;
            }
            r -= w;
        }
        return list.get(0);
    }

    /** 本模组所有可获得物品（排除石球自身），惰性计算。 */
    private static List<Item> modItems() {
        List<Item> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!infinityminecraft.MODID.equals(id.getNamespace())) {
                continue;
            }
            if (item == infinityminecraft.MYSTERIOUS_STONE_BALL.get()) {
                continue;
            }
            list.add(item);
        }
        return list;
    }

    /**
     * 其他模组物品池：仅非 minecraft/非本模组命名空间，剔除管理员与生存不可获得物品，
     * 按稀有度（普通/稀有/史诗）分档缓存。UNCOMMON 不纳入（用户只要 3 档）。
     */
    private static OtherModPools otherModPools() {
        OtherModPools pools = otherModPools;
        if (pools != null) {
            return pools;
        }
        // 配置文件黑名单（可剔除任意其他模组物品，见 Config.STONE_BALL_BLACKLIST）
        Set<ResourceLocation> configBlacklist = configBlacklist();
        List<Item> common = new ArrayList<>();
        List<Item> rare = new ArrayList<>();
        List<Item> epic = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String namespace = id.getNamespace();
            // 仅“其他模组”：原版由矿物/考古/模板等类别覆盖，本模组由 MOD_ITEM 覆盖
            if ("minecraft".equals(namespace) || infinityminecraft.MODID.equals(namespace)) {
                continue;
            }
            // 剔除管理员物品、生存不可获得物品（内置黑名单 + 刷怪蛋）以及配置文件黑名单中的物品
            if (!isObtainable(id) || configBlacklist.contains(id)) {
                continue;
            }
            ItemStack def = new ItemStack(item);
            if (def.isEmpty()) {
                continue;
            }
            switch (def.getRarity()) {
                case COMMON -> common.add(item);
                case RARE -> rare.add(item);
                case EPIC -> epic.add(item);
                default -> {
                    // UNCOMMON 不纳入
                }
            }
        }
        pools = new OtherModPools(common, rare, epic);
        otherModPools = pools;
        return pools;
    }

    /**
     * 先按稀有度分档权重（普通/稀有/史诗）抽取档位，再从该档随机取一个物品；
     * 命中档位为空时顺延到其它非空档位，全空返回 {@code null}。史诗档权重极低。
     */
    private static Item pickOtherModByRarity(OtherModPools pools, RandomSource random) {
        int total = RARITY_WEIGHT_COMMON + RARITY_WEIGHT_RARE + RARITY_WEIGHT_EPIC;
        int r = random.nextInt(total);
        List<Item> first;
        List<Item> second;
        List<Item> third;
        if (r < RARITY_WEIGHT_COMMON) {
            first = pools.common();
            second = pools.rare();
            third = pools.epic();
        } else if (r < RARITY_WEIGHT_COMMON + RARITY_WEIGHT_RARE) {
            first = pools.rare();
            second = pools.common();
            third = pools.epic();
        } else {
            first = pools.epic();
            second = pools.rare();
            third = pools.common();
        }
        if (!first.isEmpty()) {
            return pick(first, random);
        }
        if (!second.isEmpty()) {
            return pick(second, random);
        }
        if (!third.isEmpty()) {
            return pick(third, random);
        }
        return null;
    }

    /** 生存可获得性判定：不在黑名单内，且不是刷怪蛋等纯技术性物品。 */
    private static boolean isObtainable(ResourceLocation id) {
        if (BLACKLIST.contains(id)) {
            return false;
        }
        String path = id.getPath();
        return !path.endsWith("_spawn_egg");
    }

    /** 从配置读取石球黑名单（物品注册 ID 集合），惰性解析并缓存；非法条目或配置未加载时忽略。 */
    private static Set<ResourceLocation> configBlacklist() {
        Set<ResourceLocation> cached = cachedConfigBlacklist;
        if (cached != null) {
            return cached;
        }
        Set<ResourceLocation> set = new HashSet<>();
        try {
            for (String entry : Config.STONE_BALL_BLACKLIST.get()) {
                try {
                    set.add(ResourceLocation.parse(entry));
                } catch (Exception ignored) {
                    // 非法 ID 忽略
                }
            }
        } catch (Exception ignored) {
            // 配置尚未加载时忽略
        }
        cachedConfigBlacklist = set;
        return set;
    }

    /**
     * 全局黑名单判定：该物品是否被配置文件（{@link Config#STONE_BALL_BLACKLIST}）列入石球黑名单。
     * 作用于神秘石球的所有产出类别与刷子的考古产物注入。
     */
    public static boolean isBlacklisted(Item item) {
        return configBlacklist().contains(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * 通用「可获得性」判定：不在内置生存不可获得黑名单、不是刷怪蛋、且未被配置黑名单屏蔽。
     * 供流浪嗅探兽商人的交易物品池复用，与神秘石球「其他模组扫描」采用同一套过滤标准。
     */
    public static boolean isObtainableItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return isObtainable(id) && !configBlacklist().contains(id);
    }

    /** 使其他模组物品池与黑名单缓存失效（配置重载时调用），下次开球重新扫描并应用最新黑名单。 */
    public static void invalidateCache() {
        otherModPools = null;
        cachedConfigBlacklist = null;
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation modRL(String path) {
        return ResourceLocation.fromNamespaceAndPath(infinityminecraft.MODID, path);
    }
}
