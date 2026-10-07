package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 世界斩状态机——由 {@link com.himi.examplemod.item.WorldSlashItem} 右键激活后驱动。
 *
 * <p>激活后在锁定点开始一次持续 5 秒（{@value #TOTAL_TICKS} tick）的仪式，均匀降下
 * {@value #SLASH_COUNT} 道斩击；每道斩击：</p>
 * <ul>
 *   <li>沿随机方向生成一条彩虹渐变的粒子「切开」轨迹 + 中心多色爆发；</li>
 *   <li>播放挥砍音效（{@code PLAYER_ATTACK_SWEEP}）；</li>
 *   <li>对锁定点半径 {@value #ATTACK_RADIUS} 格内、除施法者外的所有存活实体造成
 *       {@value #SLASH_DAMAGE} 点世界斩伤害（自定义伤害类型 {@code infinitycraft:world_slash}，
 *       经标签 bypasses_armor / bypasses_resistance / bypasses_cooldown 配置为无视护甲/抗性/无敌帧），
 *       并施以定身（缓慢 255 + 对 Mob 额外 {@code setNoAi}）。</li>
 * </ul>
 *
 * <p>{@value #SLASH_COUNT} 道斩击全部落下后收尾：终结爆发粒子 + 爆炸音效，恢复被 {@code setNoAi}
 * 冻住的生物，清除锁定点半径内所有残存活物，最后施法者以世界斩伤害自尽——死亡播报
 * 「【玩家】因咒缚而付出生命」（伤害源以施法者为 causingEntity，走 base key）。</p>
 *
 * <p>仅在服务端结算（{@link ServerTickEvent.Post} 驱动）；任务列表为静态，单玩家同时至多一次。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class WorldSlashHandler {

    private static final int TOTAL_TICKS = 100;         // 5 秒
    private static final int SLASH_COUNT = 15;          // 15 道斩击
    private static final double ATTACK_RADIUS = 3.0D;   // 每斩 / 清除半径
    private static final double ATTACK_RADIUS_SQR = ATTACK_RADIUS * ATTACK_RADIUS;
    private static final float SLASH_DAMAGE = 100.0F;   // 每斩伤害
    private static final int SLOWDOWN_DURATION = 40;    // 定身（缓慢）时长，随每斩刷新，留缓冲覆盖斩击间隙
    private static final int SLOWDOWN_AMPLIFIER = 255;  // 缓慢 255：速度降至 0
    private static final double SLASH_LINE_HALF = 16.0D; // 斩击粒子线半长（单条斩击约 32 格）
    private static final double SLASH_DENSITY = 6.0D;    // 斩击线每格采样粒子数（更密集）
    private static final Vector3f BLACK = new Vector3f(0.0F, 0.0F, 0.0F); // 斩击粒子颜色：纯黑
    private static final int PORTAL_COUNT = 60;          // 每斩在斩击范围内生成的末影人紫色闪烁粒子数

    private static final List<Task> TASKS = new ArrayList<>();

    /** 激活一次世界斩：播放施法音效并登记任务（下一 tick 起开始降斩）。 */
    public static void start(ServerLevel level, Player caster, Vec3 target) {
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.0F, 1.0F);
        TASKS.add(new Task(level, caster, target));
    }

    /** 施法者是否已有一次世界斩进行中。 */
    public static boolean isActive(Player caster) {
        for (Task task : TASKS) {
            if (task.caster == caster) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (TASKS.isEmpty()) {
            return;
        }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            task.tick();
            if (task.done) {
                it.remove();
            }
        }
    }

    // ===================== 单道斩击 =====================

    private static void doSlash(Task task) {
        ServerLevel level = task.level;
        RandomSource rand = level.getRandom();
        Vec3 c = task.target;

        // 多色粒子切开效果 + 挥砍音效
        spawnSlashParticles(level, c, rand);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 2.0F, 0.8F + rand.nextFloat() * 0.4F);

        // 半径 3 格内、除施法者外的所有存活实体：100 点世界斩伤害 + 定身
        AABB area = new AABB(c, c).inflate(ATTACK_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != task.caster
                        && e.distanceToSqr(c.x, c.y, c.z) <= ATTACK_RADIUS_SQR);
        for (LivingEntity e : targets) {
            e.hurt(task.source, SLASH_DAMAGE);
            immobilize(task, e);
        }
    }

    /** 定身：缓慢 255（对玩家与生物均生效）；对 Mob 额外 setNoAi(true)，并记录以便收尾恢复。 */
    private static void immobilize(Task task, LivingEntity e) {
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWDOWN_DURATION, SLOWDOWN_AMPLIFIER));
        if (e instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            task.frozenMobs.add(mob);
        }
    }

    // ===================== 收尾 =====================

    private static void finish(Task task) {
        ServerLevel level = task.level;
        RandomSource rand = level.getRandom();
        Vec3 c = task.target;
        Player caster = task.caster;

        // 终结爆发：数道交叉的黑色巨型斩击 + 黑色大爆发 + 范围内紫色闪烁 + 爆炸音效
        for (int i = 0; i < 2; i++) {
            spawnSlashParticles(level, c, rand);
        }
        level.sendParticles(new DustParticleOptions(BLACK, 2.4F), c.x, c.y, c.z, 150, 2.6D, 2.6D, 2.6D, 0.22D);
        level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 120,
                ATTACK_RADIUS, ATTACK_RADIUS, ATTACK_RADIUS, 0.05D);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 2.5F, 0.7F);

        // 恢复被 setNoAi 冻住的生物（收尾稳健性，防止 AI 残留）
        for (Mob mob : task.frozenMobs) {
            if (!mob.isRemoved()) {
                mob.setNoAi(false);
            }
        }
        task.frozenMobs.clear();

        // 清除半径内残存活物（除施法者）——「直接清除」
        AABB area = new AABB(c, c).inflate(ATTACK_RADIUS);
        List<LivingEntity> remain = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != caster
                        && e.distanceToSqr(c.x, c.y, c.z) <= ATTACK_RADIUS_SQR);
        for (LivingEntity e : remain) {
            e.hurt(task.source, Float.MAX_VALUE);
        }

        // 施法者因咒缚付出生命（世界斩伤害源自尽 -> 触发专属死亡播报）
        if (caster.isAlive() && !caster.isRemoved()) {
            caster.hurt(task.source, Float.MAX_VALUE);
        }
    }

    // ===================== 粒子 =====================

    /** 一道斩击的粒子：沿随机方向的黑色致密切开线 + 中心黑色爆发 + 斩击范围内末影人紫色闪烁粒子。 */
    private static void spawnSlashParticles(ServerLevel level, Vec3 c, RandomSource rand) {
        double angle = rand.nextDouble() * Math.PI * 2.0D;
        double vy = (rand.nextDouble() - 0.5D) * 0.6D;
        Vec3 dir = new Vec3(Math.cos(angle), vy, Math.sin(angle)).normalize();
        Vec3 start = c.subtract(dir.scale(SLASH_LINE_HALF));

        // 黑色致密斩击线
        DustParticleOptions blackDust = new DustParticleOptions(BLACK, 2.0F);
        int steps = (int) (SLASH_LINE_HALF * 2.0D * SLASH_DENSITY);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = start.add(dir.scale(SLASH_LINE_HALF * 2.0D * t));
            level.sendParticles(blackDust, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        // 中心黑色爆发
        level.sendParticles(blackDust, c.x, c.y, c.z, 60, 0.9D, 0.9D, 0.9D, 0.1D);
        // 斩击范围内末影人闪烁的紫色粒子
        level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, PORTAL_COUNT,
                ATTACK_RADIUS, ATTACK_RADIUS, ATTACK_RADIUS, 0.02D);
    }

    // ===================== 任务 =====================

    private static final class Task {
        final ServerLevel level;
        final Player caster;
        final Vec3 target;
        final DamageSource source;
        final Set<Mob> frozenMobs = new HashSet<>();
        int elapsed = 0;
        int slashesDone = 0;
        boolean done = false;

        Task(ServerLevel level, Player caster, Vec3 target) {
            this.level = level;
            this.caster = caster;
            this.target = target;
            // 世界斩伤害源：以施法者为 causingEntity，死亡播报稳定走 base key，%1$s 即死者名
            Holder<DamageType> type = caster.damageSources().damageTypes
                    .getHolderOrThrow(infinitycraft.WORLD_SLASH_DAMAGE);
            this.source = new DamageSource(type, caster);
        }

        void tick() {
            // 将 SLASH_COUNT 道斩击均匀铺满 TOTAL_TICKS：第 k 斩落在 tick (k-1)*TOTAL_TICKS/(SLASH_COUNT-1)
            int should = Math.min(SLASH_COUNT, elapsed * (SLASH_COUNT - 1) / TOTAL_TICKS + 1);
            while (slashesDone < should) {
                doSlash(this);
                slashesDone++;
            }
            elapsed++;
            if (slashesDone >= SLASH_COUNT) {
                finish(this);
                done = true;
            }
        }
    }
}
