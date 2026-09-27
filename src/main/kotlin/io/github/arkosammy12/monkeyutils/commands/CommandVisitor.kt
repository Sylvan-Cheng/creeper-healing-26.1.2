package io.github.arkosammy12.monkeyutils.commands

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arkosammy12.monkeyconfig.base.ConfigManager
import io.github.arkosammy12.monkeyutils.settings.CommandControllable
import net.minecraft.commands.CommandSourceStack as ServerCommandSource

interface CommandVisitor {

    val configNode: LiteralCommandNode<ServerCommandSource>

    val onConfigReloadedCallback: (CommandContext<ServerCommandSource>, ConfigManager) -> Int

    fun <V : Any, A : ArgumentType<*>> visit(parentNode: LiteralCommandNode<ServerCommandSource>?, commandControllableSetting: CommandControllable<V, A>)

}