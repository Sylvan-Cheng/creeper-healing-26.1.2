package io.github.arkosammy12.creeperhealing.util;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record ExplosionContext(
        List<BlockPos> vanillaAffectedPositions,
        List<BlockPos> indirectlyAffectedPositions,
        Map<BlockPos, Tuple<BlockState, BlockEntity>> affectedStatesAndBlockEntities,
        ServerLevel world,
        Level.ExplosionInteraction explosionSourceType
) {
}
