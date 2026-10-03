package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 在物品提示（tooltip）中为本模组的饰品追加说明文本。
 * 文本存放在语言文件里，键名规则为 item.infinityminecraft.<物品路径>.desc，多行用 \n 分隔。
 * 约定：开头以中文引号包裹的若干行是「介绍台词」，其后是「效果说明」，
 * 两者用不同颜色区分——台词为暗紫斜体（原版 lore 风格），效果为淡蓝色。
 * 新增饰品只要在 lang 里补一条 .desc 就会自动显示，不用再动代码。
 */
@EventBusSubscriber(modid = infinityminecraft.MODID, value = Dist.CLIENT)
public class CurioTooltipHandler {

    private static final String QUOTE_OPEN = "\u201C";   // 中文左引号
    private static final String QUOTE_CLOSE = "\u201D";  // 中文右引号

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!infinityminecraft.MODID.equals(id.getNamespace())) return;

        String key = "item." + id.getNamespace() + "." + id.getPath() + ".desc";
        Language language = Language.getInstance();
        if (!language.has(key)) return;

        String[] split = language.getOrDefault(key).split("\n");

        // 判定「介绍台词」区域：首行以中文左引号开头，直到某行以中文右引号结束
        int quoteEnd = 0;
        if (split.length > 0 && split[0].startsWith(QUOTE_OPEN)) {
            quoteEnd = split.length;
            for (int i = 0; i < split.length; i++) {
                if (split[i].endsWith(QUOTE_CLOSE)) {
                    quoteEnd = i + 1;
                    break;
                }
            }
        }

        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            String line = split[i];
            if (line.isEmpty()) continue;
            if (i < quoteEnd) {
                // 介绍台词：暗紫 + 斜体
                lines.add(Component.literal(line).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            } else {
                // 效果说明：淡蓝
                lines.add(Component.literal(line).withStyle(ChatFormatting.AQUA));
            }
        }
        if (lines.isEmpty()) return;

        // 插入到物品名称（首行）之后
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }
}
