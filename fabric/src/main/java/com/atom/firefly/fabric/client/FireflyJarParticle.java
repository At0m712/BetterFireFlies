package com.atom.firefly.fabric.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class FireflyJarParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public FireflyJarParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, sprites.get(level.getRandom()));
        this.sprites = sprites;
        this.lifetime = 35 + this.random.nextInt(35);
        this.quadSize = 0.055F;
        this.hasPhysics = false;
        this.xd = (this.random.nextDouble() - 0.5D) * 0.008D;
        this.yd = (this.random.nextDouble() - 0.5D) * 0.006D;
        this.zd = (this.random.nextDouble() - 0.5D) * 0.008D;
        this.setSpriteFromAge(sprites);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return 15728880;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd += (this.random.nextDouble() - 0.5D) * 0.003D;
        this.yd += (this.random.nextDouble() - 0.5D) * 0.002D;
        this.zd += (this.random.nextDouble() - 0.5D) * 0.003D;

        this.xd *= 0.90D;
        this.yd *= 0.88D;
        this.zd *= 0.90D;

        this.move(this.xd, this.yd, this.zd);
        this.setSpriteFromAge(this.sprites);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
            return new FireflyJarParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
