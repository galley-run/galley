package run.galley.cloud

import nl.clicqo.api.ApiStatus
import nl.clicqo.web.HttpStatus

object ApiStatus {
  val ID_MISSING = ApiStatus(1000, "The identifier is missing")
  val FILTER_MISSING = ApiStatus(1001, "The filter is missing")
  val REQUEST_BODY_MISSING = ApiStatus(1002, "The request body is missing")
  val MISSING_USER_ID = ApiStatus(1003, "The user id is missing")

  val USER_ROLE_FORBIDDEN =
    ApiStatus(1100, "You don't have the required role to perform this action", HttpStatus.Forbidden)

  val VESSEL_NOT_FOUND = ApiStatus(1200, "The vessel could not be found", HttpStatus.NotFound)
  val VESSEL_INSERT_FAILED =
    ApiStatus(1201, "The vessel could not be inserted")
  val VESSEL_ID_INCORRECT = ApiStatus(1202, "The vessel id is incorrect", HttpStatus.NotFound)

  val REFRESH_TOKEN_INVALID =
    ApiStatus(1301, "The refresh token is invalid or expired", HttpStatus.Unauthorized)
  val REFRESH_TOKEN_MISSING = ApiStatus(1302, REFRESH_TOKEN_INVALID)
  val ACCESS_TOKEN_MISSING = ApiStatus(1303, "The access token is invalid or expired", HttpStatus.Unauthorized)

  val CHARTER_NO_ACCESS = ApiStatus(1400, "You don't have access to this charter", HttpStatus.Forbidden)
  val CHARTER_NOT_FOUND = ApiStatus(1401, "The charter could not be found", HttpStatus.NotFound)
  val CHARTER_CREATE_FAILURE = ApiStatus(1402, "The charter could not be created")
  val CHARTER_ID_INCORRECT = ApiStatus(1403, "The charter id is incorrect")
  val CHARTER_DELETE_FAILURE_ACTIVE_PROJECTS =
    ApiStatus(1404, "The charter could not be deleted, due to active projects", HttpStatus.Conflict)

  val USER_NOT_FOUND = ApiStatus(1500, "The user could not be found", HttpStatus.NotFound)

  val CREW_NO_VESSEL_MEMBER = ApiStatus(1600, "The user is not a member of this Vessel crew", HttpStatus.Forbidden)
  val CREW_NO_VESSEL_CAPTAIN = ApiStatus(1601, "The user is not a captain of this Vessel", HttpStatus.Forbidden)
  val CREW_NO_CHARTER_MEMBER = ApiStatus(1602, "The user is not a member of this Charter crew", HttpStatus.Forbidden)
  val CREW_EMBARKING_TOO_OLD =
    ApiStatus(
      1603,
      "You have created your account more than 6 hours ago without accepting your invite by e-mail. Please sign in again.",
      HttpStatus.Unauthorized,
    )
  val CREW_NO_ACTIVATION_SALT = ApiStatus(1604, "You can't activate without an activation key", HttpStatus.NotFound)

  val PROJECT_CREATE_FAILURE = ApiStatus(1700, "The project could not be created")
  val PROJECT_NOT_FOUND = ApiStatus(1701, "The project could not be found", HttpStatus.NotFound)
  val PROJECT_ID_INCORRECT = ApiStatus(1702, "The project id is incorrect")

  val SIGN_UP_INTENT_MISSING = ApiStatus(1800, "The sign up intent is missing")
  val SIGN_UP_USER_OBJ_MISSING = ApiStatus(1801, "The user object is missing")
  val SIGN_UP_INQUIRY_OBJ_MISSING = ApiStatus(1802, "The inquiry object is missing")
  val SIGN_UP_VESSEL_OBJ_MISSING = ApiStatus(1803, "The vessel object is missing")
  val SIGN_UP_CHARTER_OBJ_MISSING = ApiStatus(1804, "The charter object is missing")
  val SIGN_UP_PROJECT_OBJ_MISSING = ApiStatus(1805, "The project object is missing")
  val SIGN_UP_INQUIRY_STORE_FAILED = ApiStatus(1806, "Storing the inquiry failed")

  val VESSEL_BILLING_PROFILE_CREATE_FAILURE = ApiStatus(1900, "The vessel billing profile could not be created")
  val VESSEL_BILLING_PROFILE_NOT_FOUND = ApiStatus(1901, "The vessel billing profile could not be found")

  val SESSION_NOT_FOUND = ApiStatus(2000, "The session could not be found", HttpStatus.NotFound)

  val JWT_PEPPER_MISSING = ApiStatus(2100, "The JWT Pepper is not set in the configuration", HttpStatus.ServiceUnavailable)

  val VESSEL_ENGINE_ID_INCORRECT = ApiStatus(2200, "The Vessel engine id is incorrect")
  val VESSEL_ENGINE_NOT_FOUND = ApiStatus(2201, "The Vessel engine not found", HttpStatus.NotFound)

  val VESSEL_ENGINE_NODE_ID_INCORRECT = ApiStatus(2300, "The Vessel engine node id is incorrect")
  val VESSEL_ENGINE_NODE_NOT_FOUND = ApiStatus(2301, "The Vessel engine node is not found", HttpStatus.NotFound)

  val VESSEL_REGION_ID_INCORRECT = ApiStatus(2400, "The vessel region id is incorrect")
  val VESSEL_REGION_NOT_FOUND = ApiStatus(2401, "The vessel region is not found", HttpStatus.NotFound)
  val VESSEL_REGION_DELETE_FAILURE_ACTIVE_NODES =
    ApiStatus(1404, "The region could not be deleted, due to active nodes", HttpStatus.Conflict)
}
