package io.github.arkosammy12.creeperhealing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.arkosammy12.creeperhealing.explosions.ducks.ServerWorldDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.arkosammy12.creeperhealing.explosions.ducks.ExplosionImplDuck;
import io.github.arkosammy12.creeperhealing.util.callbacks.DaylightCycleEvents;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin implements ServerWorldDuck {

    @Unique
    private final Set<BlockPos> affectedBlockPositions = new HashSet<>();

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;moveToTimeMarker(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceKey;)Z", shift = At.Shift.AFTER))
    private void fastForwardDaytimeHealingModeExplosionsOnNightSkipped(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        DaylightCycleEvents.ON_NIGHT_SKIPPED.invoker().onNightSkipped(((ServerLevel) (Object) this), shouldKeepTicking);
    }

    @WrapOperation(method = "explode", at = @At(value = "NEW", target = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Explosion$BlockInteraction;)Lnet/minecraft/world/level/ServerExplosion;"))
    private ServerExplosion attachExplosionSourceTypeToExplosion(ServerLevel world, Entity entity, DamageSource damageSource, ExplosionDamageCalculator behavior, Vec3 pos, float power, boolean createFire, Explosion.BlockInteraction destructionType, Operation<ServerExplosion> original, @Local(argsOnly = true) Level.ExplosionInteraction explosionSourceType) {
        ServerExplosion explosion = original.call(world, entity, damageSource, behavior, pos, power, createFire, destructionType);
        ((ExplosionImplDuck) explosion).creeperhealing$setExplosionSourceType(explosionSourceType);
        return explosion;
    }

    @Override
    public void creeperhealing$addAffectedPositions(Collection<BlockPos> affectedPositions) {
        this.affectedBlockPositions.addAll(affectedPositions);
    }

    @Override
    public void creeperhealing$clearAffectedPositions() {
        this.affectedBlockPositions.clear();
    }

    @Override
    public boolean creeperhealing$isAffectedPosition(BlockPos pos) {
        return this.affectedBlockPositions.contains(pos);
    }

}

