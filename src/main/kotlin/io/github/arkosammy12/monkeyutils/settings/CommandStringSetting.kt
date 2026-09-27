package io.github.arkosammy12.monkeyutils.settings

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.github.arkosammy12.monkeyconfig.builders.StringSettingBuilder
import io.github.arkosammy12.monkeyconfig.settings.StringSetting
import io.github.arkosammy12.monkeyconfig.util.ElementPath
import net.minecraft.commands.CommandSourceStack as ServerCommandSource

open class CommandStringSetting(
    settingBuilder: StringSettingBuilder
) : StringSetting(settingBuilder), CommandControllable<String, StringArgumentType>{

    override val argumentType: StringArgumentType
        get() = StringArgumentType.string()

    override val commandPath: ElementPath
        get() = this.path

    override fun getArgumentValue(ctx: CommandContext<out ServerCommandSource>, argumentName: String): String {
        return StringArgumentType.getString(ctx, argumentName)
    }

}