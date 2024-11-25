package io.github.sploonmc.bundler.util

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Logger
import java.io.ByteArrayOutputStream

class LoggerOutputStream(val logger: Logger, val level: Level) : ByteArrayOutputStream() {
    val separator = System.lineSeparator()

    override fun flush() {
        synchronized(this) {
            super.flush()
            val record = toString()
            super.reset()

            if (record.isNotEmpty() && record != separator) {
                logger.log(level, record)
            }
        }
    }
}