package io.github.arkosammy12.creeperhealing.explosions;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.arkosammy12.creeperhealing.blocks.DefaultSerializedAffectedBlock;
import io.github.arkosammy12.creeperhealing.blocks.DoubleAffectedBlock;
import io.github.arkosammy12.creeperhealing.blocks.SingleAffectedBlock;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializedExplosionEventTest {

    @BeforeAll
    static void bootStrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @ParameterizedTest
    @EnumSource(ExplosionHealingMode.class)
    void restoresEveryAdvertisedHealingModeWithBlockData(ExplosionHealingMode mode) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("CustomName", "saved chest");
        BlockPos pos = new BlockPos(4, 70, -3);
        var block = new DefaultSerializedAffectedBlock(SingleAffectedBlock.TYPE, pos,
                Blocks.CHEST.defaultBlockState(), nbt, null, null, null, Level.OVERWORLD, 37, false);
        var saved = new DefaultSerializedExplosion(mode.getSerializedName(), List.of(block), 90, 0, 3, pos);

        SerializedExplosionEvent decoded = roundTrip(saved);
        AbstractExplosionEvent event = assertInstanceOf(expectedType(mode), decoded.asDeserialized());
        SingleAffectedBlock restoredBlock = assertInstanceOf(SingleAffectedBlock.class,
                event.getAffectedBlocks().findFirst().orElseThrow());

        assertEquals(90, event.getHealTimer());
        assertEquals(0, event.getBlockCounter());
        assertEquals(mode.getSerializedName(), event.asSerialized().getExplosionTypeName());
        assertEquals(pos, restoredBlock.getBlockPos());
        assertEquals(Blocks.CHEST.defaultBlockState(), restoredBlock.getBlockState());
        assertEquals(Level.OVERWORLD, restoredBlock.getWorldRegistryKey());
        assertEquals(37, restoredBlock.getBlockTimer());
        assertEquals(nbt, restoredBlock.getNbt());
    }

    @Test
    void restoresBothHalvesOfADoubleBlock() {
        BlockPos lowerPos = new BlockPos(2, 64, 2);
        BlockPos upperPos = lowerPos.above();
        var lowerState = Blocks.OAK_DOOR.defaultBlockState();
        var upperState = lowerState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        var block = new DefaultSerializedAffectedBlock(DoubleAffectedBlock.TYPE, lowerPos, lowerState,
                null, upperPos, upperState, null, Level.OVERWORLD, 25, false);
        var saved = new DefaultSerializedExplosion(ExplosionHealingMode.DEFAULT_MODE.getSerializedName(),
                List.of(block), 50, 0, 2, lowerPos);

        ExplosionEvent event = roundTrip(saved).asDeserialized();
        DoubleAffectedBlock restored = assertInstanceOf(DoubleAffectedBlock.class,
                event.getAffectedBlocks().findFirst().orElseThrow());

        assertEquals(lowerPos, restored.getBlockPos());
        assertEquals(lowerState, restored.getBlockState());
        assertEquals(upperPos, restored.getSecondHalfPos());
        assertEquals(upperState, restored.getSecondHalfState());
        assertEquals(25, restored.getBlockTimer());
    }

    @Test
    void resumesPartiallyHealedExplosionAtSavedBlock() {
        var first = new DefaultSerializedAffectedBlock(SingleAffectedBlock.TYPE, new BlockPos(0, 64, 0),
                Blocks.STONE.defaultBlockState(), null, null, null, null, Level.NETHER, -1, true);
        var second = new DefaultSerializedAffectedBlock(SingleAffectedBlock.TYPE, new BlockPos(1, 64, 0),
                Blocks.STONE.defaultBlockState(), null, null, null, null, Level.NETHER, 12, false);
        var saved = new DefaultSerializedExplosion(ExplosionHealingMode.DEFAULT_MODE.getSerializedName(),
                List.of(first, second), -1, 1, 2, BlockPos.ZERO);

        AbstractExplosionEvent event = assertInstanceOf(DefaultExplosionEvent.class, roundTrip(saved).asDeserialized());
        var blocks = event.getAffectedBlocks().toList();

        assertEquals(1, event.getBlockCounter());
        assertTrue(blocks.getFirst().isPlaced());
        assertFalse(blocks.getLast().isPlaced());
        assertEquals(12, blocks.getLast().getBlockTimer());
        assertEquals(Level.NETHER, blocks.getLast().getWorldRegistryKey());
    }

    private static SerializedExplosionEvent roundTrip(SerializedExplosionEvent saved) {
        JsonElement json = DefaultSerializedExplosion.CODEC.encodeStart(JsonOps.INSTANCE, saved).result().orElseThrow();
        return DefaultSerializedExplosion.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
    }

    private static Class<? extends AbstractExplosionEvent> expectedType(ExplosionHealingMode mode) {
        return switch (mode) {
            case DEFAULT_MODE -> DefaultExplosionEvent.class;
            case DAYTIME_HEALING_MODE -> DaytimeExplosionEvent.class;
            case DIFFICULTY_BASED_HEALING_MODE -> DifficultyBasedExplosionEvent.class;
            case BLAST_RESISTANCE_BASED_HEALING_MODE -> BlastResistanceBasedExplosionEvent.class;
        };
    }
}
