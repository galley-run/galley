package nl.clicqo.api

import nl.clicqo.web.HttpStatus

open class ApiStatus: Throwable {
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
        FAILED
      )
    val UUID_PARSE_EXCEPTION = ApiStatus(103, "A UUID couldn't be processed")
    val DATE_PARSE_EXCEPTION = ApiStatus(104, "A local date string couldn't be processed")
    val FAILED_FIND = ApiStatus(110, "The requested resource could not be found", HttpStatus.NotFound)
    val FAILED_VALIDATION = ApiStatus(111, "The resource failed on validation", HttpStatus.BadRequest)
    val FAILED_INSERT = ApiStatus(112, "Inserting the resource failed")
    val FAILED_UPDATE = ApiStatus(113, "Updating the resource failed")
    val FAILED_DELETE = ApiStatus(114, "Deleting the resource failed")
    val FAILED_AUTHORIZATION = ApiStatus(115, "Authorization failed", HttpStatus.Unauthorized)
  }
}
