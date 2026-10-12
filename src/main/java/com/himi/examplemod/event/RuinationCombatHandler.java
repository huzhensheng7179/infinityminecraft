package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import com.himi.examplemod.item.RuinationItem;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 「破灭」各形态近战命中结算处理器（服务端权威）。
 *
 * <p>统一在伤害管线各阶段读取「攻击者主手持破灭 + 当前形态」，按形态施加被动/强化效果：</p>
 * <ul>
 *   <li>1 炎烧炽铁：命中点燃普通火焰；若右键蓄力存在则改为施加 {@code SOUL_BURN} 灵魂火灼烧；</li>
 *   <li>2 冽冰结雨：命中施加缓慢 II；</li>
 *   <li>3 霆云电幕：命中造成的伤害改为法术伤害（在 Incoming 清零、Post 以 magic 重施加）；</li>
 *   <li>4 枝生歹芳：命中回复「实际伤害×30%」生命（强化期 100%）；</li>
 *   <li>5 华光巨阙：被动横扫由形态属性提供；强化期每次命中额外执行一次「直接横扫」；</li>
 *   <li>6 辉桃百里：樱花领域期间命中触发穿透剑气；</li>
 *   <li>7 末地更新（不是）：强化期命中附带末影粒子并按目标类型施加凋零/中毒/缓慢。</li>
 * </ul>
 *
 * <p>「灵魂火灼烧期间被攻击无视伤害吸收」在 {@link LivingDamageEvent.Pre} 统一处理。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class RuinationCombatHandler {

    private static final int FIRE_TICKS_SECONDS = 5;         // 炎烧炽铁普通点燃秒数
    private static final int SLOW_DURATION = 100;            // 冽冰/末地缓慢等 5 秒
    private static final int STATUS_DURATION = 100;          // 末地凋零/中毒 5 秒

    /** 霆云电幕物理转法术：Incoming 记录原始伤害、Post 消费（key=受击者）。 */
    private static final Map<UUID, Float> PENDING_MAGIC = new HashMap<>();

    // ===================== Incoming：霆云物理转法术 =====================
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (!src.is(DamageTypes.PLAYER_ATTACK)) return;
        if (!isRuinationForm(attacker, 3)) return;
        // 记录本次近战原始伤害并清零物理结算，稍后在 Post 以法术伤害重施加（穿透护甲、受魔法减免）
        PENDING_MAGIC.put(event.getEntity().getUUID(), event.getAmount());
        event.setAmount(0.0F);
    }

    // ===================== Pre：灵魂火期间无视伤害吸收 =====================
    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (victim.hasEffect(infinitycraft.SOUL_BURN)) {
            float absorb = victim.getAbsorptionAmount();
            if (absorb > 0.0F) {
                // 使吸收完全失效：伤害 += 当前吸收量（吸收至多扣掉 absorb，故净扣血恰为原伤害）
                event.setNewDamage(event.getNewDamage() + absorb);
            }
        }
    }

    // ===================== Post：各形态命中效果 =====================
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        if (!(attacker.level() instanceof ServerLevel level)) return;
        LivingEntity victim = event.getEntity();
        if (victim == attacker) return;

        // 霆云电幕：把 Incoming 清零的物理伤害以法术伤害补上
        if (src.is(DamageTypes.PLAYER_ATTACK)) {
            Float pending = PENDING_MAGIC.remove(victim.getUUID());
            if (pending != null && pending > 0.0F && victim.isAlive()) {
                victim.invulnerableTime = 0;
                victim.hurt(attacker.damageSources().indirectMagic(attacker, attacker), pending);
            }
        }

        // 后续被动/强化均以「玩家近战 PLAYER_ATTACK」为触发前提
        if (!src.is(DamageTypes.PLAYER_ATTACK)) return;
        ItemStack main = attacker.getMainHandItem();
        if (!main.is(infinitycraft.RUINATION.get())) return;
        int form = RuinationItem.getForm(main);

        switch (form) {
            case 1 -> { // 炎烧炽铁：蓄力优先→灵魂火，否则普通点燃
                if (RuinationBuffs.consumeSoulCharge(attacker)) {
                    RuinationBuffs.applySoulBurn(victim);
                } else {
                    victim.igniteForSeconds(FIRE_TICKS_SECONDS);
                }
            }
            case 2 -> // 冽冰结雨：缓慢 II
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION, 1));
            case 3 -> { // 霆云电幕：强化期攻击清除目标正面效果
                if (RuinationBuffs.hasStorm(attacker)) {
                    clearBeneficialEffects(victim);
                }
            }
            case 4 -> { // 枝生歹芳：吸血（被动 30%，强化期 100%）
                float pct = RuinationBuffs.hasGank(attacker) ? 1.0F : 0.30F;
                float heal = event.getNewDamage() * pct;
                if (heal > 0.0F) {
                    attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
                }
            }
            case 5 -> { // 华光巨阙：强化期每次命中执行「直接横扫」
                if (RuinationBuffs.hasGiant(attacker)) {
                    doGiantSweep(level, attacker, victim);
                }
            }
            case 6 -> { // 辉桃百里：樱花领域期间命中释放穿透剑气
                if (RuinationBuffs.hasSakura(attacker)) {
                    RuinationBuffs.fireSakuraQi(attacker);
                }
            }
            case 7 -> { // 末地更新：强化期末影粒子 + 按类型施加状态
                if (RuinationBuffs.hasEnder(attacker)) {
                    applyEnderOnHit(level, victim);
                }
            }
            default -> {
            }
        }
    }

    // ===================== AttackEntityEvent：华光无冷却计数递减 =====================
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        if (attacker.level().isClientSide()) return;
        if (!isRuinationForm(attacker, 5)) return;
        if (RuinationBuffs.hasGiantNoCooldown(attacker)) {
            RuinationBuffs.consumeGiantHit(attacker); // 每次挥砍消耗一次“无冷却”额度
        }
    }

    // ===================== 具体机制 =====================

    /** 华光巨阙强化期「直接横扫」：对被击目标周围实体追加一次横扫伤害（无视冲刺/离地限制）。 */
    private static void doGiantSweep(ServerLevel level, Player attacker, LivingEntity victim) {
        double ratio = 5.0 / 6.0;
        float sweep = (float) (1.0 + attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * ratio);
        AABB area = victim.getBoundingBox().inflate(1.5D, 0.5D, 1.5D);
        DamageSource sweepSrc = attacker.damageSources().playerAttack(attacker);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != victim && e != attacker && e.isAlive())) {
            other.hurt(sweepSrc, sweep);
        }
    }

    /** 龙血魅影（形态7）强化期命中：末影粒子 + 亡灵凋零、节肢中毒、其余生物（如苦力怕/末影龙/玩家）获得全部效果。 */
    private static void applyEnderOnHit(ServerLevel level, LivingEntity victim) {
        level.sendParticles(ParticleTypes.PORTAL,
                victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                24, victim.getBbWidth(), victim.getBbHeight() * 0.5D, victim.getBbWidth(), 0.1D);
        if (victim.getType().is(EntityTypeTags.UNDEAD)) {
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, STATUS_DURATION, 0));
        } else if (victim.getType().is(EntityTypeTags.ARTHROPOD)) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, STATUS_DURATION, 0));
        } else {
            // 非亡灵、非节肢的生物（苦力怕、末影龙、玩家等）：施加全部负面效果
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, STATUS_DURATION, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, STATUS_DURATION, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STATUS_DURATION, 0));
        }
    }

    /** 霆云电幕强化期：移除目标身上所有「有益」类别的正面效果。 */
    private static void clearBeneficialEffects(LivingEntity victim) {
        for (MobEffectInstance inst : List.copyOf(victim.getActiveEffects())) {
            if (inst.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                victim.removeEffect(inst.getEffect());
            }
        }
    }

    // ===================== 工具 =====================

    /** 攻击者主手是否持有「破灭」且处于指定形态。 */
    private static boolean isRuinationForm(Player attacker, int form) {
        ItemStack main = attacker.getMainHandItem();
        return main.is(infinitycraft.RUINATION.get()) && RuinationItem.getForm(main) == form;
    }
}
