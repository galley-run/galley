package run.galley.cloud

import nl.clicqo.api.ApiStatus
import nl.clicqo.web.HttpStatus

object ApiStatus {
  val USER_ROLE_FORBIDDEN =
    ApiStatus(1100, "You don't have the required role to perform this action", HttpStatus.Forbidden)
  val VESSEL_INSERT_FAILED =
    ApiStatus(1201, "The vessel could not be inserted")
}
