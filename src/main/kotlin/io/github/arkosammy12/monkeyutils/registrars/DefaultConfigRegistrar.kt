package io.github.arkosammy12.monkeyutils.registrars

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arkosammy12.monkeyconfig.base.ConfigElement
import io.github.arkosammy12.monkeyconfig.base.ConfigElementContainer
import io.github.arkosammy12.monkeyconfig.base.ConfigManager
import io.github.arkosammy12.monkeyconfig.base.Section
import io.github.arkosammy12.monkeyutils.commands.CommandVisitor
import io.github.arkosammy12.monkeyutils.commands.DefaultCommandVisitor
import io.github.arkosammy12.monkeyutils.settings.CommandControllable
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands as CommandManager
import net.minecraft.commands.CommandSourceStack as ServerCommandSource
import net.minecraft.network.chat.Component as Text

object DefaultConfigRegistrar : ConfigManagerRegistrar {

    private val configManagers: MutableCollection<ConfigManager> = mutableListOf()

    override fun registerConfigManager(configManager: ConfigManager) {
        this.configManagers.add(configManager)
        CommandRegistrationCallback.EVENT.register { commandDispatcher, _, _ ->
            val commandVisitor: CommandVisitor = DefaultCommandVisitor(configManager, commandDispatcher = commandDispatcher)
            registerCommandLayer(configManager = configManager, commandDispatcher = commandDispatcher, visitor = commandVisitor)
        }
    }

    private fun registerCommandLayer(currentContainer: ConfigElementContainer? = null, parentNode: LiteralCommandNode<ServerCommandSource>? = null, configManager: ConfigManager, visitor: CommandVisitor, commandDispatcher: CommandDispatcher<ServerCommandSource>) {
        val layerElements: Collection<ConfigElement> =  currentContainer?.configElements ?: configManager.configElements
        val currentContainerNode: LiteralCommandNode<ServerCommandSource>? = if (currentContainer !is Section) null else CommandManager
            .literal(currentContainer.name)
            .requires(Permissions.require("${configManager.fileName}.config", 4))
            .also { argumentBuilder ->
                currentContainer.comment?.let { comment ->
                    argumentBuilder.executes { ctx ->
                        ctx.source.sendSystemMessage(Text.literal("Description: $comment"))
                        Command.SINGLE_SUCCESS
                    }
                }
            }
            .build()
        var sectionContainsCommandSetting = false
        for (element: ConfigElement in layerElements) {
            if (element !is CommandControllable<*, *>) {
                continue
            }
            sectionContainsCommandSetting = true
            element.accept(currentContainerNode, visitor)
        }
        if (sectionContainsCommandSetting) {
            when {
                currentContainerNode != null && parentNode == null -> visitor.configNode.addChild(currentContainerNode)
                currentContainerNode != null && parentNode != null -> parentNode.addChild(currentContainerNode)
            }
        }
        for (element: ConfigElement in layerElements) {
            if (element !is ConfigElementContainer) {
                continue
            }
            registerCommandLayer(element, currentContainerNode, configManager, visitor, commandDispatcher)
        }
    }

    override fun reloadConfigManagers(onReloadedCallback: ConfigManager.() -> Unit) {
        for (configManager : ConfigManager in this.configManagers) {
            configManager.loadFromFile()
            onReloadedCallback(configManager)
        }
    }

    override fun saveConfigManagers(onSavedCallback: ConfigManager.() -> Unit) {
        for (configManager : ConfigManager in this.configManagers) {
            configManager.saveToFile()
            onSavedCallback(configManager)
        }
    }

    override fun forEachConfigManager(action: ConfigManager.() -> Unit) {
        for (configManager : ConfigManager in this.configManagers) {
            action(configManager)
        }
    }

}
