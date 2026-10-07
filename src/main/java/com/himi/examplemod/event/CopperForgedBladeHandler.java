package com.himi.examplemod.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.himi.examplemod.infinitycraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * “铜铸之刃”事件处理器。
 *
 * <p>1. 铁砧熔铸（{@link AnvilUpdateEvent}）：左槽为铜铸之刃、右槽为任意药水（含喷溅/滞留）时，
 * 把药水效果熔铸进剑身——同种效果只保留等级最高的一条（等级相同则保留时长较长者），
 * 输出为携带全部已熔铸效果的铜铸之刃（保留原耐久与附魔），消耗 1 瓶药水与 {@value #ANVIL_COST} 级经验。
 * 反复合入不同药水即可在剑身累积多种效果。</p>
 *
 * <p>2. 攻击附加（{@link LivingDamageEvent.Pre}）：主手持铜铸之刃近战命中生物时，
 * 为受击目标附加剑身已熔铸的全部药水效果；瞬间效果（如治疗/伤害）按原版方式即时结算。</p>
 *
 * <p>熔铸的效果以原版 {@code minecraft:potion_contents} 组件的 customEffects 保存。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class CopperForgedBladeHandler {

    private static final int ANVIL_COST = 2;  // 熔铸一瓶药水的经验等级花费

    /** 铁砧：铜铸之刃（左）+ 任意药水（右）→ 熔铸了药水效果的铜铸之刃。 */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!left.is(infinitycraft.COPPER_FORGED_BLADE.get())) return;
        if (!(right.getItem() instanceof PotionItem)) return; // 只与药水合成

        PotionContents potionContents = right.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null || !potionContents.hasEffects()) return;

        PotionContents bladeContents = left.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        PotionContents merged = mergeEffects(bladeContents, potionContents);

        ItemStack output = left.copy(); // 保留耐久与附魔
        output.set(DataComponents.POTION_CONTENTS, merged);

        event.setOutput(output);
        event.setCost(ANVIL_COST);
        event.setMaterialCost(1); // 消耗 1 瓶药水
    }

    /** 近战命中：为受击目标附加剑身已熔铸的全部药水效果。 */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return; // 仅近战攻击

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(infinitycraft.COPPER_FORGED_BLADE.get())) return;

        LivingEntity victim = event.getEntity();
        if (victim == attacker) return;

        PotionContents contents = weapon.get(DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.hasEffects()) return;

        contents.forEachEffect(effect -> applyToTarget(effect, attacker, victim));
    }

    /** 把单条效果施加到目标：瞬间效果即时结算，持续效果按副本附加。 */
    private static void applyToTarget(MobEffectInstance effect, Player attacker, LivingEntity victim) {
        MobEffect mobEffect = effect.getEffect().value();
        if (mobEffect.isInstantenous()) {
            mobEffect.applyInstantenousEffect(attacker, attacker, victim, effect.getAmplifier(), 1.0);
        } else {
            victim.addEffect(new MobEffectInstance(effect));
        }
    }

    /** 合并两组药水效果：同种只保留等级最高者（等级相同取时长较长者）。 */
    private static PotionContents mergeEffects(PotionContents base, PotionContents incoming) {
        Map<MobEffect, MobEffectInstance> map = new LinkedHashMap<>();
        base.forEachEffect(effect -> mergeInto(map, effect));      // 剑身已有
        incoming.forEachEffect(effect -> mergeInto(map, effect));   // 本次药水
        return new PotionContents(Optional.empty(), Optional.empty(), new ArrayList<>(map.values()));
    }

    /** forEachEffect 已传入副本，可直接收纳；同种效果取等级最高、等级相同取时长较长者。 */
    private static void mergeInto(Map<MobEffect, MobEffectInstance> map, MobEffectInstance effect) {
        MobEffect key = effect.getEffect().value();
        MobEffectInstance old = map.get(key);
        if (old == null
                || effect.getAmplifier() > old.getAmplifier()
                || (effect.getAmplifier() == old.getAmplifier() && effect.getDuration() > old.getDuration())) {
            map.put(key, effect);
        }
    }
}
