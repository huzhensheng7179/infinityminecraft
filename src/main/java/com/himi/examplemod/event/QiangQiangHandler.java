package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 「？！墙墙？！」（腰带）事件处理器。装备在 Curios 腰带栏位时：
 * 统计玩家背包中所有盔甲（含身上已穿盔甲、主背包、副手）的护甲值与盔甲韧性之和，
 * 作为额外加成应用到玩家的 {@link Attributes#ARMOR} / {@link Attributes#ARMOR_TOUGHNESS} 上；卸下时移除加成。
 *
 * <p>实现要点：</p>
 * <ul>
 *   <li>仅服务端计算，属性会自动同步到客户端；</li>
 *   <li>每 tick 重新统计，随背包装甲增减动态变化；</li>
 *   <li>{@code addOrUpdateTransientModifier} 内部按引用判定是否变更，故先用 {@code getModifier} 比对数值，
 *       仅在合计值真正变化时才更新，避免每 tick 触发无谓的属性同步（setDirty）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class QiangQiangHandler {

    private static final ResourceLocation ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "qiang_qiang.armor");
    private static final ResourceLocation TOUGHNESS_ID =
            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "qiang_qiang.toughness");

    /** 玩家 Tick：装备时把背包所有盔甲的护甲值/盔甲韧性之和加到玩家身上，卸下时移除。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armorAttr == null || toughnessAttr == null) {
            return;
        }

        if (!hasQiangQiang(player)) {
            armorAttr.removeModifier(ARMOR_ID);
            toughnessAttr.removeModifier(TOUGHNESS_ID);
            return;
        }

        // totals[0] = 护甲值合计，totals[1] = 盔甲韧性合计
        double[] totals = new double[2];
        Inventory inventory = player.getInventory();
        addArmorStats(inventory.items, totals);     // 主背包 36 格
        addArmorStats(inventory.armor, totals);     // 身上已穿盔甲 4 格
        addArmorStats(inventory.offhand, totals);   // 副手 1 格

        applyIfChanged(armorAttr, ARMOR_ID, totals[0]);
        applyIfChanged(toughnessAttr, TOUGHNESS_ID, totals[1]);
    }

    /** 累加一批物品栏中所有盔甲（{@link ArmorItem}）的护甲值与盔甲韧性到 totals。 */
    private static void addArmorStats(Iterable<ItemStack> stacks, double[] totals) {
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof ArmorItem armor) {
                totals[0] += armor.getDefense();
                totals[1] += armor.getToughness();
            }
        }
    }

    /** 仅当加成数值变化时更新修饰符，避免每 tick setDirty 触发无谓的属性同步。 */
    private static void applyIfChanged(AttributeInstance attr, ResourceLocation id, double value) {
        AttributeModifier existing = attr.getModifier(id);
        if (existing == null || existing.amount() != value) {
            attr.addOrUpdateTransientModifier(
                    new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** 检查玩家是否在 Curios 腰带栏位装备了「？！墙墙？！」。 */
    private static boolean hasQiangQiang(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinitycraft.QIANG_QIANG.get())))
                .isPresent();
    }
}
