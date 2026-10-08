package com.himi.examplemod.client;

import com.himi.examplemod.infinitycraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 热砂世界环境粒子：在热砂世界（{@link infinitycraft#HOT_SAND_DIM}）的露天表面，
 * 于玩家周围持续飘散三种环境粒子——白色灰烬、火焰、红沙破坏粒子。
 *
 * <p>原版群系 effects.particle 每个群系只支持一种环境粒子，无法同时配置三种，
 * 故改由客户端 tick 事件统一生成；密度由 {@link #ATTEMPTS_PER_TICK} 与三种粒子的权重控制。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID, value = Dist.CLIENT)
public class HotSandAmbientParticles {

    /** 每 tick 的生成尝试次数（越大总密度越高）。 */
    private static final int ATTEMPTS_PER_TICK = 48;
    /** 玩家周围的水平生成半径（格）。 */
    private static final double HORIZONTAL_RANGE = 14.0;
    /** 玩家周围的垂直生成范围（格）。 */
    private static final double VERTICAL_RANGE = 7.0;

    // 三种环境粒子的相对权重（白灰烬 : 火焰 : 红沙破坏）
    private static final int WEIGHT_WHITE_ASH = 5;
    private static final int WEIGHT_FLAME = 3;
    private static final int WEIGHT_RED_SAND = 3;
    private static final int TOTAL_WEIGHT = WEIGHT_WHITE_ASH + WEIGHT_FLAME + WEIGHT_RED_SAND;

    /** 红沙破坏粒子：red_sand 方块的 block 碎裂粒子。 */
    private static final BlockParticleOption RED_SAND_PARTICLE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_SAND.defaultBlockState());

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;
        if (level.dimension() != infinitycraft.HOT_SAND_DIM) return;

        RandomSource random = level.getRandom();

        for (int i = 0; i < ATTEMPTS_PER_TICK; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 2.0 * HORIZONTAL_RANGE;
            double y = player.getY() + (random.nextDouble() - 0.5) * 2.0 * VERTICAL_RANGE;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 2.0 * HORIZONTAL_RANGE;

            // 仅在露天（可见天空）处生成，营造「世界表面」氛围，避免洞穴内刷屏
            if (!level.canSeeSky(BlockPos.containing(x, y, z))) continue;

            double vx = (random.nextDouble() - 0.5) * 0.02;
            double vy = random.nextDouble() * 0.015;
            double vz = (random.nextDouble() - 0.5) * 0.02;

            int roll = random.nextInt(TOTAL_WEIGHT);
            if (roll < WEIGHT_WHITE_ASH) {
                // 白色灰烬：缓慢飘落
                level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, vx, vy, vz);
            } else if (roll < WEIGHT_WHITE_ASH + WEIGHT_FLAME) {
                // 火焰：给一点向上初速度，呈现升腾感
                level.addParticle(ParticleTypes.FLAME, x, y, z, vx * 0.3, vy + 0.02, vz * 0.3);
            } else {
                // 红沙破坏粒子
                level.addParticle(RED_SAND_PARTICLE, x, y, z, vx, vy, vz);
            }
        }
    }
}
