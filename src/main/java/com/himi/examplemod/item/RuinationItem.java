package com.himi.examplemod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import com.himi.examplemod.event.RuinationBuffs;
import com.himi.examplemod.infinitycraft;

/**
 * “破灭”：一把可在 8 种形态之间切换的剑。
 *
 * <p>基础形态为「剑柄」，手持时按住 Shift 再右键可循环切换到下一个形态，共 7 把剑身形态。
 * 每次切换会改写物品上的三样内容（均随 ItemStack 组件自动同步到客户端）：
 * <ol>
 *   <li>{@code CUSTOM_DATA} 里的形态编号——驱动客户端 {@code infinitycraft:form} 物品属性切换贴图；</li>
 *   <li>{@code ATTRIBUTE_MODIFIERS} 组件——替换为该形态的攻击伤害与攻速；</li>
 *   <li>{@code ITEM_NAME} 组件——显示当前形态的名称。</li>
 * </ol>
 * 各形态的伤害/攻速集中在下面的 {@link #DAMAGE_AMOUNTS} 与 {@link #SPEED_AMOUNTS} 数组，便于调整。</li>
 */
public class RuinationItem extends SwordItem {

    /** 形态总数：0 为剑柄，1~7 为七把剑身。 */
    public static final int FORM_COUNT = 8;

    /** CUSTOM_DATA 中存放当前形态编号的键。 */
    private static final String TAG_FORM = "ruination_form";

    /**
     * 各形态的攻击伤害基线值（传给 {@link SwordItem#createAttributes}，最终显示伤害 = 基线 + 玩家基础 1）。
     * 采用原版标准属性修饰符 id，才能像其它剑一样在提示中显示为「合计值」而非带符号增量。
     * 索引即形态编号：0=破灭(2)、1=炎烧炽铁(15)、2=冽冰结雨(12)、3=霆云电幕(16)、
     * 4=枝生歹芳(13)、5=华光巨阙(20)、6=辉桃百里(8)、7=末地更新(17)。
     */
    private static final double[] DAMAGE_AMOUNTS = {1.0, 14.0, 11.0, 15.0, 12.0, 19.0, 7.0, 16.0};

    /**
     * 各形态的攻速修饰符值（传给 {@link SwordItem#createAttributes}，最终显示攻速 = 4 + 本值）。
     * 对应显示：0=1.4、1=1.6、2=2.1、3=1.1、4=1.9、5=0.9、6=2.0、7=1.6。
     */
    private static final double[] SPEED_AMOUNTS = {-2.6, -2.4, -1.9, -2.9, -2.1, -3.1, -2.0, -2.4};

    public RuinationItem(Item.Properties properties) {
        super(RuinationTier.INSTANCE, properties);
    }

    /** 读取当前形态编号（无组件时视为 0）。 */
    public static int getForm(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.floorMod(tag.getInt(TAG_FORM), FORM_COUNT);
    }

    /** 构建指定形态的攻击伤害与攻速修饰符（复用 {@link SwordItem#createAttributes} 的原版标准 id，确保提示按合计值显示）。 */
    private static ItemAttributeModifiers buildModifiers(int form) {
        ItemAttributeModifiers mods = SwordItem.createAttributes(RuinationTier.INSTANCE,
                (float) DAMAGE_AMOUNTS[form], (float) SPEED_AMOUNTS[form]);
        // 形态 5 华光巨阙：常驻横扫伤害比率 = 5/6（等价横扫之刃 V，横扫伤害 = 1 + 基础伤害 × 比率）。
        if (form == 5) {
            mods = mods.withModifierAdded(Attributes.SWEEPING_DAMAGE_RATIO,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "ruination.giant_sweep"),
                            5.0 / 6.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        return mods;
    }

    /** 形态显示名对应的语言键。 */
    public static Component formName(int form) {
        return Component.translatable("item.infinitycraft.ruination.form." + form);
    }

    /**
     * 物品栈从存档/网络加载后的回调（原版 {@code ItemStack} 在反序列化时会调用）。
     * 借此刻意「自愈」：按当前形态用原版标准 id 重新写回属性与显示名，
     * 使早期版本遗留的自定义-id 脏组件在下次加载时自动更正，无需玩家手动重切形态。
     */
    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        super.verifyComponentsAfterLoad(stack);
        // 幂等自愈：仅当属性组件缺失、或仍是早期版本遗留的「自定义-id」脏数据时，
        // 才按当前形态用原版标准 id 重写一次；已是 base id 则跳过，避免每次加载都改写组件造成抖动。
        if (!hasBaseIdModifiers(stack)) {
            applyForm(stack, getForm(stack));
        }
    }

