package io.github.arkosammy12.creeperhealing.blocks;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface SerializedAffectedBlock {

    String getAffectedBlockTypeName();

    BlockPos getBlockPos();

    BlockState getBlockState();

    ResourceKey<Level> getWorldRegistryKey();

    long getBlockTimer();

    boolean isPlaced();

    <T> Optional<T> getCustomData(String name, Class<T> clazz);

    AffectedBlock asDeserialized();

}
