package run.galley.cloud.model

import io.vertx.ext.auth.User

enum class UserRole {
  VESSEL_CAPTAIN,
  CHARTER_CAPTAIN,
  CHARTER_BOATSWAIN,
  CHARTER_PURSER,
  CHARTER_STEWARD,
  CHARTER_DECKHAND,
}

fun User.getUserRole(): UserRole? = principal().getString("scope")?.let(UserRole::valueOf)
