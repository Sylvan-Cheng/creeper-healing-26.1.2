package io.github.arkosammy12.creeperhealing.explosions;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import io.github.arkosammy12.creeperhealing.blocks.AffectedBlock;

import java.util.stream.Stream;

public interface ExplosionEvent {

    void setup(ServerLevel world);

    Stream<AffectedBlock> getAffectedBlocks();

    ServerLevel getWorld(MinecraftServer server);

    long getHealTimer();

    boolean isFinished();

    void tick(MinecraftServer server);

    default TickResult tickAndReport(MinecraftServer server) {
        this.tick(server);
        return TickResult.UNKNOWN_CHANGE;
    }

    enum TickResult {
        WAITING(false), HEALING_STARTED(true), BLOCK_PLACED(true), RETRY_SCHEDULED(true), UNKNOWN_CHANGE(true);

        private final boolean progress;

        TickResult(boolean progress) {
            this.progress = progress;
        }

        public boolean hasProgress() {
            return this.progress;
        }
    }

    SerializedExplosionEvent asSerialized();

}
