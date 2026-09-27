package io.github.arkosammy12.monkeyutils

import io.github.arkosammy12.monkeyutils.registrars.DefaultConfigRegistrar
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.LoggerFactory

object MonkeyUtils : ModInitializer {

    private val LOGGER = LoggerFactory.getLogger("monkey-utils")

	override fun onInitialize() {

		ServerLifecycleEvents.SERVER_STOPPING.register {

			DefaultConfigRegistrar.saveConfigManagers()

		}

		LOGGER.info("Thanks to isXander (https://github.com/isXander) for helping me get into Kotlin from Java :D")

	}
}