    /** 判断栈上是否已带有原版标准 id（base_attack_damage/base_attack_speed）的属性修饰符。 */
    private static boolean hasBaseIdModifiers(ItemStack stack) {
        ItemAttributeModifiers mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods == null) {
            return false;
        }
        final boolean[] ok = {false};
        mods.forEach(net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND, (attr, m) -> {
            if (m.is(Item.BASE_ATTACK_DAMAGE_ID) || m.is(Item.BASE_ATTACK_SPEED_ID)) {
                ok[0] = true;
            }
        });
        return ok[0];
    }

    /**
     * 应用形态：写入编号、替换属性组件、更新显示名。服务端与客户端都会调用（组件同步后两端一致）。
     */
    public static void applyForm(ItemStack stack, int form) {
        form = Math.floorMod(form, FORM_COUNT);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_FORM, form);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, buildModifiers(form));
        stack.set(DataComponents.ITEM_NAME, formName(form));
    }

    /**
     * 右键行为：
     * <ul>
     *   <li>按住 Shift：循环切换形态（原有行为）；</li>
     *   <li>未按 Shift（仅主手）：释放当前形态的右键技能，并按形态施加冷却。</li>
     * </ul>
     * 技能与服务端权威结算，客户端仅通过组件/持久数据同步表现。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int next = (getForm(stack) + 1) % FORM_COUNT;
                applyForm(stack, next);
                player.displayClientMessage(formName(next), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
                player.getCooldowns().addCooldown(this, 6);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        // 非 Shift 右键：主手释放当前形态技能
        if (level.isClientSide || hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        int form = getForm(stack);
        if (form == 0) {
            return InteractionResultHolder.pass(stack); // 剑柄无技能
        }
        ServerLevel sl = (ServerLevel) level;
        if (!activateSkill(sl, player, form)) {
            return InteractionResultHolder.pass(stack);
        }
        player.getCooldowns().addCooldown(this, SKILL_COOLDOWNS[form]);
        return InteractionResultHolder.sidedSuccess(stack, true);
    }

    /** 各形态右键技能冷却（tick）；索引即形态编号，0 不使用。 */
    private static final int[] SKILL_COOLDOWNS = {0, 120, 560, 400, 560, 640, 560, 480};

    /** 分发到对应形态的右键技能；返回是否已消耗本次右键。 */
    private static boolean activateSkill(ServerLevel level, Player player, int form) {
        switch (form) {
            case 1 -> RuinationBuffs.activateSoulCharge(player);          // 炎烧炽铁：下次近战附带灵魂火
            case 2 -> RuinationBuffs.activateIce(player);                 // 冽冰结雨：降雪领域
            case 3 -> {                                                     // 霆云电幕：指向闪电 + 自身强化
                strikeLightning(level, player);
                RuinationBuffs.activateStorm(player);
            }
            case 4 -> RuinationBuffs.activateGank(player);                 // 枝生歹芳：距离/吸收/吸血强化
            case 5 -> RuinationBuffs.activateGiant(player);                // 华光巨阙：力量/无冷却/强制横扫
            case 6 -> RuinationBuffs.activateSakura(player);               // 辉桃百里：樱花领域
            case 7 -> RuinationBuffs.activateEnder(player);                // 末地更新：末影强化
            default -> {
                return false;
            }
        }
        return true;
    }

    /** 霆云电幕右键：在指针目标处（最远 32 格）落一道纯视觉闪电，对落点半径 3 格内除施法者外的生物造成 16 点法术伤害。 */
    private static void strikeLightning(ServerLevel level, Player player) {
        HitResult hit = player.pick(32.0D, 0.0F, true);
        Vec3 target = hit instanceof EntityHitResult ehr
                ? ehr.getEntity().position()
                : hit.getLocation();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target); // 仅保留闪电视觉，伤害由下方手动结算（保证为法术伤害）
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        DamageSource magic = player.damageSources().indirectMagic(player, player);
        AABB area = new AABB(target, target).inflate(3.0D);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive())) {
            living.hurt(magic, 16.0F);
        }
    }
}
