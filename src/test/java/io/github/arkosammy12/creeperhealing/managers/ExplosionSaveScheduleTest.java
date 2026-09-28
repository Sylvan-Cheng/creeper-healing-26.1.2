package io.github.arkosammy12.creeperhealing.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionSaveScheduleTest {

    @Test
    void checkpointsLongWaitsEverySixtySecondsOfActiveTicks() {
        ExplosionSaveSchedule schedule = new ExplosionSaveSchedule();
        schedule.markUrgent();
        assertTrue(schedule.tick(true, true));
        schedule.saved();

        for (int i = 1; i < 1200; i++) {
            assertFalse(schedule.tick(true, true));
        }
        assertTrue(schedule.tick(true, true));
        schedule.saved();
        assertFalse(schedule.tick(true, true));
    }

    @Test
    void waitingForDaylightDoesNotCauseShortIntervalWrites() {
        ExplosionSaveSchedule schedule = new ExplosionSaveSchedule();
        for (int i = 0; i < 1199; i++) {
            assertFalse(schedule.tick(true, true));
        }
        assertTrue(schedule.tick(true, true));
    }

    @Test
    void progressSavesSoonAndFailedWritesRetry() {
        ExplosionSaveSchedule schedule = new ExplosionSaveSchedule();
        schedule.markProgress();
        for (int i = 0; i < 199; i++) {
            assertFalse(schedule.tick(true, true));
        }
        assertTrue(schedule.tick(true, true));
        schedule.saveFailed();
        for (int i = 0; i < 199; i++) {
            assertFalse(schedule.tick(true, true));
        }
        assertTrue(schedule.tick(true, true));
        schedule.saved();
        assertFalse(schedule.tick(true, true));
    }

    @Test
    void pausedTicksDoNotAdvanceCheckpoint() {
        ExplosionSaveSchedule schedule = new ExplosionSaveSchedule();
        for (int i = 0; i < 2400; i++) {
            assertFalse(schedule.tick(false, true));
        }
        for (int i = 0; i < 1199; i++) {
            assertFalse(schedule.tick(true, true));
        }
        assertTrue(schedule.tick(true, true));
    }
}
