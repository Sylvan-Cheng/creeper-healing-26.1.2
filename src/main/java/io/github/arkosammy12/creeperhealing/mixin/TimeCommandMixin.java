package io.github.arkosammy12.creeperhealing.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import io.github.arkosammy12.creeperhealing.util.callbacks.TimeCommandCallbacks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TimeCommand;

@Mixin(TimeCommand.class)
public abstract class TimeCommandMixin {

    @ModifyReturnValue(method = "addTime", at = @At("RETURN"))
    private static int onTimeAdd(int original, CommandSourceStack serverCommandSource, int time) {
        TimeCommandCallbacks.ON_TIME_EXECUTE_ADD.invoker().onTimeExecuteAdd(serverCommandSource, time, original);
        return original;
    }

    @ModifyReturnValue(method = "setTime", at = @At("RETURN"))
    private static int onTimeSet(int original, CommandSourceStack serverCommandSource, int time) {
        TimeCommandCallbacks.ON_TIME_EXECUTE_SET.invoker().onTimeExecuteSet(serverCommandSource, time, original);
        return original;
    }

}
