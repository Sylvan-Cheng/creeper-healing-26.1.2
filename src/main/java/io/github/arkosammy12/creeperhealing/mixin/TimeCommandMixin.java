package io.github.arkosammy12.creeperhealing.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import io.github.arkosammy12.creeperhealing.util.callbacks.TimeCommandCallbacks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

@Mixin(TimeCommand.class)
public abstract class TimeCommandMixin {

    @ModifyReturnValue(method = "addTime", at = @At("RETURN"))
    private static int onTimeAdd(int original, CommandSourceStack serverCommandSource, Holder<WorldClock> clock, int time) {
        if (clock.is(WorldClocks.OVERWORLD)) {
            TimeCommandCallbacks.ON_TIME_EXECUTE_ADD.invoker().onTimeExecuteAdd(serverCommandSource, time, original);
        }
        return original;
    }

    @ModifyReturnValue(method = "setTotalTicks", at = @At("RETURN"))
    private static int onTimeSet(int original, CommandSourceStack serverCommandSource, Holder<WorldClock> clock, int time) {
        if (clock.is(WorldClocks.OVERWORLD)) {
            TimeCommandCallbacks.ON_TIME_EXECUTE_SET.invoker().onTimeExecuteSet(serverCommandSource, time, original);
        }
        return original;
    }

    @ModifyReturnValue(method = "setTimeToTimeMarker", at = @At("RETURN"))
    private static int onTimeSetToMarker(int original, CommandSourceStack serverCommandSource, Holder<WorldClock> clock, ResourceKey<ClockTimeMarker> marker) {
        if (clock.is(WorldClocks.OVERWORLD)) {
            TimeCommandCallbacks.ON_TIME_EXECUTE_SET.invoker().onTimeExecuteSet(serverCommandSource, original, original);
        }
        return original;
    }

}
