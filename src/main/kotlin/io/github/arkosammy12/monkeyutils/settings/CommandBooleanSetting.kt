package io.github.arkosammy12.monkeyutils.settings

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import io.github.arkosammy12.monkeyconfig.builders.BooleanSettingBuilder
import io.github.arkosammy12.monkeyconfig.settings.BooleanSetting
import io.github.arkosammy12.monkeyconfig.util.ElementPath
import net.minecraft.commands.CommandSourceStack as ServerCommandSource

open class CommandBooleanSetting(
    settingBuilder: BooleanSettingBuilder,
) : BooleanSetting(settingBuilder), CommandControllable<Boolean, BoolArgumentType> {

    override val argumentType: BoolArgumentType
        get() = BoolArgumentType.bool()

    override val commandPath: ElementPath
        get() = this.path

    override fun getArgumentValue(ctx: CommandContext<out ServerCommandSource>, argumentName: String): Boolean {
        return BoolArgumentType.getBool(ctx, argumentName)
    }

}