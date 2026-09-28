package io.github.arkosammy12.creeperhealing.managers;

final class ExplosionSaveSchedule {

    static final int PROGRESS_INTERVAL_TICKS = 200;
    static final int CHECKPOINT_INTERVAL_TICKS = 1200;

    private int ticksSinceSave;
    private int activeTicksSinceSave;
    private boolean dirty;
    private boolean progressDirty;

    void markUrgent() {
        this.dirty = true;
        this.ticksSinceSave = Math.max(this.ticksSinceSave, PROGRESS_INTERVAL_TICKS - 1);
    }

    void markProgress() {
        this.progressDirty = true;
    }

    boolean tick(boolean active, boolean hasEvents) {
        this.ticksSinceSave = Math.min(this.ticksSinceSave + 1, CHECKPOINT_INTERVAL_TICKS);
        if (active && hasEvents) {
            this.activeTicksSinceSave = Math.min(this.activeTicksSinceSave + 1, CHECKPOINT_INTERVAL_TICKS);
        }
        return this.ticksSinceSave >= PROGRESS_INTERVAL_TICKS
                && (this.dirty || this.progressDirty
                || hasEvents && this.activeTicksSinceSave >= CHECKPOINT_INTERVAL_TICKS);
    }

    void saved() {
        this.ticksSinceSave = 0;
        this.activeTicksSinceSave = 0;
        this.dirty = false;
        this.progressDirty = false;
    }

    void saveFailed() {
        this.ticksSinceSave = 0;
    }
}
