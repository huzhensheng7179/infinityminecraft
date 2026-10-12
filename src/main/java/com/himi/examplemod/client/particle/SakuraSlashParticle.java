package com.himi.examplemod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 辉桃百里「樱花剑气」弧片粒子。
 *
 * <p>对应 {@code sakura_slash.json}（emissive=true,自发光）,引用两种樱花贴图并随机取一。
 * 由 {@code RuinationBuffs.fireSakuraQi} 沿视线以抛物线弧环批量铺点,拼成一弯向前飞行的月牙剑气;
 * 粒子本身短寿、快速淡出并沿飞行方向自旋,以体现剑气的锋锐与流动感。</p>
 */
public class SakuraSlashParticle extends TextureSheetParticle {

    public SakuraSlashParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.quadSize *= 1.1F;
        this.lifetime = 10 + this.random.nextInt(6); // 短暂停留即淡出,强化「飞过」的速度感
        this.gravity = 0.0F;                         // 不受重力,保持剑气直线推进的观感
        this.roll = this.random.nextFloat() * (float) Math.PI * 2.0F;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        this.alpha = this.age < this.lifetime
                ? 1.0F - (float) this.age / (float) this.lifetime : 0.0F; // 随寿命线性淡出
        this.oRoll = this.roll;
        this.roll += 0.15F; // 剑气片高速自旋
        super.tick();
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
            SakuraSlashParticle particle = new SakuraSlashParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites); // 从两张贴图中随机取一
            return particle;
        }
    }
}
