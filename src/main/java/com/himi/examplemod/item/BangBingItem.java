package com.himi.examplemod.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 「棒冰」：胸饰（Curios body 槽位）饰品。
 *
 * <p>佩戴时为「所有」Curios 槽位类型各额外提供 {@value #BONUS_PER_SLOT} 个位置——即头饰 / 项链 / 戒指 /
 * 护符 / 胸饰 / 腰带 / 背饰每一栏都可多容纳 4 件饰品，卸下后自动恢复。</p>
 *
 * <p>实现要点（Curios 9.5.1 / MC 1.21.1）：</p>
 * <ul>
 *   <li>实现 {@link ICurioItem} 的物品，Curios 会自动为其 ItemStack 附加 ICurio capability
 *       （见 Curios#registerCaps 对 {@code BuiltInRegistries.ITEM} 的遍历），无需手动注册；</li>
 *   <li>装备时 Curios 回调 {@link #getAttributeModifiers}，此处用
 *       {@link CuriosApi#addSlotModifier(Multimap, String, ResourceLocation, double, AttributeModifier.Operation)}
 *       为每个槽位类型追加一个 {@code SlotAttribute} 修饰符（+4，{@code ADD_VALUE}）；
 *       Curios 内部据此 grow 对应槽位，卸下时对称 shrink，全程自动同步客户端；</li>
 *   <li>因该物品仅存在于 body 标签，只可能被装备进 body 槽，故无需再按 {@code slotContext} 过滤。</li>
 * </ul>
 */
public class BangBingItem extends Item implements ICurioItem {

    /** 本模组定义的全部 Curios 槽位类型（与 data/infinitycraft/curios/slots/*.json 一致）。 */
    private static final List<String> ALL_SLOT_TYPES =
            List.of("head", "necklace", "ring", "charm", "body", "belt", "back");

    /** 佩戴时每个槽位类型额外获得的位置数。 */
    private static final int BONUS_PER_SLOT = 4;

    public BangBingItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = LinkedHashMultimap.create();
        for (String slotType : ALL_SLOT_TYPES) {
            CuriosApi.addSlotModifier(modifiers, slotType, id, BONUS_PER_SLOT,
                    AttributeModifier.Operation.ADD_VALUE);
        }
        return modifiers;
    }
}
