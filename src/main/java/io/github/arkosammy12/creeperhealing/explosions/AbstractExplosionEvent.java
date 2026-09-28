package io.github.arkosammy12.creeperhealing.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import io.github.arkosammy12.creeperhealing.blocks.AffectedBlock;
import io.github.arkosammy12.creeperhealing.config.ConfigUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class AbstractExplosionEvent implements ExplosionEvent {

    private final List<AffectedBlock> affectedBlocks;
    protected long healTimer;
    private int blockCounter;
    protected boolean finished;
    private boolean healingStarted;
    private final int radius;
    private final BlockPos center;

    public AbstractExplosionEvent(List<AffectedBlock> affectedBlocks, int radius, BlockPos center) {
        this(affectedBlocks, ConfigUtils.getExplosionHealDelay(), 0, radius, center);
    }

    public AbstractExplosionEvent(List<AffectedBlock> affectedBlocks, long healTimer, int blockCounter, int radius, BlockPos center) {
        this.affectedBlocks = affectedBlocks;
        this.healTimer = healTimer;
        this.blockCounter = blockCounter;
        this.center = center;
        this.radius = radius;
    }

    @Override
    public Stream<AffectedBlock> getAffectedBlocks() {
        return this.affectedBlocks.stream();
    }

    @Override
    public ServerLevel getWorld(MinecraftServer server) {
        return server.getLevel(this.affectedBlocks.getFirst().getWorldRegistryKey());
    }

    @Override
    public long getHealTimer() {
        return this.healTimer;
    }

    public int getBlockCounter() {
        return this.blockCounter;
    }

    public BlockPos getCenter() {
        return this.center;
    }

    public int getRadius() {
        return this.radius;
    }

    @Override
    public boolean isFinished() {
        return this.finished;
    }

    protected final Optional<AffectedBlock> getCurrentAffectedBlock() {
        return this.blockCounter < this.affectedBlocks.size() ? Optional.of(this.affectedBlocks.get(this.blockCounter)) : Optional.empty();
    }

    protected final void incrementCounter() {
        this.blockCounter++;
    }

    public final void setHealTimer(long timer) {
        this.healTimer = timer;
    }

    protected boolean canHealNow(Level world) {
        return true;
    }

    abstract protected ExplosionHealingMode getHealingMode();

    @Override
    public final void tick(MinecraftServer server) {
        this.tickAndReport(server);
    }

    @Override
    public final TickResult tickAndReport(MinecraftServer server) {
        if (this.isFinished()) {
            return TickResult.WAITING;
        }
        this.healTimer--;
        if (healTimer >= 0) {
            return TickResult.WAITING;
        }
        Optional<AffectedBlock> optionalAffectedBlock = this.getCurrentAffectedBlock();
        if (optionalAffectedBlock.isEmpty()) {
            this.finished = true;
            return TickResult.WAITING;
        }
        AffectedBlock currentAffectedBlock = optionalAffectedBlock.get();
        if (currentAffectedBlock.isPlaced()) {
            this.incrementCounter();
            return TickResult.BLOCK_PLACED;
        }
        if (!this.canHealNow(this.getWorld(server))) {
            return TickResult.WAITING;
        }
        boolean started = !this.healingStarted;
        this.healingStarted = true;
        long timerBeforeTick = currentAffectedBlock.getBlockTimer();
        currentAffectedBlock.tick(this, server);
        if (currentAffectedBlock.isPlaced()) {
            this.incrementCounter();
            return TickResult.BLOCK_PLACED;
        } else if (timerBeforeTick <= 0 && this.blockCounter < this.affectedBlocks.size() - 1) {
            this.affectedBlocks.remove(this.blockCounter);
            this.affectedBlocks.add(currentAffectedBlock);
            return TickResult.RETRY_SCHEDULED;
        }
        return started ? TickResult.HEALING_STARTED : TickResult.WAITING;
    }

    @Override
    public SerializedExplosionEvent asSerialized() {
        return new DefaultSerializedExplosion(this.getHealingMode().getSerializedName(), this.getAffectedBlocks().map(AffectedBlock::asSerialized).toList(), this.healTimer, this.blockCounter, this.radius, this.center);
    }

    public final void findAndMarkPlaced(BlockPos blockPos, BlockState blockState, Level world) {
        for (AffectedBlock affectedBlock : this.affectedBlocks) {
            if (affectedBlock.getBlockState().equals(blockState) && affectedBlock.getBlockPos().equals(blockPos) && affectedBlock.getWorldRegistryKey().equals(world.dimension())) {
                affectedBlock.setPlaced();
            }
        }
    }

}
