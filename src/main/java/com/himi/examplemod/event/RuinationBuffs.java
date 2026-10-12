package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import com.himi.examplemod.item.RuinationItem;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 「破灭」各形态右键技能的状态管理器。
 *
 * <p>所有形态的计时 buff、一次性标记与持续领域都以「玩家持久化数据」为唯一真相源
 * （键前缀 {@code ruin_}，存 {@code long}=领域/强化的绝对结束 gameTime，或 {@code int}=剩余次数），
 * 由单一 {@link PlayerTickEvent.Post} 驱动在服务端维护瞬时属性修改器、领域伤害与粒子，
 * 到期自动清理，并随存档持久化。</p>
 *
 * <p>右键激活入口由 {@link RuinationItem#use} 分发到本类的 {@code activateX} 方法。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class RuinationBuffs {

    // ============ 持久化数据键 ============
    private static final String KEY_SOUL_CHARGE = "ruin_soul_charge"; // 1=下次近战附带灵魂火
    private static final String KEY_ICE_END = "ruin_ice_end";         // 冽冰结雨降雪领域结束 gameTime
    private static final String KEY_STORM_END = "ruin_storm_end";     // 霆云电幕强化结束 gameTime
    private static final String KEY_GANK_END = "ruin_gank_end";       // 枝生歹芳强化结束 gameTime
    private static final String KEY_GIANT_END = "ruin_giant_end";     // 华光巨阙强化结束 gameTime
    private static final String KEY_GIANT_HITS = "ruin_giant_hits";   // 华光巨阙剩余「无冷却」攻击次数
    private static final String KEY_SAKURA_END = "ruin_sakura_end";   // 辉桃百里樱花领域结束 gameTime
    private static final String KEY_ENDER_END = "ruin_ender_end";     // 末地更新强化结束 gameTime

    // ============ 时长（tick）============
    private static final int ICE_DURATION = 320;      // 16 秒
    private static final int STORM_DURATION = 240;    // 12 秒
    private static final int GANK_DURATION = 360;     // 18 秒
    private static final int GIANT_DURATION = 400;    // 20 秒
    private static final int SAKURA_DURATION = 320;   // 16 秒
    private static final int ENDER_DURATION = 240;    // 12 秒
    private static final int SOUL_BURN_DURATION = 240; // 12 秒（施加给目标）

    // ============ 领域参数 ============
    private static final double ICE_RADIUS = 12.0D;
    private static final float ICE_COLD_DAMAGE = 2.0F;   // 每秒 2 点
    private static final int ICE_SLOW_AMPLIFIER = 3;     // 缓慢 IV
    private static final double SAKURA_QI_RANGE = 12.0D;
    private static final float SAKURA_QI_DAMAGE = 18.0F;

    // ============ 属性修改器 id ============
    private static final ResourceLocation REACH_MOD = rl("ruination.reach");
    private static final ResourceLocation ATTACK_SPEED_GIANT_MOD = rl("ruination.attack_speed_giant");
    private static final ResourceLocation ATTACK_SPEED_SAKURA_MOD = rl("ruination.attack_speed_sakura");
    private static final ResourceLocation SPEED_HOLD_SAKURA_MOD = rl("ruination.speed_hold_sakura");

    /** 形态 1 炎烧炽铁：右键强化——置「下次近战附带灵魂火」一次性标记。 */
    public static void activateSoulCharge(Player player) {
        persist(player).putInt(KEY_SOUL_CHARGE, 1);
        fxCast(player, SoundEvents.FIRECHARGE_USE);
    }

    /** 命中时消耗并返回「灵魂火蓄力」是否存在。 */
    public static boolean consumeSoulCharge(Player player) {
        CompoundTag tag = persist(player);
        if (tag.getInt(KEY_SOUL_CHARGE) > 0) {
            tag.putInt(KEY_SOUL_CHARGE, 0);
            return true;
        }
        return false;
    }

    /** 形态 2 冽冰结雨：右键释放降雪领域。 */
    public static void activateIce(Player player) {
        setEnd(player, KEY_ICE_END, ICE_DURATION);
        fxCast(player, SoundEvents.SPLASH_POTION_THROW);
    }

    /** 形态 3 霆云电幕：右键强化（清除命中目标正面效果），闪电单独由 use 结算。 */
    public static void activateStorm(Player player) {
        setEnd(player, KEY_STORM_END, STORM_DURATION);
        fxCast(player, SoundEvents.LIGHTNING_BOLT_THUNDER);
    }

    /** 形态 4 枝生歹芳：右键强化——距离+3、伤害吸收IV、吸血 100%。 */
    public static void activateGank(Player player) {
        setEnd(player, KEY_GANK_END, GANK_DURATION);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, GANK_DURATION, 3)); // 伤害吸收 IV
        fxCast(player, SoundEvents.BEACON_POWER_SELECT);
    }

    /** 形态 5 华光巨阙：右键强化——力量III(20s)、距离+2、下 3 次攻击无冷却并强制横扫。 */
    public static void activateGiant(Player player) {
        long end = setEnd(player, KEY_GIANT_END, GIANT_DURATION);
        persist(player).putInt(KEY_GIANT_HITS, 3);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, GIANT_DURATION, 2)); // 力量 III
        fxCast(player, SoundEvents.IRON_TRAPDOOR_CLOSE);
    }

    /** 形态 6 辉桃百里：右键释放樱花领域（领域内攻速大幅提升 + 左键释放剑气）。 */
    public static void activateSakura(Player player) {
        setEnd(player, KEY_SAKURA_END, SAKURA_DURATION);
        fxCast(player, SoundEvents.NOTE_BLOCK_HARP.value());
    }

    /** 形态 7 末地更新（不是）：右键强化——命中附带末影粒子并按类型施加状态、封锁末影人瞬移。 */
    public static void activateEnder(Player player) {
        setEnd(player, KEY_ENDER_END, ENDER_DURATION);
        fxCast(player, SoundEvents.ENDERMAN_TELEPORT);
    }

    // ============ 查询（供其它处理器判定）============
    public static boolean hasStorm(Player p) { return active(p, KEY_STORM_END); }
    public static boolean hasGank(Player p) { return active(p, KEY_GANK_END); }
    public static boolean hasGiant(Player p) { return active(p, KEY_GIANT_END); }
    public static boolean hasSakura(Player p) { return active(p, KEY_SAKURA_END); }
    public static boolean hasEnder(Player p) { return active(p, KEY_ENDER_END); }

    /** 华光巨阙「无冷却」剩余次数 > 0（并在外部命中后调用 {@link #consumeGiantHit} 递减）。 */
    public static boolean hasGiantNoCooldown(Player p) {
        return persist(p).getInt(KEY_GIANT_HITS) > 0;
    }

    /** 消耗一次华光巨阙无冷却攻击计数；返回消耗后剩余次数。 */
    public static int consumeGiantHit(Player p) {
        CompoundTag tag = persist(p);
        int left = tag.getInt(KEY_GIANT_HITS) - 1;
        if (left < 0) left = 0;
        tag.putInt(KEY_GIANT_HITS, left);
        return left;
    }

    /** 施加灵魂火灼烧效果给目标（炎烧炽铁右键强化命中）。 */
    public static void applySoulBurn(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(infinitycraft.SOUL_BURN, SOUL_BURN_DURATION, 0));
        if (victim.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                    40, victim.getBbWidth(), victim.getBbHeight() * 0.5D, victim.getBbWidth(), 0.02D);
        }
    }

    // ============ 每 tick 维护（服务端）============
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // 1) 攻击距离：枝生强化 +3、华光强化 +2 叠加
        double reach = (hasGank(player) ? 3.0D : 0.0D) + (hasGiant(player) ? 2.0D : 0.0D);
        applyModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MOD, reach, AttributeModifier.Operation.ADD_VALUE);

        // 2) 攻速：华光「无冷却」拉满；樱花领域大幅提升
        applyModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_GIANT_MOD,
                hasGiantNoCooldown(player) ? 100.0D : 0.0D, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_SAKURA_MOD,
                hasSakura(player) ? 6.0D : 0.0D, AttributeModifier.Operation.ADD_VALUE);

        // 3) 辉桃百里被动：手持（主/副手）时速度 II
        applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED_HOLD_SAKURA_MOD,
                isHoldingForm(player, 6) ? 0.4D : 0.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // 4) 冽冰结雨领域：每 tick 降雪粒子；每 20 tick 对半径内敌人造成寒冷伤害 + 缓慢 IV
        if (active(player, KEY_ICE_END)) {
            maintainIce(player, level);
        }
        // 5) 辉桃百里领域：头顶持续飘落樱花粒子
        if (active(player, KEY_SAKURA_END)) {
            maintainSakura(player, level);
        }
    }

    private static void maintainIce(Player player, ServerLevel level) {
        double r = ICE_RADIUS;
        // 降雪粒子：在玩家周围 r 范围顶部薄层生成
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + 2.0D, player.getZ(),
                20, r, 1.0D, r, 0.0D);
        if (player.tickCount % 20 != 0) return;
        AABB area = player.getBoundingBox().inflate(r);
        DamageSource cold = coldSource(player);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= r * r)) {
            t.hurt(cold, ICE_COLD_DAMAGE);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, ICE_SLOW_AMPLIFIER));
        }
    }

    private static void maintainSakura(Player player, ServerLevel level) {
        // 在玩家周围顶部薄层随机生成自定义樱花花瓣粒子（sakura_petal 引用两张贴图,SpriteSet 随机取一→两种贴图都会出现）
        for (int i = 0; i < 8; i++) {
            double dx = (level.random.nextDouble() - 0.5D) * 10.0D;
            double dz = (level.random.nextDouble() - 0.5D) * 10.0D;
            double dy = 2.0D + level.random.nextDouble() * 2.5D;
            level.sendParticles(infinitycraft.SAKURA_PETAL.get(),
                    player.getX() + dx, player.getY() + dy, player.getZ() + dz,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 辉桃百里剑气：沿视线 hitscan ≤12 格，穿透路径上所有生物各造成 18 点伤害，并以自发光樱花粒子渲染弧形月牙剑气。 */
    public static void fireSakuraQi(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(SAKURA_QI_RANGE));
        AABB path = player.getBoundingBox().expandTowards(look.scale(SAKURA_QI_RANGE)).inflate(1.0D);
        DamageSource qi = magicSource(player);
        // 穿透：对射线路径包围盒内、除自己外的存活生物一律结算（用实体射线过滤真正被击中的）
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, path,
                e -> e != player && e.isAlive())) {
            AABB tb = t.getBoundingBox().inflate(0.3D);
            // 线段与实体包围盒是否相交
            if (segmentIntersectsAABB(eye, end, tb)) {
                t.invulnerableTime = 0;
                t.hurt(qi, SAKURA_QI_DAMAGE);
            }
        }
        // 弧形剑气:沿视线铺多道抛物线弧环,用自发光樱花粒子(sakura_slash)拼成一弯向前飞行的月牙
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        Vec3 bladeUp = right.cross(look).normalize();
        int rings = 12;   // 沿射程铺演的弧环数(越远越迟,形成推进感)
        int perArc = 11;  // 每道月牙的粒子数
        double width = 1.4D; // 月牙半宽
        double depth = 1.1D; // 月牙拱高
        for (int r = 0; r <= rings; r++) {
            double ft = r / (double) rings;
            Vec3 center = eye.add(look.scale(SAKURA_QI_RANGE * ft));
            double scale = 0.5D + 1.3D * ft; // 月牙由小变大,强化「斩出」的张力
            for (int a = 0; a < perArc; a++) {
                double s = -1.0D + 2.0D * (a / (double) (perArc - 1)); // -1..1
                double arc = (1.0D - s * s) * depth;                    // 抛物线拱
                Vec3 p = center
                        .add(right.scale(s * width * scale))
                        .add(bladeUp.scale(arc * scale));
                level.sendParticles(infinitycraft.SAKURA_SLASH.get(), p.x, p.y, p.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    /** 线段 p0->p1 是否与 AABB 相交（slab 法）。 */
    private static boolean segmentIntersectsAABB(net.minecraft.world.phys.Vec3 p0, net.minecraft.world.phys.Vec3 p1, AABB box) {
        net.minecraft.world.phys.Vec3 dir = p1.subtract(p0);
        double tMin = 0.0D, tMax = 1.0D;
        double[] o = {p0.x, p0.y, p0.z};
        double[] d = {dir.x, dir.y, dir.z};
        double[][] bn = {{box.minX, box.maxX}, {box.minY, box.maxY}, {box.minZ, box.maxZ}};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-8D) {
                if (o[i] < bn[i][0] || o[i] > bn[i][1]) return false;
            } else {
                double t1 = (bn[i][0] - o[i]) / d[i];
                double t2 = (bn[i][1] - o[i]) / d[i];
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) return false;
            }
        }
        return true;
    }

    // ============ 工具 ============
    private static DamageSource coldSource(Player player) {
        Holder<DamageType> type = player.damageSources().damageTypes.getHolderOrThrow(infinitycraft.RUINATION_COLD);
        return new DamageSource(type, player);
    }

    /** 剑气按「法术伤害」结算（复用原版 magic，攻击者归因，穿透护甲、受抗性/保护减免）。 */
    private static DamageSource magicSource(Player player) {
        return player.damageSources().indirectMagic(player, player);
    }

    /** 是否主/副手持「破灭」且处于指定形态。 */
    public static boolean isHoldingForm(Player player, int form) {
        return isForm(player.getMainHandItem(), form) || isForm(player.getOffhandItem(), form);
    }

    private static boolean isForm(ItemStack stack, int form) {
        return stack.is(infinitycraft.RUINATION.get()) && RuinationItem.getForm(stack) == form;
    }

    private static CompoundTag persist(Player player) {
        return player.getPersistentData();
    }

    private static long setEnd(Player player, String key, int durationTicks) {
        long end = player.level().getGameTime() + durationTicks;
        persist(player).putLong(key, end);
        return end;
    }

    private static boolean active(Player player, String key) {
        return persist(player).getLong(key) > player.level().getGameTime();
    }

    private static void fxCast(Player player, net.minecraft.sounds.SoundEvent sound) {
        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute, ResourceLocation id,
                                      double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (amount == 0.0D) {
            if (instance.getModifier(id) != null) instance.removeModifier(id);
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.amount() == amount) return;
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, path);
    }
}
