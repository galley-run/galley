package nl.clicqo.ext

import io.vertx.ext.auth.User

fun User.getScope(): String? = principal().getString("scope")
