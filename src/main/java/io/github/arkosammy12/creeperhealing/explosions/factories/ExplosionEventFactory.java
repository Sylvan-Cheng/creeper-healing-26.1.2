package io.github.arkosammy12.creeperhealing.explosions.factories;

import org.jetbrains.annotations.Nullable;
import io.github.arkosammy12.creeperhealing.blocks.AffectedBlock;
import io.github.arkosammy12.creeperhealing.explosions.ExplosionEvent;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface ExplosionEventFactory<T extends ExplosionEvent> {

    @Nullable
    List<BlockPos> getAffectedPositions();

    ServerLevel getWorld();

    @Nullable
    T createExplosionEvent();

    @Nullable
    T createExplosionEvent(List<BlockPos> affectedPositions, ServerLevel world);

    @Nullable
    T createExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer);

    @Nullable
    T createExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, long blockHealDelay);

}
