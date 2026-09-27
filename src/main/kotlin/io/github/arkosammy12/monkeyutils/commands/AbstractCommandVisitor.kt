package io.github.arkosammy12.monkeyutils.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arkosammy12.monkeyconfig.base.ConfigManager
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.Commands as CommandManager
import net.minecraft.commands.CommandSourceStack as ServerCommandSource
import net.minecraft.network.chat.Component as Text
import net.minecraft.ChatFormatting as Formatting

abstract class AbstractCommandVisitor(
    protected val configManager: ConfigManager,
    protected val rootNodeName: String = configManager.fileName,
    protected val commandDispatcher: CommandDispatcher<ServerCommandSource>
) : CommandVisitor {

    final override val configNode: LiteralCommandNode<ServerCommandSource> = CommandManager
        .literal("config")
        .requires(Permissions.require("$rootNodeName.config", 4))
        .build()

    override val onConfigReloadedCallback: (CommandContext<ServerCommandSource>, ConfigManager) -> Int
        get() = get@{ ctx, configManager ->
            if (configManager.loadFromFile()) {
                ctx.source.sendSystemMessage(Text.literal("Config \"${this.rootNodeName}\" reloaded successfully!").withStyle(Formatting.GREEN))
            } else {
                ctx.source.sendSystemMessage(Text.literal("Found no existing configuration file \"${this.rootNodeName} to reload from!").withStyle(Formatting.RED))
            }
            return@get Command.SINGLE_SUCCESS
        }

    init {

        val reloadNode: LiteralCommandNode<ServerCommandSource> = CommandManager
            .literal("reload")
            .requires(Permissions.require("$rootNodeName.config.reload", 4))
            .executes { ctx -> onConfigReloadedCallback(ctx, configManager) }
            .build()

        // Check if the root node already exists. If it does, use it. Otherwise, create it and add it as a child to the dispatcher root
        val rootNode: CommandNode<ServerCommandSource> = commandDispatcher.root.getChild(rootNodeName) ?: run{
            val node: LiteralCommandNode<ServerCommandSource> = CommandManager
                .literal(rootNodeName)
                .requires(Permissions.require("$rootNodeName.config", 4))
                .build()
            commandDispatcher.root.addChild(node)
            node
        }

        rootNode.addChild(configNode)
        configNode.addChild(reloadNode)

    }

}
