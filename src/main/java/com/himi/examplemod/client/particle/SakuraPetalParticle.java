package com.himi.examplemod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 辉桃百里「樱花领域」飘落花瓣粒子。
 *
 * <p>引用 {@code sakura_petal.json} 中的两张贴图（sakura_1/sakura_2），{@link SpriteSet#pickSprite}
 * 会随机取一，因此同一领域内两种花瓣贴图都会出现。粒子缓慢下落并自旋，营造花瓣飘零的氛围。</p>
 */
public class SakuraPetalParticle extends TextureSheetParticle {

    public SakuraPetalParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.quadSize *= 1.4F;                        // 花瓣略大更易辨认
        this.lifetime = 50 + this.random.nextInt(30); // 1.5~3 秒飘落
        this.gravity = 0.03F;                         // 轻微下坠
        this.roll = this.random.nextFloat() * (float) Math.PI * 2.0F;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        super.tick();
        // 飘落时左右摆动 + 自旋,增强花瓣轻盈感
        this.xd += (double) (this.random.nextFloat() - 0.5F) * 0.002D;
        this.zd += (double) (this.random.nextFloat() - 0.5F) * 0.002D;
        this.oRoll = this.roll;
        this.roll += 0.08F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            SakuraPetalParticle particle = new SakuraPetalParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites); // 从两张贴图中随机取一
            return particle;
        }
    }
}
