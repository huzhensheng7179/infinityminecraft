package com.himi.examplemod;

import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.himi.examplemod.archaeology.StoneBallRewardTable;
import com.himi.examplemod.effect.CantCatchMeEffect;
import com.himi.examplemod.effect.ChocoStormEffect;
import com.himi.examplemod.effect.XuebiStormEffect;
import com.himi.examplemod.item.EternalFlameTier;
import com.himi.examplemod.item.LostAncientBookItem;
import com.himi.examplemod.item.MysteriousCoinItem;
import com.himi.examplemod.item.MysteriousStoneBallItem;
import com.himi.examplemod.item.WorldSlashItem;
import com.himi.examplemod.item.XuebiItem;
import com.himi.examplemod.loot.SetSuperEnchantmentsFunction;
import com.himi.examplemod.network.ModNetwork;
import com.himi.examplemod.recipe.UnbreakableSmithingRecipe;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(infinityminecraft.MODID)
public class infinityminecraft {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "infinityminecraft";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "infinityminecraft" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "infinityminecraft" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "infinityminecraft" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold RecipeSerializers
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    // Create a Deferred Register to hold MobEffects which will all be registered under the "infinityminecraft" namespace
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    // Create a Deferred Register to hold PoiTypes (villager job sites) under the "infinityminecraft" namespace
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MODID);
    // Create a Deferred Register to hold VillagerProfessions under the "infinityminecraft" namespace
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, MODID);
    // Create a Deferred Register to hold LootItemFunctionTypes under the "infinityminecraft" namespace
    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, MODID);

    // 考古学者职业的工作站点方块 - 原版陶罐（decorated_pot），认领其全部方块状态作为 POI
    public static final DeferredHolder<PoiType, PoiType> ARCHAEOLOGIST_POI = POI_TYPES.register("archaeologist",
            () -> new PoiType(ImmutableSet.copyOf(Blocks.DECORATED_POT.getStateDefinition().getPossibleStates()), 1, 1));

    // 考古学者 - 村民新职业：以陶罐为工作站点，收购考古获得的陶片，出售珍宝与本模组稀有物品（交易见 event/ArchaeologistTradesHandler）
    public static final DeferredHolder<VillagerProfession, VillagerProfession> ARCHAEOLOGIST = VILLAGER_PROFESSIONS.register("archaeologist",
            () -> new VillagerProfession("archaeologist",
                    holder -> holder.is(ARCHAEOLOGIST_POI.getKey()),
                    holder -> holder.is(ARCHAEOLOGIST_POI.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    // 避箭之戒 - 装备在饰品栏戒指栏位，免疫弹射物伤害
    public static final DeferredItem<Item> ARROW_DEFLECTION_RING = ITEMS.registerSimpleItem("arrow_deflection_ring",
            new Item.Properties().durability(512).rarity(Rarity.UNCOMMON));

    // 无暇辰星 - 在锻造台中配合“无暇辰星升级模板”为任意带耐久物品（含下界合金）附上无法破坏
    public static final DeferredItem<Item> FLAWLESS_STAR = ITEMS.registerSimpleItem("flawless_star",
            new Item.Properties().rarity(Rarity.EPIC));

    // 无暇辰星升级模板 - 专用锻造模板：与无暇辰星一起放入锻造台，可为任意武器/装备/工具附上无法破坏
    public static final DeferredItem<Item> FLAWLESS_STAR_TEMPLATE = ITEMS.register("flawless_star_upgrade_smithing_template",
            () -> new SmithingTemplateItem(
                    Component.translatable("item.infinityminecraft.flawless_star_upgrade_smithing_template.applies_to").withStyle(ChatFormatting.BLUE),
                    Component.translatable("item.infinityminecraft.flawless_star_upgrade_smithing_template.ingredients").withStyle(ChatFormatting.BLUE),
                    Component.translatable("upgrade.infinityminecraft.flawless_star").withStyle(ChatFormatting.GRAY),
                    Component.translatable("item.infinityminecraft.flawless_star_upgrade_smithing_template.base_slot_description"),
                    Component.translatable("item.infinityminecraft.flawless_star_upgrade_smithing_template.additions_slot_description"),
                    List.of(
                            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
                            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
                            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
                            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots"),
                            ResourceLocation.withDefaultNamespace("item/empty_slot_sword"),
                            ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe"),
                            ResourceLocation.withDefaultNamespace("item/empty_slot_axe"),
                            ResourceLocation.withDefaultNamespace("item/empty_slot_shovel"),
                            ResourceLocation.withDefaultNamespace("item/empty_slot_hoe")),
                    List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_diamond"))));

    // 法棍护符 - 装备在饰品栏护符栏位，提供永久饥饿 I，受伤时叠加抗性提升
    public static final DeferredItem<Item> BAGUETTE_TALISMAN = ITEMS.registerSimpleItem("baguette_talisman",
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));

    // baka向日葵吧唧 - 可装备在任意饰品栏，光照>7时获得瞬间治疗 II
    public static final DeferredItem<Item> BAKA_SUNFLOWER_BADGE = ITEMS.registerSimpleItem("baka_sunflower_badge",
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));

    // 钢甲系列 - 胸饰槽位，逐级增强
    public static final DeferredItem<Item> IRON_STEEL_ARMOR = ITEMS.registerSimpleItem("iron_steel_armor",
            new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1).attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(MODID, "iron_steel_armor.armor"),
                            5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
                    .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(MODID, "iron_steel_armor.toughness"),
                            6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
                    .build()));
    public static final DeferredItem<Item> GOLD_STEEL_ARMOR = ITEMS.registerSimpleItem("gold_steel_armor",
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));
    public static final DeferredItem<Item> DIAMOND_STEEL_ARMOR = ITEMS.registerSimpleItem("diamond_steel_armor",
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));
    public static final DeferredItem<Item> NETHERITE_STEEL_ARMOR = ITEMS.registerSimpleItem("netherite_steel_armor",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
    public static final DeferredItem<Item> NETHER_STAR_STEEL_ARMOR = ITEMS.registerSimpleItem("nether_star_steel_armor",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 奶龙面具 - 头饰槽位，注视敌对生物时定身
    public static final DeferredItem<Item> NAILONG_MASK = ITEMS.registerSimpleItem("nailong_mask",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 冰冰冰 - 护符槽位，受伤>6点时移除周围生物AI（6秒后复原，25秒冷却）
    public static final DeferredItem<Item> BING_BING_BING = ITEMS.registerSimpleItem("bing_bing_bing",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 石鬼面 - 头饰槽位，伤害+20%、攻击吸血50%、进食加速，但阳光下持续扣血
    public static final DeferredItem<Item> STONE_MASK = ITEMS.registerSimpleItem("stone_mask",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 太阳之环 - 头饰槽位，佩戴时持续击退周围 2 格内的敌对生物（力度约 2.5）
    public static final DeferredItem<Item> SUN_RING = ITEMS.registerSimpleItem("sun_ring",
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));

    // 坚果墙 - 腰带槽位（或副手），强制吸引半径20格敌方单位（不转移仇恨），受伤时获得抗性提升4与缓慢4持续20秒
    public static final DeferredItem<Item> NUT_WALL = ITEMS.registerSimpleItem("nut_wall",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 贝质素 - 腰带槽位，装备后免疫摔落伤害、跳跃高度提升到约2格、可直接跨越1格高方块；装备时按 C 键打开末影箱
    public static final DeferredItem<Item> BEI_ZHI_SU = ITEMS.registerSimpleItem("bei_zhi_su",
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));

    // 流星一条 - 项链槽位，弓蓄力可持续积攒伤害（满蓄力后每秒+100%原伤害，上限5000%）
    public static final DeferredItem<Item> METEOR_STREAK = ITEMS.registerSimpleItem("meteor_streak",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 神秘硬币 - 手持右键发射蓝色粒子光线，命中实体/方块产生不破坏方块的爆炸，造成10~100000随机伤害，冷却30分钟
    public static final DeferredItem<Item> MYSTERIOUS_COIN = ITEMS.register("mysterious_coin",
            () -> new MysteriousCoinItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // 神秘石球 - 考古获得（注入 minecraft:archaeology/* 战利品表）；手持右键敲开，消耗任意品质镐子 3 点耐久并消耗 1 个石球，
    // 随机产出考古产物/矿物/经验（常见）或锻造模板/附魔金苹果/本模组物品/其它模组最高稀有度物品（极小概率，见 StoneBallRewardTable）
    public static final DeferredItem<Item> MYSTERIOUS_STONE_BALL = ITEMS.register("mysterious_stone_ball",
            () -> new MysteriousStoneBallItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    // 世界斩 - 手持右键锁定鼠标指向处（≤16格），5秒降下15道斩击（多色粒子+音效，每斩100伤害并定身，半径3格），
    // 结束后清除残存活物并使施法者因咒缚付出生命（逻辑见 event/WorldSlashHandler）
    public static final DeferredItem<Item> WORLD_SLASH = ITEMS.register("world_slash",
            () -> new WorldSlashItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // “杰”厕灵 - 背饰槽位，免疫接下来3次任何伤害，之后损失最大生命的70%，内置冷却60秒
    public static final DeferredItem<Item> JIE_TOILET_CLEANER = ITEMS.registerSimpleItem("jie_toilet_cleaner",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 粗制辣味糖果 - 护符槽位，佩戴时免疫冰冻伤害与体温过低
    public static final DeferredItem<Item> CRUDE_SPICY_CANDY = ITEMS.registerSimpleItem("crude_spicy_candy",
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));

    // 火与钢 - 胸饰槽位，免疫火焰伤害，攻击附加火焰，攻击已燃烧目标无视护甲且伤害×5，每次攻击附加目标5%最大生命固定伤害
    public static final DeferredItem<Item> FIRE_AND_STEEL = ITEMS.registerSimpleItem("fire_and_steel",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 淬火之刃 - 放在背包时使用铁砧不消耗经验且不会过于昂贵；用铁砧 30 次后就地变形为“在烈焰中永恒”
    public static final DeferredItem<Item> TEMPERED_BLADE = ITEMS.registerSimpleItem("tempered_blade",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // 在烈焰中永恒 - 剑：50 点伤害、攻速 1.7、耐久 2700，近战无视一切减伤（虚空伤害）
    public static final DeferredItem<SwordItem> ETERNAL_IN_FLAMES = ITEMS.register("eternal_in_flames",
            () -> new SwordItem(EternalFlameTier.INSTANCE, new Item.Properties()
                    .rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(EternalFlameTier.INSTANCE, 49.0F, -2.3F))));

    // 失落古籍 - 可放入附魔台进行“超限附魔”（附魔等级必定超过原版上限，最多高出 3 级，如锋利 VIII）；
    // 附魔后像附魔书一样存入 STORED_ENCHANTMENTS，可在铁砧上把超限附魔转移给其它物品（mixin 见 EnchantmentHelperMixin / AnvilMenuLostBookMixin）；
    // 会以已附魔形态出现在所有原版奖励箱中（战利品注入见 event/LostAncientBookLootHandler）
    public static final DeferredItem<Item> LOST_ANCIENT_BOOK = ITEMS.register("lost_ancient_book",
            () -> new LostAncientBookItem(new Item.Properties()
                    .rarity(Rarity.EPIC)
                    .stacksTo(1)
                    .component(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)));

    // 巧乐兹风暴 - 效果：在任意方块上如冰面般滑行；受击时对伤害来源反射冰冻伤害（1级4/2级8/3级16）
    public static final DeferredHolder<MobEffect, MobEffect> CHOCO_STORM =
            MOB_EFFECTS.register("qiaolezi_storm", ChocoStormEffect::new);

    // 巧乐兹 - 食物：6 饱食度 / 8 饱和度，食用后获得 30 秒巧乐兹风暴 III
    public static final DeferredItem<Item> QIAOLEZI = ITEMS.register("qiaolezi",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).food(new FoodProperties.Builder()
                    .nutrition(6).saturationModifier(8.0F)
                    .effect(() -> new MobEffectInstance(CHOCO_STORM, 600, 2), 1.0F)
                    .build())));

    // 雪碧风暴 - 效果：近战命中使敌人减速（缓慢 II / 5 秒）并额外承受冰冻伤害（1级2/2级4/3级8）
    public static final DeferredHolder<MobEffect, MobEffect> XUEBI_STORM =
            MOB_EFFECTS.register("xuebi_storm", XuebiStormEffect::new);

    // 雪碧 - 饮品：2 饱食度 / 2 饱和度，饮用后获得 60 秒雪碧风暴 III
    public static final DeferredItem<Item> XUEBI = ITEMS.register("xuebi",
            () -> new XuebiItem(new Item.Properties().rarity(Rarity.UNCOMMON).food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(2.0F)
                    .effect(() -> new MobEffectInstance(XUEBI_STORM, 1200, 2), 1.0F)
                    .build())));

    // 你跑不过我你信不信 - 效果：巧乐兹风暴 + 雪碧风暴同时存在时合成；提速、抬高台阶、周身冰冻光环、每秒自损，冲刺时全部翻倍
    public static final DeferredHolder<MobEffect, MobEffect> CANT_CATCH_ME =
            MOB_EFFECTS.register("cant_catch_me", CantCatchMeEffect::new);

    // 自定义冰冻伤害类型：数据驱动，经 data/minecraft/tags/damage_type/* 配置为无视护甲/抗性/无敌帧
    public static final ResourceKey<DamageType> CANT_CATCH_ME_FREEZE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "cant_catch_me_freeze"));

    // 世界斩伤害类型：数据驱动（data/infinityminecraft/damage_type/world_slash.json），经 bypasses_armor /
    // bypasses_cooldown / bypasses_resistance 标签配置为无视护甲/无敌帧/抗性；message_id=world_slash 决定死亡播报
    public static final ResourceKey<DamageType> WORLD_SLASH_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "world_slash"));

    // 铡刃 - 剑/斧专属附魔（数据驱动 data/infinityminecraft/enchantment/guillotine.json，最高 4 级）：
    // 击杀可掉落头颅的生物时按等级将头颅掉率变为 12%/24%/36%/48%（掉率逻辑见 event/GuillotineHandler）
    public static final ResourceKey<Enchantment> GUILLOTINE =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, "guillotine"));

    // 考古勘探 - 镐子专属附魔（数据驱动 data/infinityminecraft/enchantment/archaeological_prospecting.json，最高 3 级）：
    // 用带此附魔的镐子破坏「镐子适应的方块」时，按等级 2%/4%/6% 概率额外掉落神秘石球（逻辑见 event/ProspectingHandler）
    public static final ResourceKey<Enchantment> ARCHAEOLOGICAL_PROSPECTING =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, "archaeological_prospecting"));

    // 注册自定义锻造配方序列化器
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> UNBREAKABLE_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("unbreakable_smithing", UnbreakableSmithingRecipe.Serializer::new);

    // 注册自定义战利品函数类型：为奖励箱中的失落古籍附上超限附魔
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<SetSuperEnchantmentsFunction>> SET_SUPER_ENCHANTMENTS =
            LOOT_FUNCTIONS.register("set_super_enchantments",
                    () -> new LootItemFunctionType<>(SetSuperEnchantmentsFunction.CODEC));

    // Creates a creative tab with the id "infinityminecraft:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.infinityminecraft")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> BAKA_SUNFLOWER_BADGE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ARROW_DEFLECTION_RING.get());
                output.accept(FLAWLESS_STAR.get());
                output.accept(FLAWLESS_STAR_TEMPLATE.get());
                output.accept(BAGUETTE_TALISMAN.get());
                output.accept(BAKA_SUNFLOWER_BADGE.get());
                output.accept(IRON_STEEL_ARMOR.get());
                output.accept(GOLD_STEEL_ARMOR.get());
                output.accept(DIAMOND_STEEL_ARMOR.get());
                output.accept(NETHERITE_STEEL_ARMOR.get());
                output.accept(NETHER_STAR_STEEL_ARMOR.get());
                output.accept(NAILONG_MASK.get());
                output.accept(BING_BING_BING.get());
                output.accept(STONE_MASK.get());
                output.accept(SUN_RING.get());
                output.accept(NUT_WALL.get());
                output.accept(BEI_ZHI_SU.get());
                output.accept(METEOR_STREAK.get());
                output.accept(MYSTERIOUS_COIN.get());
                output.accept(MYSTERIOUS_STONE_BALL.get());
                output.accept(WORLD_SLASH.get());
                output.accept(JIE_TOILET_CLEANER.get());
                output.accept(CRUDE_SPICY_CANDY.get());
                output.accept(FIRE_AND_STEEL.get());
                output.accept(TEMPERED_BLADE.get());
                output.accept(ETERNAL_IN_FLAMES.get());
                output.accept(QIAOLEZI.get());
                output.accept(XUEBI.get());
                output.accept(LOST_ANCIENT_BOOK.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public infinityminecraft(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so recipe serializers get registered
        RECIPE_SERIALIZERS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so mob effects get registered
        MOB_EFFECTS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so poi types (villager job sites) get registered
        POI_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so villager professions get registered
        VILLAGER_PROFESSIONS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so loot function types get registered
        LOOT_FUNCTIONS.register(modEventBus);

        // Register custom network payloads (e.g. open ender chest keybind)
        modEventBus.addListener(ModNetwork::register);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (infinityminecraft) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 配置重载时刷新神秘石球「其他模组」奖励池缓存，使黑名单改动无需重启即可生效
        modEventBus.addListener((ModConfigEvent.Reloading event) -> StoneBallRewardTable.invalidateCache());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
