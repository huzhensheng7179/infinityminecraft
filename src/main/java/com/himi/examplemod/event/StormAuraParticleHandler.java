package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

/**
 * 风暴光环粒子处理器：为三个 buff 在玩家周身持续生成环绕粒子。
 *
 * <ul>
 *   <li>巧乐兹风暴 / 雪碧风暴：白色环绕粒子；</li>
 *   <li>你跑不过我你信不信（由前两者合成，与风暴互斥）：蓝色环绕粒子。</li>
 * </ul>
 *
 * <p>粒子由服务端 {@link ServerLevel} 的 sendParticles 广播：玩家的 MobEffect 不会同步给
 * 其他客户端，只有服务端广播才能让周围所有玩家都看到光环。每 tick 沿玩家身体的旋转圆周
 * 生成粒子（相位随 tickCount 旋转），形成环绕周身的光环。仅在服务端结算。</p>
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class StormAuraParticleHandler {

    // 白色尘埃粒子（巧乐兹风暴 / 雪碧风暴）
    private static final DustParticleOptions WHITE_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F);
    // 蓝色尘埃粒子（你跑不过我你信不信）
    private static final DustParticleOptions BLUE_DUST =
            new DustParticleOptions(new Vector3f(0.2F, 0.5F, 1.0F), 1.2F);

    private static final double WHITE_RADIUS = 0.7;   // 白色环绕半径
    private static final double BLUE_RADIUS = 0.85;   // 蓝色环绕半径（终极 buff，光环更大）
    private static final int WHITE_PER_TICK = 3;      // 白色每 tick 粒子数
    private static final int BLUE_PER_TICK = 4;       // 蓝色每 tick 粒子数
    private static final double SPIN_PER_TICK = 0.22; // 环绕旋转相位增量（弧度/tick）

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // 你跑不过我你信不信：蓝色环绕（由两个风暴合成而来，与风暴互斥，优先显示）
        if (player.hasEffect(infinityminecraft.CANT_CATCH_ME)) {
            spawnRing(level, player, BLUE_DUST, BLUE_PER_TICK, BLUE_RADIUS);
            return;
        }
        // 巧乐兹风暴 / 雪碧风暴：白色环绕
        if (player.hasEffect(infinityminecraft.CHOCO_STORM)
                || player.hasEffect(infinityminecraft.XUEBI_STORM)) {
            spawnRing(level, player, WHITE_DUST, WHITE_PER_TICK, WHITE_RADIUS);
        }
    }

    /**
     * 环绕周身：每 tick 沿玩家身体的水平圆周生成 perTick 个粒子，圆周相位随 tickCount
     * 旋转，粒子纵向随机落在身高范围内，形成旋转环绕的光环。
     * sendParticles 传 count=1 且 offset=0 -> 在精确坐标生成单个粒子（不额外随机散布）。
     */
    private static void spawnRing(ServerLevel level, Player player, ParticleOptions particle,
                                  int perTick, double radius) {
        double cx = player.getX();
        double cy = player.getY();
        double cz = player.getZ();
        double height = player.getBbHeight();
        RandomSource rand = player.getRandom();
        double spin = player.tickCount * SPIN_PER_TICK;
        for (int i = 0; i < perTick; i++) {
            double angle = spin + (2.0 * Math.PI * i / perTick) + rand.nextDouble() * 0.3;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = cy + 0.15 + rand.nextDouble() * (height - 0.25);
            level.sendParticles(particle, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
