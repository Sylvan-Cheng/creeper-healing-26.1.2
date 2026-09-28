package io.github.arkosammy12.creeperhealing.explosions;

import io.github.arkosammy12.creeperhealing.blocks.AffectedBlock;
import io.github.arkosammy12.creeperhealing.blocks.SerializedAffectedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractExplosionEventTest {

    @Test
    void waitsForEachBlockBeforeAdvancingToTheNext() {
        TestBlock first = new TestBlock(20, false);
        TestBlock second = new TestBlock(20, false);
        TestExplosionEvent event = new TestExplosionEvent(first, second);

        for (int tick = 1; tick <= 20; tick++) {
            event.tick(null);
            assertFalse(first.isPlaced());
            assertFalse(second.isPlaced());
            assertSame(first, event.getAffectedBlocks().findFirst().orElseThrow());
        }

        event.tick(null);
        assertTrue(first.isPlaced());
        assertFalse(second.isPlaced());
        assertEquals(1, event.getBlockCounter());

        for (int tick = 22; tick <= 41; tick++) {
            event.tick(null);
            assertFalse(second.isPlaced());
        }
        event.tick(null);
        assertTrue(second.isPlaced());
        assertEquals(2, event.getBlockCounter());
    }

    @Test
    void rotatesOnlyAfterAPlacementAttemptIsBlocked() {
        TestBlock blocked = new TestBlock(2, true);
        TestBlock next = new TestBlock(2, false);
        TestExplosionEvent event = new TestExplosionEvent(blocked, next);

        event.tick(null);
        event.tick(null);
        assertSame(blocked, event.getAffectedBlocks().findFirst().orElseThrow());

        event.tick(null);
        assertEquals(1, blocked.attempts);
        assertSame(next, event.getAffectedBlocks().findFirst().orElseThrow());

        for (int tick = 0; tick < 3; tick++) {
            event.tick(null);
        }
        assertTrue(next.isPlaced());
        assertFalse(blocked.isPlaced());
    }

    @Test
    void lightGateKeepsTaskUntilLightReturns() {
        TestBlock block = new TestBlock(130, false);
        boolean[] lightAvailable = {false};
        TestExplosionEvent event = new TestExplosionEvent(block) {
            @Override
            protected boolean canHealNow(Level world) {
                if (!lightAvailable[0]) {
                    this.healTimer = 20;
                }
                return lightAvailable[0];
            }
        };

        for (int tick = 0; tick < 250; tick++) {
            assertEquals(ExplosionEvent.TickResult.WAITING, event.tickAndReport(null));
            assertFalse(event.isFinished());
        }
        assertEquals(130, block.getBlockTimer());

        lightAvailable[0] = true;
        for (int tick = 0; tick < 152 && !block.isPlaced(); tick++) {
            event.tick(null);
        }
        assertTrue(block.isPlaced());
    }

    @Test
    void blockedFinalBlockRemainsPendingAndCanHealLater() {
        TestBlock block = new TestBlock(0, true);
        TestExplosionEvent event = new TestExplosionEvent(block);

        for (int tick = 0; tick < 100; tick++) {
            event.tick(null);
        }
        assertFalse(event.isFinished());
        assertFalse(block.isPlaced());
        assertEquals(0, event.getBlockCounter());
        assertTrue(block.attempts >= 4);

        block.blocked = false;
        for (int tick = 0; tick < 21 && !block.isPlaced(); tick++) {
            event.tick(null);
        }
        assertTrue(block.isPlaced());
        event.tick(null);
        assertTrue(event.isFinished());
    }

    private static class TestExplosionEvent extends AbstractExplosionEvent {
        TestExplosionEvent(AffectedBlock... blocks) {
            super(new ArrayList<>(List.of(blocks)), -1, 0, 1, BlockPos.ZERO);
        }

        @Override
        protected ExplosionHealingMode getHealingMode() {
            return ExplosionHealingMode.DEFAULT_MODE;
        }

        @Override
        public void setup(ServerLevel world) {
        }

        @Override
        public ServerLevel getWorld(MinecraftServer server) {
            return null;
        }
    }

    private static final class TestBlock implements AffectedBlock {
        private long timer;
        private boolean placed;
        private boolean blocked;
        private int attempts;

        TestBlock(long timer, boolean blocked) {
            this.timer = timer;
            this.blocked = blocked;
        }

        @Override
        public long getBlockTimer() {
            return timer;
        }

        @Override
        public void tick(ExplosionEvent event, MinecraftServer server) {
            if (--timer < 0) {
                attempts++;
                if (blocked) {
                    timer = 20;
                } else {
                    placed = true;
                }
            }
        }

        @Override
        public boolean isPlaced() {
            return placed;
        }

        @Override
        public void setPlaced() {
            placed = true;
        }

        @Override
        public BlockPos getBlockPos() {
            return BlockPos.ZERO;
        }

        @Override
        public BlockState getBlockState() {
            return null;
        }

        @Override
        public ResourceKey<Level> getWorldRegistryKey() {
            return null;
        }

        @Override
        public ServerLevel getWorld(MinecraftServer server) {
            return null;
        }

        @Override
        public boolean canBePlaced(MinecraftServer server) {
            return !blocked;
        }

        @Override
        public SerializedAffectedBlock asSerialized() {
            return null;
        }
    }
}
