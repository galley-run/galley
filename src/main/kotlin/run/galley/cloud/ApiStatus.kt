package run.galley.cloud

import nl.clicqo.api.ApiStatus
import nl.clicqo.web.HttpStatus

object ApiStatus {
  val ID_MISSING = ApiStatus(1000, "The identifier is missing")
  val FILTER_MISSING = ApiStatus(1000, "The filter is missing")

  val USER_ROLE_FORBIDDEN =
    ApiStatus(1100, "You don't have the required role to perform this action", HttpStatus.Forbidden)

  val VESSEL_NOT_FOUND = ApiStatus(1200, "The vessel could not be found")
  val VESSEL_INSERT_FAILED =
    ApiStatus(1201, "The vessel could not be inserted")
  val VESSEL_ID_INCORRECT = ApiStatus(1202, "The vessel id is incorrect")

  val REFRESH_TOKEN_INVALID =
    ApiStatus(1301, "The refresh token is invalid or expired", HttpStatus.Unauthorized)
  val REFRESH_TOKEN_MISSING = ApiStatus(1302, REFRESH_TOKEN_INVALID)

  val CHARTER_NO_ACCESS = ApiStatus(1400, "You don't have access to this charter")

  val USER_NOT_FOUND = ApiStatus(1500, "The user could not be found")

  val CREW_NO_VESSEL_MEMBER = ApiStatus(1600, "The user is not a member of this Vessel crew")
}
