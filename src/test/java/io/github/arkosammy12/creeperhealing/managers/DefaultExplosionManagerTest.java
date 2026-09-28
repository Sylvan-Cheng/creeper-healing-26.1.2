package io.github.arkosammy12.creeperhealing.managers;

import io.github.arkosammy12.creeperhealing.blocks.AffectedBlock;
import io.github.arkosammy12.creeperhealing.blocks.DefaultSerializedAffectedBlock;
import io.github.arkosammy12.creeperhealing.blocks.SingleAffectedBlock;
import io.github.arkosammy12.creeperhealing.explosions.DefaultSerializedExplosion;
import io.github.arkosammy12.creeperhealing.explosions.ExplosionEvent;
import io.github.arkosammy12.creeperhealing.explosions.ExplosionHealingMode;
import io.github.arkosammy12.creeperhealing.explosions.factories.ExplosionEventFactory;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultExplosionManagerTest {

    @Test
    void startingServerKeepsSavedModeSpecificBlockDelays() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var first = new DefaultSerializedAffectedBlock(SingleAffectedBlock.TYPE, BlockPos.ZERO,
                Blocks.STONE.defaultBlockState(), null, null, null, null, Level.OVERWORLD, 130, false);
        var second = new DefaultSerializedAffectedBlock(SingleAffectedBlock.TYPE, new BlockPos(1, 0, 0),
                Blocks.STONE.defaultBlockState(), null, null, null, null, Level.OVERWORLD, 130, false);
        ExplosionEvent restored = new DefaultSerializedExplosion(
                ExplosionHealingMode.DAYTIME_HEALING_MODE.getSerializedName(),
                List.of(first, second), 500, 0, 2, BlockPos.ZERO).asDeserialized();
        DefaultExplosionManager manager = new DefaultExplosionManager(DefaultSerializedExplosion.CODEC) {
            @Override
            public void readExplosionEvents(MinecraftServer server) {
                this.addExplosionEvent(new ExplosionEventFactory<>() {
                    @Override
                    public List<BlockPos> getAffectedPositions() { return List.of(BlockPos.ZERO); }

                    @Override
                    public ServerLevel getWorld() { return null; }

                    @Override
                    public ExplosionEvent createExplosionEvent() { return restored; }

                    @Override
                    public ExplosionEvent createExplosionEvent(List<BlockPos> positions, ServerLevel world) { return null; }

                    @Override
                    public ExplosionEvent createExplosionEvent(List<AffectedBlock> blocks, long timer) { return null; }

                    @Override
                    public ExplosionEvent createExplosionEvent(List<AffectedBlock> blocks, long timer, long delay) { return null; }
                });
            }
        };

        manager.onServerStarting(null);

        assertEquals(List.of(130L, 130L), manager.getExplosionEvents().findFirst().orElseThrow()
                .getAffectedBlocks().map(AffectedBlock::getBlockTimer).toList());
    }
}
