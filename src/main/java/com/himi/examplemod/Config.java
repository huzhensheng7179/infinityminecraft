package com.himi.examplemod;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    // 神秘石球全局黑名单：列入此处的物品注册 ID 不会从神秘石球（所有奖励类别）开出，
    // 也不会经刷子考古产物注入掉落；用于剔除其他模组的管理员/生存不可获得/过强物品，或本模组不想产出的物品。
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STONE_BALL_BLACKLIST = BUILDER
            .comment(
                    "神秘石球全局黑名单：此处列出的物品不会从神秘石球开出（覆盖矿物/模板/附魔金苹果/本模组物品/其他模组物品/失落古籍等所有类别），",
                    "也不会通过刷子刷扫可疑沙/沙砾的考古产物注入掉落。",
                    "用于剔除其他模组的管理员物品、生存无法获得的物品、过强物品，或任何你不想让石球产出的物品。",
                    "填写物品注册 ID，例如 \"somemod:admin_sword\"、\"minecraft:enchanted_golden_apple\"；留空表示不额外剔除。",
                    "石球改动：重启游戏或在模组配置界面重载配置后生效；刷子改动：需 /reload 或重启生效。")
            .defineListAllowEmpty("stoneBallBlacklist", List.of(), () -> "", Config::validateResourceId);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    // 校验黑名单条目是合法的物品注册 ID（namespace:path）；不要求该物品当前已注册，
    // 以便预先屏蔽尚未安装/可选模组中的物品。
    private static boolean validateResourceId(final Object obj) {
        if (!(obj instanceof String text)) {
            return false;
        }
        try {
            ResourceLocation.parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
