package io.github.arkosammy12.monkeyutils.registrars

import io.github.arkosammy12.monkeyconfig.base.ConfigManager

interface ConfigManagerRegistrar {

    fun registerConfigManager(configManager: ConfigManager)

    fun reloadConfigManagers(onReloadedCallback: ConfigManager.() -> Unit = {})

    fun saveConfigManagers(onSavedCallback: ConfigManager.() -> Unit = {})

    fun forEachConfigManager(action: ConfigManager.() -> Unit)

}