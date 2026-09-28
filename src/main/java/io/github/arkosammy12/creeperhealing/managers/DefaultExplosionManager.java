package io.github.arkosammy12.creeperhealing.managers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.arkosammy12.creeperhealing.explosions.AbstractExplosionEvent;
import io.github.arkosammy12.creeperhealing.explosions.ExplosionEvent;
import io.github.arkosammy12.creeperhealing.explosions.SerializedExplosionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import io.github.arkosammy12.creeperhealing.CreeperHealing;
import io.github.arkosammy12.creeperhealing.blocks.AffectedBlock;
import io.github.arkosammy12.creeperhealing.blocks.DoubleAffectedBlock;
import io.github.arkosammy12.creeperhealing.blocks.SingleAffectedBlock;
import io.github.arkosammy12.creeperhealing.config.ConfigUtils;
import io.github.arkosammy12.creeperhealing.explosions.factories.DefaultExplosionFactory;
import io.github.arkosammy12.creeperhealing.explosions.factories.ExplosionEventFactory;
import io.github.arkosammy12.creeperhealing.util.ExplosionContext;
import io.github.arkosammy12.creeperhealing.util.ExplosionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultExplosionManager implements ExplosionManager {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(CreeperHealing.MOD_ID, "default_explosion_manager");
    private static final Function<ExplosionContext, ExplosionEventFactory<?>> explosionContextToFactoryFunction = explosionContext -> {
        List<BlockPos> indirectlyExplodedPositions = explosionContext.indirectlyAffectedPositions();
        Map<BlockPos, Tuple<BlockState, BlockEntity>> affectedStatesAndBlockEntities = explosionContext.affectedStatesAndBlockEntities();
        DefaultExplosionFactory explosionFactory = new DefaultExplosionFactory(
                affectedStatesAndBlockEntities,
                explosionContext.vanillaAffectedPositions(),
                indirectlyExplodedPositions,
                explosionContext.world()
        );
        return explosionFactory;
    };
    private static final String SCHEDULED_EXPLOSIONS_FILE = "scheduled-explosions.json";
    private record BlockLocation(ResourceKey<Level> world, BlockPos pos) {}

    private final Codec<DefaultExplosionManager> codec;
    private final List<ExplosionEvent> explosionEvents = new ArrayList<>();
    private final ExplosionSaveSchedule saveSchedule = new ExplosionSaveSchedule();
    private boolean storageWritable = true;

    @Override
    public Identifier getId() {
        return ID;
    }

    public DefaultExplosionManager(Codec<SerializedExplosionEvent> explosionSerializer) {
        this.codec = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(explosionSerializer).fieldOf("scheduled_explosions").forGetter(DefaultExplosionManager::getSerializedExplosionEvents)
        ).apply(instance, (serializedExplosionEvents) -> new DefaultExplosionManager(serializedExplosionEvents, explosionSerializer)));
    }

    private DefaultExplosionManager(List<SerializedExplosionEvent> serializedExplosionEvents, Codec<SerializedExplosionEvent> explosionSerializer) {
        this(explosionSerializer);
        this.explosionEvents.addAll(serializedExplosionEvents.stream().map(SerializedExplosionEvent::asDeserialized).toList());
    }

    private List<SerializedExplosionEvent> getSerializedExplosionEvents() {
        return this.explosionEvents.stream().map(ExplosionEvent::asSerialized).collect(Collectors.toList());
    }

    @Override
    public Stream<ExplosionEvent> getExplosionEvents() {
        return this.explosionEvents.stream();
    }

    @Override
    public void onServerStarting(MinecraftServer server) {
        this.readExplosionEvents(server);
    }

    @Override
    public void onServerStopping(MinecraftServer server) {
        this.storeExplosionEvents(server);
        this.explosionEvents.clear();
        this.saveSchedule.saved();
        this.storageWritable = true;
    }

    @Override
    public Function<ExplosionContext, ExplosionEventFactory<?>> getExplosionContextToEventFactoryFunction() {
        return explosionContextToFactoryFunction;
    }

    @Override
    public void tick(MinecraftServer server) {
        boolean active = server.tickRateManager().runsNormally();
        if (active) {
            for (ExplosionEvent explosionEvent : this.explosionEvents) {
                if (explosionEvent.tickAndReport(server).hasProgress()) {
                    this.saveSchedule.markProgress();
                }
            }
            if (explosionEvents.removeIf(ExplosionEvent::isFinished)) {
                this.saveSchedule.markUrgent();
            }
        }
        if (this.saveSchedule.tick(active, !this.explosionEvents.isEmpty())) {
            this.storeExplosionEvents(server);
        }
    }

    @Override
    public <T extends ExplosionEvent> void addExplosionEvent(ExplosionEventFactory<T> explosionEventFactory) {
        ExplosionEvent explosionEvent = explosionEventFactory.createExplosionEvent();
        if (explosionEvent == null) {
            return;
        }
        Set<ExplosionEvent> collidingExplosions = this.getCollidingExplosions(explosionEvent, explosionEventFactory.getAffectedPositions());
        if (collidingExplosions.isEmpty()) {
            this.explosionEvents.add(explosionEvent);
        } else {
            this.explosionEvents.removeIf(collidingExplosions::contains);
            collidingExplosions.add(explosionEvent);
            this.explosionEvents.add(combineCollidingExplosions(explosionEvent, collidingExplosions, explosionEventFactory));
        }
        this.saveSchedule.markUrgent();

    }

    @Override
    public void storeExplosionEvents(MinecraftServer server) {
        if (!this.storageWritable) {
            CreeperHealing.LOGGER.error("Scheduled explosions were not saved because the unreadable original file could not be preserved");
            this.saveSchedule.saveFailed();
            return;
        }
        Path savedExplosionsFilePath = server.getWorldPath(LevelResource.ROOT).resolve(SCHEDULED_EXPLOSIONS_FILE);
        DataResult<JsonElement> encodedExplosions = this.codec.encodeStart(server.registryAccess().createSerializationContext(JsonOps.COMPRESSED), this);
        if (encodedExplosions.isError()) {
            CreeperHealing.LOGGER.error("Error storing creeper healing explosion(s): No value present!");
            this.saveSchedule.saveFailed();
            return;
        }
        JsonElement encodedExplosionsJson = encodedExplosions.getPartialOrThrow();
        Gson gson = new Gson();
        String jsonString = gson.toJson(encodedExplosionsJson);
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(savedExplosionsFilePath.getParent(), "scheduled-explosions-", ".tmp");
            Files.writeString(temporaryFile, jsonString);
            if (Files.exists(savedExplosionsFilePath) && Files.size(savedExplosionsFilePath) > 0) {
                Files.copy(savedExplosionsFilePath, savedExplosionsFilePath.resolveSibling(SCHEDULED_EXPLOSIONS_FILE + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporaryFile, savedExplosionsFilePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, savedExplosionsFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            this.saveSchedule.saved();
        } catch (Exception e) {
            CreeperHealing.LOGGER.error("Error storing explosion event(s)", e);
            this.saveSchedule.saveFailed();
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    CreeperHealing.LOGGER.warn("Could not remove temporary explosion file {}", temporaryFile, e);
                }
            }
        }
    }

    @Override
    public void readExplosionEvents(MinecraftServer server) {
        Path savedExplosionsFilePath = server.getWorldPath(LevelResource.ROOT).resolve(SCHEDULED_EXPLOSIONS_FILE);
        if (!Files.exists(savedExplosionsFilePath)) {
            this.readBackupIfPresent(server, savedExplosionsFilePath);
            return;
        }
        try {
            if (Files.size(savedExplosionsFilePath) == 0) {
                this.readBackupIfPresent(server, savedExplosionsFilePath);
                return;
            }
            this.readExplosionEventsFromFile(server, savedExplosionsFilePath);
        } catch (Exception e) {
            CreeperHealing.LOGGER.error("Error reading scheduled explosions from {}", savedExplosionsFilePath, e);
            try {
                Path corruptFile = savedExplosionsFilePath.resolveSibling(SCHEDULED_EXPLOSIONS_FILE + ".corrupt-" + System.currentTimeMillis());
                Files.move(savedExplosionsFilePath, corruptFile);
                CreeperHealing.LOGGER.error("Preserved unreadable scheduled explosions at {}", corruptFile);
            } catch (IOException preserveError) {
                this.storageWritable = false;
                CreeperHealing.LOGGER.error("Could not preserve unreadable scheduled explosions", preserveError);
                return;
            }
            this.readBackupIfPresent(server, savedExplosionsFilePath);
        }
    }

    private void readBackupIfPresent(MinecraftServer server, Path primaryFile) {
        Path backupFile = primaryFile.resolveSibling(SCHEDULED_EXPLOSIONS_FILE + ".bak");
        if (!Files.exists(backupFile)) {
            return;
        }
        try {
            this.readExplosionEventsFromFile(server, backupFile);
            this.saveSchedule.markUrgent();
        } catch (Exception e) {
            CreeperHealing.LOGGER.error("Error reading scheduled explosions backup {}", backupFile, e);
        }
    }

    private void readExplosionEventsFromFile(MinecraftServer server, Path file) throws IOException {
        if (Files.size(file) == 0) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            JsonElement json = JsonParser.parseReader(reader);
            DefaultExplosionManager decoded = this.codec.parse(server.registryAccess().createSerializationContext(JsonOps.COMPRESSED), json)
                    .result().orElseThrow(() -> new IOException("Invalid scheduled explosions data"));
            for (ExplosionEvent event : decoded.explosionEvents) {
                int blockCount = (int) event.getAffectedBlocks().count();
                if (blockCount == 0 || event instanceof AbstractExplosionEvent abstractEvent && (abstractEvent.getBlockCounter() < 0 || abstractEvent.getBlockCounter() > blockCount)) {
                    throw new IOException("Invalid scheduled explosion cursor or empty block list");
                }
            }
            this.explosionEvents.addAll(decoded.explosionEvents);
            CreeperHealing.LOGGER.info("Rescheduled {} explosion event(s) from {}", decoded.explosionEvents.size(), file);
        }
    }

    // An explosion collides with another if the square of the distance between their centers is less than or equal to the sum of their radii
    private Set<ExplosionEvent> getCollidingExplosions(ExplosionEvent newExplosionEvent, List<BlockPos> affectedPositions) {
        Set<ExplosionEvent> collidingExplosions = new LinkedHashSet<>();
        BlockPos newExplosionCenter;
        int newExplosionRadius;
        if (newExplosionEvent instanceof AbstractExplosionEvent abstractExplosionEvent) {
            newExplosionCenter = abstractExplosionEvent.getCenter();
            newExplosionRadius = abstractExplosionEvent.getRadius();
        } else {
            newExplosionRadius = ExplosionUtils.getMaxExplosionRadius(affectedPositions);
            newExplosionCenter = ExplosionUtils.calculateCenter(affectedPositions);
        }
        ResourceKey<Level> newWorld = newExplosionEvent.getAffectedBlocks().findFirst().orElseThrow().getWorldRegistryKey();

        for (ExplosionEvent explosionEvent : this.explosionEvents) {
            if (explosionEvent.getAffectedBlocks().anyMatch(block -> !block.getWorldRegistryKey().equals(newWorld))) {
                continue;
            }
            boolean hasStartedHealing = explosionEvent.getHealTimer() <= 0;
            if (hasStartedHealing) {
                continue;
            }
            BlockPos currentExplosionCenter;
            int currentExplosionRadius;
            if (explosionEvent instanceof AbstractExplosionEvent abstractExplosionEvent) {
                currentExplosionCenter = abstractExplosionEvent.getCenter();
                currentExplosionRadius = abstractExplosionEvent.getRadius();
            } else {
                List<BlockPos> currentAffectedPositions = explosionEvent.getAffectedBlocks().map(AffectedBlock::getBlockPos).toList();
                currentExplosionRadius = ExplosionUtils.getMaxExplosionRadius(currentAffectedPositions);
                currentExplosionCenter = ExplosionUtils.calculateCenter(currentAffectedPositions);
            }
            int combinedRadius = newExplosionRadius + currentExplosionRadius;
            double distanceBetweenCenters = Math.floor(Math.sqrt(newExplosionCenter.distSqr(currentExplosionCenter)));
            if (distanceBetweenCenters <= combinedRadius) {
                collidingExplosions.add(explosionEvent);
            }
        }
        return collidingExplosions;
    }

    // Combine the list of affected blocks and use the attributes of the newest explosion as the attributes of the combined explosion
    private ExplosionEvent combineCollidingExplosions(ExplosionEvent newestExplosion, Set<ExplosionEvent> collidingExplosions, ExplosionEventFactory<?> explosionEventFactory) {
        Map<BlockLocation, AffectedBlock> blocksByLocation = new LinkedHashMap<>();
        List<AffectedBlock> combinedBlocks = new ArrayList<>();
        collidingExplosions.stream().flatMap(ExplosionEvent::getAffectedBlocks).forEach(block -> {
            BlockLocation first = new BlockLocation(block.getWorldRegistryKey(), block.getBlockPos());
            BlockLocation second = block instanceof DoubleAffectedBlock doubleBlock
                    ? new BlockLocation(block.getWorldRegistryKey(), doubleBlock.getSecondHalfPos()) : null;
            AffectedBlock firstConflict = blocksByLocation.get(first);
            AffectedBlock secondConflict = second == null ? null : blocksByLocation.get(second);
            if (!(block instanceof DoubleAffectedBlock) && firstConflict instanceof DoubleAffectedBlock) {
                return;
            }
            if (firstConflict != null) {
                removeConflictingBlock(firstConflict, combinedBlocks, blocksByLocation);
            }
            if (secondConflict != null && secondConflict != firstConflict) {
                removeConflictingBlock(secondConflict, combinedBlocks, blocksByLocation);
            }
            combinedBlocks.add(block);
            blocksByLocation.put(first, block);
            if (second != null) {
                blocksByLocation.put(second, block);
            }
        });
        ExplosionEvent combinedExplosionEvent = explosionEventFactory.createExplosionEvent(combinedBlocks, newestExplosion.getHealTimer(), ConfigUtils.getBlockPlacementDelay());
        return combinedExplosionEvent;
    }

    private static void removeConflictingBlock(AffectedBlock conflict, List<AffectedBlock> blocks, Map<BlockLocation, AffectedBlock> byLocation) {
        blocks.removeIf(block -> block == conflict);
        byLocation.remove(new BlockLocation(conflict.getWorldRegistryKey(), conflict.getBlockPos()));
        if (conflict instanceof DoubleAffectedBlock doubleConflict) {
            byLocation.remove(new BlockLocation(conflict.getWorldRegistryKey(), doubleConflict.getSecondHalfPos()));
        }
    }

    public void markProgressDirty() {
        this.saveSchedule.markProgress();
    }

    public void updateAffectedBlocksTimers() {
        for (ExplosionEvent explosionEvent : this.explosionEvents) {
            if (!(explosionEvent instanceof AbstractExplosionEvent abstractExplosionEvent)) {
                continue;
            }
            List<AffectedBlock> affectedBlocks = explosionEvent.getAffectedBlocks().toList();
            for (int i = abstractExplosionEvent.getBlockCounter() + 1; i < affectedBlocks.size(); i++) {
                AffectedBlock currentAffectedBlock = affectedBlocks.get(i);
                if (!(currentAffectedBlock instanceof SingleAffectedBlock singleAffectedBlock)) {
                    continue;
                }
                singleAffectedBlock.setTimer(ConfigUtils.getBlockPlacementDelay());
                this.markProgressDirty();
            }

        }
    }

}
