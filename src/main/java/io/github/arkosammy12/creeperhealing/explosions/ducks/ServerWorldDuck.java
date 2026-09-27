package io.github.arkosammy12.creeperhealing.explosions.ducks;

import java.util.Collection;
import net.minecraft.core.BlockPos;

public interface ServerWorldDuck {

    void creeperhealing$addAffectedPositions(Collection<BlockPos> affectedPositions);

    void creeperhealing$clearAffectedPositions();

    boolean creeperhealing$isAffectedPosition(BlockPos pos);

}
