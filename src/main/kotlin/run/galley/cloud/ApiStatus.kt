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
  val VESSEL_ENGINE_NODE_NOT_FOUND = ApiStatus(2301, "The node is not found", HttpStatus.NotFound)
  val VESSEL_ENGINE_NODE_DELETION_FAILED = ApiStatus(2302, "The node could not be deleted", HttpStatus.BadRequest)

  val VESSEL_ENGINE_REGION_ID_INCORRECT = ApiStatus(2400, "The vessel region id is incorrect")
  val VESSEL_ENGINE_REGION_NOT_FOUND = ApiStatus(2401, "The vessel region is not found", HttpStatus.NotFound)
  val VESSEL_ENGINE_REGION_DELETE_FAILURE_ACTIVE_NODES =
    ApiStatus(2402, "The region could not be deleted, due to active nodes", HttpStatus.Conflict)
  val VESSEL_ENGINE_REGION_DELETION_FAILED = ApiStatus(2403, "The region could not be deleted", HttpStatus.BadRequest)

  val COMPUTE_PLAN_NOT_FOUND = ApiStatus(2500, "The compute plan could not be found", HttpStatus.NotFound)
  val COMPUTE_PLAN_CREATE_FAILURE = ApiStatus(2501, "The compute plan could not be created")
  val COMPUTE_PLAN_ID_INCORRECT = ApiStatus(2502, "The compute plan id is incorrect")

  val OAUTH_PROVIDER_MISSING = ApiStatus(2601, "The oauth provider is missing")
  val OAUTH_TYPE_MISSING = ApiStatus(2602, "The oauth type is missing")
  val OAUTH_STATUS_MISSING = ApiStatus(2603, "The oauth status is missing")
  val OAUTH_CONNECTION_CREATE_FAILURE = ApiStatus(2604, "The oauth connection could not be created")
  val OAUTH_CONNECTION_APPROVAL_FAILURE = ApiStatus(2605, "The oauth connection could not be approved", HttpStatus.BadRequest)
  val OAUTH_CODE_MISSING = ApiStatus(2606, "The oauth authorization code is missing")
  val OAUTH_STATE_MISSING = ApiStatus(2607, "The oauth state is missing")
  val OAUTH_CONNECTION_NOT_FOUND = ApiStatus(2608, "The oauth connection could not be found", HttpStatus.NotFound)
  val OAUTH_CONNECTION_INACTIVE = ApiStatus(2609, "The oauth connection is not active", HttpStatus.BadRequest)
  val OAUTH_CREDENTIALS_NOT_FOUND = ApiStatus(2610, "The oauth credentials could not be found", HttpStatus.NotFound)
  val OAUTH_PROVIDER_API_ERROR = ApiStatus(2611, "Failed to fetch data from the OAuth provider", HttpStatus.BadGateway)
  val OAUTH_CONNECTION_TYPE_NOT_GIT = ApiStatus(2612, "The oauth connection is not of type 'git'", HttpStatus.BadRequest)
  val OAUTH_CONNECTION_TYPE_NOT_REGISTRY = ApiStatus(2613, "The oauth connection is not of type 'registry'", HttpStatus.BadRequest)
  val OAUTH_TYPE_MISMATCH = ApiStatus(2614, "The oauth connection is of incorrect type", HttpStatus.BadRequest)
  val OAUTH_CONFIG_MISSING = ApiStatus(2615, "OAuth configuration is missing in the config file", HttpStatus.ServiceUnavailable)
  val OAUTH_PROVIDER_CONFIG_MISSING =
    ApiStatus(
      2616,
      "OAuth provider configuration is missing. Please add the provider configuration to the config file.",
      HttpStatus.ServiceUnavailable,
    )
  val OAUTH_CLIENT_ID_MISSING = ApiStatus(2617, "OAuth client_id is missing in the provider configuration", HttpStatus.ServiceUnavailable)
  val OAUTH_CLIENT_SECRET_MISSING =
    ApiStatus(2618, "OAuth client_secret is missing in the provider configuration", HttpStatus.ServiceUnavailable)
}
