package io.github.arkosammy12.creeperhealing.blocks;

import io.github.arkosammy12.monkeyconfig.base.Setting;
import io.github.arkosammy12.monkeyconfig.sections.maps.StringMapSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import io.github.arkosammy12.creeperhealing.config.ConfigUtils;
import io.github.arkosammy12.creeperhealing.explosions.ExplosionEvent;
import io.github.arkosammy12.creeperhealing.util.ExplosionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DoubleAffectedBlock extends SingleAffectedBlock {

    private final BlockPos secondHalfPos;
    private final BlockState secondHalfState;
    @Nullable
    private final CompoundTag secondHalfNbt;

    public static final String TYPE = "double_affected_block";

    protected DoubleAffectedBlock(BlockPos firstHalfPos, BlockState firstHalfState, @Nullable CompoundTag firstHalfNbt, @Nullable BlockPos secondHalfPos, @Nullable BlockState secondHalfState, @Nullable CompoundTag secondHalfNbt, ResourceKey<Level> registryKey, long affectedBlockTimer, boolean placed) {
        super(firstHalfPos, firstHalfState, registryKey, firstHalfNbt, affectedBlockTimer, placed);
        this.secondHalfNbt = secondHalfNbt;
        if (secondHalfState == null) {
            if (firstHalfState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf secondHalf = firstHalfState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).equals(DoubleBlockHalf.UPPER) ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER;
                this.secondHalfState = firstHalfState.getBlock().withPropertiesOf(firstHalfState).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, secondHalf);
            } else if (firstHalfState.hasProperty(BlockStateProperties.BED_PART)) {
                BedPart secondBedPart = firstHalfState.getValue(BlockStateProperties.BED_PART).equals(BedPart.HEAD) ? BedPart.FOOT : BedPart.HEAD;
                this.secondHalfState = firstHalfState.getBlock().withPropertiesOf(firstHalfState).setValue(BlockStateProperties.BED_PART, secondBedPart);
            } else {
                this.secondHalfState = null;
            }
        } else {
            this.secondHalfState = secondHalfState;
        }
        if (secondHalfPos == null) {
            this.secondHalfPos = getOtherHalfPos(firstHalfPos, firstHalfState);
        } else {
            this.secondHalfPos = secondHalfPos;
        }
    }

    @Override
    protected String getAffectedBlockType() {
        return TYPE;
    }

    public BlockState getSecondHalfState() {
        return this.secondHalfState;
    }

    public BlockPos getSecondHalfPos() {
        return this.secondHalfPos;
    }

    @Nullable
    public CompoundTag getSecondHalfNbt() {
        return this.secondHalfNbt;
    }

    @Override
    public SerializedAffectedBlock asSerialized() {
        return new DefaultSerializedAffectedBlock(this.getAffectedBlockType(), this.getBlockPos(), this.getBlockState(), this.getNbt(), this.getSecondHalfPos(), this.getSecondHalfState(), this.getSecondHalfNbt(), this.getWorldRegistryKey(), this.getBlockTimer(), this.isPlaced());
    }

    @Override
    protected boolean shouldForceHeal() {
        boolean forceBlocksWithNbtToAlwaysHeal = ConfigUtils.getRawBooleanSetting(ConfigUtils.FORCE_BLOCKS_WITH_NBT_TO_ALWAYS_HEAL);
        return (this.getNbt() != null || this.getSecondHalfNbt() != null) && forceBlocksWithNbtToAlwaysHeal;
    }

    @Override
    protected boolean tryHealing(MinecraftServer server, ExplosionEvent currentExplosionEvent) {
        if (this.secondHalfState == null) {
            return super.tryHealing(server, currentExplosionEvent);
        }

        BlockState firstHalfState = this.getBlockState();
        BlockPos firstHalfPos = this.getBlockPos();
        BlockState secondHalfState = this.secondHalfState;
        BlockPos secondHalfPos = this.secondHalfPos;
        Level world = this.getWorld(server);
        boolean stateReplaced = false;

        String blockIdentifier = BuiltInRegistries.BLOCK.getKey(firstHalfState.getBlock()).toString();
        StringMapSection replaceMapSection = ConfigUtils.getRawStringMapSection(ConfigUtils.REPLACE_MAP);
        Setting<String, ?> replaceMapValue = replaceMapSection.get(blockIdentifier);
        // Hardcode an exception to allow beds to be replaced with other blocks despite them having an Nbt tag.
        if (replaceMapValue != null && (!this.shouldForceHeal() || firstHalfState.is(BlockTags.BEDS))) {
            try {
                Identifier replacementId = Identifier.parse(replaceMapValue.getValue().getRaw());
                if (!BuiltInRegistries.BLOCK.containsKey(replacementId)) {
                    throw new IllegalArgumentException("Unknown block: " + replacementId);
                }
                Block replacement = BuiltInRegistries.BLOCK.getValue(replacementId);
                if (replacement != null) {
                    firstHalfState = replacement.withPropertiesOf(firstHalfState);
                    secondHalfState = replacement.withPropertiesOf(secondHalfState);
                    stateReplaced = true;
                }
            } catch (RuntimeException e) {
                io.github.arkosammy12.creeperhealing.CreeperHealing.LOGGER.warn("Invalid replacement for {}: {}", blockIdentifier, replaceMapValue.getValue().getRaw(), e);
            }
        }


        // Prevent both halves of a double block from being replaced with two of a single regular block
        if (!firstHalfState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && !firstHalfState.hasProperty(BlockStateProperties.BED_PART)) {
            return super.tryHealing(server, currentExplosionEvent);
        }

        if (!this.shouldForceHeal() && (!canRestoreAt(world, firstHalfPos, firstHalfState) || !canRestoreAt(world, secondHalfPos, secondHalfState))) {
            return false;
        }

        ExplosionUtils.pushEntitiesUpwards(world, firstHalfPos, firstHalfState, firstHalfState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF));
        if (!firstHalfState.equals(world.getBlockState(firstHalfPos)) && !ExplosionUtils.placeRestoredBlock(world, firstHalfPos, firstHalfState)) {
            return false;
        }
        if (!secondHalfState.equals(world.getBlockState(secondHalfPos)) && !ExplosionUtils.placeRestoredBlock(world, secondHalfPos, secondHalfState)) {
            return false;
        }
        if (!firstHalfState.equals(world.getBlockState(firstHalfPos)) || !secondHalfState.equals(world.getBlockState(secondHalfPos))) {
            return false;
        }

        boolean healFirstHalfNbt = this.getNbt() != null && !stateReplaced;
        if (healFirstHalfNbt) {
            world.setBlockEntity(BlockEntity.loadStatic(firstHalfPos, firstHalfState, this.getNbt(), world.registryAccess()));
        }
        boolean healSecondHalfNbt = this.secondHalfNbt != null && !stateReplaced;
        if (healSecondHalfNbt) {
            world.setBlockEntity(BlockEntity.loadStatic(secondHalfPos, secondHalfState, this.secondHalfNbt, world.registryAccess()));
        }
        ExplosionUtils.playBlockPlacementSoundEffect(world, firstHalfPos, firstHalfState);
        ExplosionUtils.spawnParticles(world, firstHalfPos);
        return true;
    }

    private static boolean canRestoreAt(Level world, BlockPos pos, BlockState state) {
        BlockState current = world.getBlockState(pos);
        return current.canBeReplaced() || current.equals(state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleAffectedBlock that)) return false;
        return Objects.equals(getBlockPos(), that.getBlockPos()) && Objects.equals(getBlockState(), that.getBlockState()) && Objects.equals(this.getSecondHalfPos(), that.getSecondHalfPos()) && Objects.equals(this.getSecondHalfState(), that.getSecondHalfState()) && Objects.equals(getWorldRegistryKey(), that.getWorldRegistryKey());
    }

    @Override
    public String toString() {
        return "DoubleAffectedBlock(firstHalfPos=%s, firstHalfState=%s, firstHalfNbt=%s, secondHalfPos=%s, secondHalfState=%s, secondHalfNbt=%s, world=%s, timer=%s, placed=%s)"
                .formatted(this.getBlockPos(), this.getBlockState(), this.getNbt(), this.secondHalfPos, this.secondHalfState, this.secondHalfNbt, this.getWorldRegistryKey(), this.getBlockTimer(), this.isPlaced());
    }

    @Nullable
    public static BlockPos getOtherHalfPos(BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return DoubleAffectedBlock.getSecondDoubleBlockHalfPos(pos, state);
        } else if (state.hasProperty(BlockStateProperties.BED_PART)) {
            return DoubleAffectedBlock.getSecondBedBlockHalfPos(pos, state);
        }
        return null;
    }

    public static BlockPos getSecondDoubleBlockHalfPos(BlockPos firstHalfPos, BlockState firstHalfState) {
        return firstHalfState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).equals(DoubleBlockHalf.UPPER) ? firstHalfPos.below() :  firstHalfPos.above();
    }

    public static BlockPos getSecondBedBlockHalfPos(BlockPos firstHalfPos, BlockState firstHalfState) {
        BedPart secondBedPart = firstHalfState.getValue(BlockStateProperties.BED_PART).equals(BedPart.HEAD) ? BedPart.FOOT : BedPart.HEAD;
        Direction firstBedPartOrientation = firstHalfState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return switch (secondBedPart) {
            case HEAD -> switch (firstBedPartOrientation) {
                case NORTH -> firstHalfPos.north();
                case SOUTH -> firstHalfPos.south();
                case EAST -> firstHalfPos.east();
                default -> firstHalfPos.west();
            };
            case FOOT -> switch (firstBedPartOrientation) {
                case NORTH -> firstHalfPos.south();
                case SOUTH -> firstHalfPos.north();
                case EAST -> firstHalfPos.west();
                default -> firstHalfPos.east();
            };
        };
    }

}
