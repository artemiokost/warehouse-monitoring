package app.common.config

import com.typesafe.config.Config

fun Config.getNonBlankString(path: String): String {
    val value = getString(path)
    require(value.isNotBlank()) {
        "$path is blank: an environment override set to an empty string still counts as set"
    }
    return value
}
