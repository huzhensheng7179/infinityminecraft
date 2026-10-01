package com.himi.examplemod;

import org.slf4j.Logger;

import com.himi.examplemod.item.MysteriousCoinItem;
import com.himi.examplemod.recipe.UnbreakableSmithingRecipe;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
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

    // Creates a new Block with the id "infinityminecraft:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "infinityminecraft:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "infinityminecraft:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // 避箭之戒 - 装备在饰品栏戒指栏位，免疫弹射物伤害
    public static final DeferredItem<Item> ARROW_DEFLECTION_RING = ITEMS.registerSimpleItem("arrow_deflection_ring",
            new Item.Properties().durability(512).rarity(Rarity.UNCOMMON));

    // 无暇辰星 - 在锻造台中为任意带耐久物品附上无法破坏
    public static final DeferredItem<Item> FLAWLESS_STAR = ITEMS.registerSimpleItem("flawless_star",
            new Item.Properties().rarity(Rarity.EPIC));

    // 法棍护符 - 装备在饰品栏护符栏位，提供永久饥饿 I，受伤时叠加抗性提升
    public static final DeferredItem<Item> BAGUETTE_TALISMAN = ITEMS.registerSimpleItem("baguette_talisman",
            new Item.Properties().rarity(Rarity.RARE));

    // baka向日葵吧嘆 - 可装备在任意饰品栏，光照>7时获得生命回复 II
    public static final DeferredItem<Item> BAKA_SUNFLOWER_BADGE = ITEMS.registerSimpleItem("baka_sunflower_badge",
            new Item.Properties().rarity(Rarity.RARE));

    // 钢甲系列 - 胸饰槽位，逐级增强
    public static final DeferredItem<Item> IRON_STEEL_ARMOR = ITEMS.registerSimpleItem("iron_steel_armor",
            new Item.Properties().rarity(Rarity.UNCOMMON).attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(MODID, "iron_steel_armor.armor"),
                            5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
                    .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(MODID, "iron_steel_armor.toughness"),
                            6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY)
                    .build()));
    public static final DeferredItem<Item> GOLD_STEEL_ARMOR = ITEMS.registerSimpleItem("gold_steel_armor",
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> DIAMOND_STEEL_ARMOR = ITEMS.registerSimpleItem("diamond_steel_armor",
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> NETHERITE_STEEL_ARMOR = ITEMS.registerSimpleItem("netherite_steel_armor",
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> NETHER_STAR_STEEL_ARMOR = ITEMS.registerSimpleItem("nether_star_steel_armor",
            new Item.Properties().rarity(Rarity.EPIC));

    // 奶龙面具 - 头饰槽位，注视敌对生物时定身
    public static final DeferredItem<Item> NAILONG_MASK = ITEMS.registerSimpleItem("nailong_mask",
            new Item.Properties().rarity(Rarity.EPIC));

    // 冰冰冰 - 护符槽位，受伤>6点时移除周围生物AI（6秒后复原，25秒冷却）
    public static final DeferredItem<Item> BING_BING_BING = ITEMS.registerSimpleItem("bing_bing_bing",
            new Item.Properties().rarity(Rarity.EPIC));

    // 石鬼面 - 头饰槽位，伤害+20%、攻击吸血50%、进食加速，但阳光下持续扣血
    public static final DeferredItem<Item> STONE_MASK = ITEMS.registerSimpleItem("stone_mask",
            new Item.Properties().rarity(Rarity.EPIC));

    // 坚果墙 - 腰带槽位（或副手），强制吸引半径20格敌方单位（不转移仇恨），受伤时获得抗性提升4与缓慢4持续20秒
    public static final DeferredItem<Item> NUT_WALL = ITEMS.registerSimpleItem("nut_wall",
            new Item.Properties().rarity(Rarity.EPIC));

    // 流星一条 - 项链槽位，弓蓄力可持续积攒伤害（满蓄力后每秒+2%原伤害，上限300%）
    public static final DeferredItem<Item> METEOR_STREAK = ITEMS.registerSimpleItem("meteor_streak",
            new Item.Properties().rarity(Rarity.EPIC));

    // 神秘硬币 - 手持右键发射蓝色粒子光线，命中实体/方块产生不破坏方块的爆炸，造成10~100000随机伤害，冷却30分钟
    public static final DeferredItem<Item> MYSTERIOUS_COIN = ITEMS.register("mysterious_coin",
            () -> new MysteriousCoinItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // “杰”厕灵 - 背饰槽位，免疫接下来3次任何伤害，之后损失最大生命的70%，内置冷却60秒
    public static final DeferredItem<Item> JIE_TOILET_CLEANER = ITEMS.registerSimpleItem("jie_toilet_cleaner",
            new Item.Properties().rarity(Rarity.EPIC));

    // 粗制辣味糖果 - 护符槽位，佩戴时免疫冰冻伤害与体温过低
    public static final DeferredItem<Item> CRUDE_SPICY_CANDY = ITEMS.registerSimpleItem("crude_spicy_candy",
            new Item.Properties().rarity(Rarity.RARE));

    // 火与钢 - 胸饰槽位，免疫火焰伤害，攻击附加火焰，攻击已燃烧目标无视护甲且伤害×5，每次攻击附加目标5%最大生命固定伤害
    public static final DeferredItem<Item> FIRE_AND_STEEL = ITEMS.registerSimpleItem("fire_and_steel",
            new Item.Properties().rarity(Rarity.EPIC));

    // 注册自定义锻造配方序列化器
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> UNBREAKABLE_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("unbreakable_smithing", UnbreakableSmithingRecipe.Serializer::new);

    // Creates a creative tab with the id "infinityminecraft:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.infinityminecraft")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
                output.accept(ARROW_DEFLECTION_RING.get());
                output.accept(FLAWLESS_STAR.get());
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
                output.accept(NUT_WALL.get());
                output.accept(METEOR_STREAK.get());
                output.accept(MYSTERIOUS_COIN.get());
                output.accept(JIE_TOILET_CLEANER.get());
                output.accept(CRUDE_SPICY_CANDY.get());
                output.accept(FIRE_AND_STEEL.get());
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

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (infinityminecraft) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
