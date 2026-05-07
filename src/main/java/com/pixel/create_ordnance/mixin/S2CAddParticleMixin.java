package com.pixel.create_ordnance.mixin;

import com.pixel.create_ordnance.CreateOrdnance;
import com.pixel.create_ordnance.config.OrdnanceConfigs;

import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into AAAParticles' {@code ParticleEmitterInfo} to limit particle
 * rendering distance on the client side.
 *
 * <p>Cancels {@code spawnInWorld} when the emitter origin is farther than
 * {@code maxParticleDistance} (configurable in client config). This prevents
 * excessive particle load from distant projectile trails.</p>
 *
 * @see com.pixel.create_ordnance.config.OrdnanceClientConfig
 */
@Mixin(value = ParticleEmitterInfo.class, remap = false)
public abstract class S2CAddParticleMixin {

    @Inject(method = "spawnInWorld", at = @At("HEAD"), cancellable = true)
    private void create_ordnance$onSpawnInWorld(Level level, Player player, CallbackInfo ci) {
        ParticleEmitterInfo info = (ParticleEmitterInfo) (Object) this;

        // Only apply the distance limit to particles from our mod
        if (info.effek != null && info.effek.getNamespace().equals(CreateOrdnance.MODID)) {
            if (info.isPositionSet()) {
                Player localPlayer = player != null ? player : Minecraft.getInstance().player;

                if (localPlayer != null) {
                    double maxDist = OrdnanceConfigs.CLIENT.maxParticleDistance.get();
                    double distSqr = localPlayer.position().distanceToSqr(info.position());

                    if (distSqr > maxDist * maxDist) {
                        ci.cancel();
                    }
                }
            }
        }
    }
}