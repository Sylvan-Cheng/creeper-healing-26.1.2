package io.github.arkosammy12.monkeyutils.settings

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.context.CommandContext
import io.github.arkosammy12.monkeyconfig.builders.NumberSettingBuilder
import io.github.arkosammy12.monkeyconfig.settings.NumberSetting
import io.github.arkosammy12.monkeyconfig.util.ElementPath
import net.minecraft.commands.CommandSourceStack as ServerCommandSource
import kotlin.math.max
import kotlin.math.min

open class CommandNumberSetting<T : Number> (
    settingBuilder: NumberSettingBuilder<T>
) : NumberSetting<T>(settingBuilder), CommandControllable<T, ArgumentType<T>> {

    override val argumentType: ArgumentType<T>
        @Throws(IllegalArgumentException::class)
        get() = when (this.value.default) {
            is Byte -> getByteArgumentType()
            is Short -> getShortArgumentType()
            is Int -> getIntegerArgumentType()
            is Long -> getLongArgumentType()
            is Float -> getFloatArgumentType()
            is Double -> getDoubleArgumentType()
            else -> throw IllegalArgumentException("Unsupported number type: ${this.value.default::class.java}")
        }

    override val commandPath: ElementPath
        get() = this.path

    // The following unchecked cast is safe as long as the type of the value retrieved by the "getX"
    // method matches the type parameter of the current NumberSetting.
    // For example, given a NumberSetting<Int>, an IntegerArgumentType should have been supplied to the command setter node
    // and the value of the command argument should be retrieved with IntegerArgumentType.getInteger(), which returns an Int, which can then be safely casted to T,
    // which in this case, is Int.
    @Suppress("UNCHECKED_CAST")
    override fun getArgumentValue(ctx: CommandContext<out ServerCommandSource>, argumentName: String): T =
        when (this.value.default) {
            is Byte, is Short, is Int -> IntegerArgumentType.getInteger(ctx, argumentName)
            is Long -> LongArgumentType.getLong(ctx, argumentName)
            is Float -> FloatArgumentType.getFloat(ctx, argumentName)
            is Double -> DoubleArgumentType.getDouble(ctx, argumentName)
            else -> throw IllegalArgumentException("Unsupported number type: ${this.value.default::class.java}")
        } as T


    // The following unchecked casts are safe if the ArgumentType provided by one of Brigadier's argument types for numerical
    // arguments are subtypes of the corresponding ArgumentType<T>.
    // For example, a NumberSetting<Double> should retrieve its ArgumentType with DoubleArgumentType.doubleArg(), which returns
    // an instance of ArgumentType<Double>. In this case, the type parameter of the setting is Double, so this argument type can be safely casted to
    // ArgumentType<Double>.
    @Suppress("UNCHECKED_CAST")
    private fun getByteArgumentType(): ArgumentType<T> {
        val min = max(Byte.MIN_VALUE.toInt(), this.value.minValue?.toInt() ?: Byte.MIN_VALUE.toInt())
        val max = min(Byte.MAX_VALUE.toInt(), this.value.maxValue?.toInt() ?: Byte.MAX_VALUE.toInt())
        return IntegerArgumentType.integer(min, max) as ArgumentType<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun getShortArgumentType(): ArgumentType<T> {
        val min = max(Short.MIN_VALUE.toInt(), this.value.minValue?.toInt() ?: Short.MIN_VALUE.toInt())
        val max = min(Short.MAX_VALUE.toInt(), this.value.maxValue?.toInt() ?: Short.MAX_VALUE.toInt())
        return IntegerArgumentType.integer(min, max) as ArgumentType<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun getIntegerArgumentType(): ArgumentType<T> {
        val min = max(Int.MIN_VALUE, this.value.minValue?.toInt() ?: Int.MIN_VALUE)
        val max = min(Int.MAX_VALUE, this.value.maxValue?.toInt() ?: Int.MAX_VALUE)
        return IntegerArgumentType.integer(min, max) as ArgumentType<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLongArgumentType(): ArgumentType<T> {
        val min = max(Long.MIN_VALUE, this.value.minValue?.toLong() ?: Long.MIN_VALUE)
        val max = min(Long.MAX_VALUE, this.value.maxValue?.toLong() ?: Long.MAX_VALUE)
        return LongArgumentType.longArg(min, max) as ArgumentType<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun getFloatArgumentType(): ArgumentType<T> {
        val min = max(Float.MIN_VALUE, this.value.minValue?.toFloat() ?: Float.MIN_VALUE)
        val max = min(Float.MAX_VALUE, this.value.maxValue?.toFloat() ?: Float.MAX_VALUE)
        return FloatArgumentType.floatArg(min, max) as ArgumentType<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDoubleArgumentType(): ArgumentType<T> {
        val min = max(Double.MIN_VALUE, this.value.minValue?.toDouble() ?: Double.MIN_VALUE)
        val max = min(Double.MAX_VALUE, this.value.maxValue?.toDouble() ?: Double.MAX_VALUE)
        return DoubleArgumentType.doubleArg(min, max) as ArgumentType<T>
    }

}