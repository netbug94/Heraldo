package com.netbug94.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Modern Kotlin delegate for SLF4J.
 * Usage: private val logger by logger()
 */
fun <T : Any> T.logger(): ReadOnlyProperty<T, Logger> = object : ReadOnlyProperty<T, Logger> {
    private var logger: Logger? = null
    override fun getValue(thisRef: T, property: KProperty<*>): Logger {
        if (logger == null) {
            // Automatically detects the class name
            logger = LoggerFactory.getLogger(thisRef::class.java)
        }
        return logger!!
    }
}