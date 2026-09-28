package io.github.arkosammy12.creeperhealing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import io.github.arkosammy12.creeperhealing.util.ExplosionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;

@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {

    @WrapOperation(method = "onPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V"))
    private void schedulePlacedBlockTick(Level world, BlockPos pos, Block block, int delay, Operation<Void> original) {
        if (ExplosionUtils.shouldScheduleFallingBlock(pos)) {
            original.call(world, pos, block, delay);
        }
    }

    @WrapOperation(method = "updateShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ScheduledTickAccess;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V"))
    private void scheduleNeighborUpdateTick(ScheduledTickAccess world, BlockPos pos, Block block, int delay, Operation<Void> original) {
        if (ExplosionUtils.shouldScheduleFallingBlock(pos)) {
            original.call(world, pos, block, delay);
        }
    }
}
