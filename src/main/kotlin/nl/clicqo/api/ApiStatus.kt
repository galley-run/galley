package nl.clicqo.api

import nl.clicqo.web.HttpStatus

open class ApiStatus : Throwable {
  val code: Int
  override var message: String = ""
  val httpStatus: HttpStatus

  /**
   * Create an [ApiStatus] object with unique number and Error Message
   *
   * @param code Unique Error status number
   * @param message The Error Message
   */
  constructor(code: Int, message: String, httpStatus: HttpStatus = HttpStatus.InternalServerError) {
    this.code = code
    this.message = message
    this.httpStatus = httpStatus
  }

  /**
   * Create an [ApiStatus] object with unique number and copied Error Message
   *
   * @param code Unique Error status number
   * @param extendFrom Pass on an [ApiStatus] object to use its error message
   */
  constructor(code: Int, extendFrom: ApiStatus) {
    this.code = code
    this.message = extendFrom.message
    this.httpStatus = extendFrom.httpStatus
  }

  constructor(extendFrom: ApiStatus, message: String?) {
    this.code = extendFrom.code
    this.message = message ?: extendFrom.message
    this.httpStatus = extendFrom.httpStatus
  }

  constructor(code: Int) {
    this.code = code
    this.message = ""
    this.httpStatus = HttpStatus.InternalServerError
  }

  fun message(vararg values: String): String = String.format(this.message, *values)

  companion object {
    val FAILED = ApiStatus(100, "An unknown error occurred")
    val HTTP_CLASS_CAST_EXCEPTION =
      ApiStatus(
        101,
        "An error occurred while processing the request. The JSON Object contains an incorrect data type.",
        HttpStatus.BadRequest,
      )
    val THROWABLE_EXCEPTION =
      ApiStatus(
        102,
        FAILED,
      )
    val UUID_PARSE_EXCEPTION = ApiStatus(103, "A UUID couldn't be processed", HttpStatus.BadRequest)
    val DATE_PARSE_EXCEPTION = ApiStatus(104, "A local date string couldn't be processed")
    val FAILED_FIND = ApiStatus(110, "The requested resource could not be found", HttpStatus.NotFound)
    val FAILED_VALIDATION = ApiStatus(111, "The resource failed on validation", HttpStatus.BadRequest)
    val FAILED_INSERT = ApiStatus(112, "Inserting the resource failed")
    val FAILED_UPDATE = ApiStatus(113, "Updating the resource failed")
    val FAILED_DELETE = ApiStatus(114, "Deleting the resource failed")
    val FAILED_AUTHORIZATION = ApiStatus(115, "Authorization failed", HttpStatus.Unauthorized)
    val PG_FAILED_CONSTRAINT_DUPLICATE =
      ApiStatus(120, "The request couldn't be processed, it seems it already exists", HttpStatus.Conflict)
    val REQUEST_BODY_NOT_ALLOWED = ApiStatus(130, "The request body should be a Json object", HttpStatus.BadRequest)
    val REQUEST_BODY_MISSING_REQUIRED_FIELDS = ApiStatus(131, "The request body is missing required fields", HttpStatus.BadRequest)
    val CONTENT_TYPE_NOT_DEFINED = ApiStatus(132, "The requested content type is not available", HttpStatus.UnsupportedMediaType)
    val RESPONSE_VALIDATION_FAILED = ApiStatus(140, "The response validation failed", HttpStatus.InternalServerError)
    val MESSAGING_EMAIL_FAILED = ApiStatus(150, "The email couldn't be send")

    @Suppress("ktlint:standard:function-naming")
    fun JOOQ_MISSING_REQUIRED_FIELDS(message: String) = ApiStatus(130, "The following fields are missing in the condition: ($message)")
  }
}
