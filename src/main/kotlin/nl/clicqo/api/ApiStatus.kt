package nl.clicqo.api

import nl.clicqo.web.HttpStatus

class ApiStatus {
  val code: Int
  var message: String = ""
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
    val FAILED = ApiStatus(1000, "An unknown error occurred")
    val HTTP_CLASS_CAST_EXCEPTION =
      ApiStatus(
        1001,
        "An error occurred while processing the request. The JSON Object contains an incorrect data type.",
        HttpStatus.BadRequest,
      )
    val THROWABLE_EXCEPTION =
      ApiStatus(
        1002,
        FAILED
      )
    val UUID_PARSE_EXCEPTION = ApiStatus(1003, "A UUID couldn't be processed")
    val DATE_PARSE_EXCEPTION = ApiStatus(1004, "A local date string couldn't be processed")
    val FAILED_FIND = ApiStatus(1100, "The requested resource could not be found")
    val FAILED_VALIDATION = ApiStatus(1101, "The resource failed on validation")
    val FAILED_INSERT = ApiStatus(1102, "Inserting the resource failed")
    val FAILED_UPDATE = ApiStatus(1103, "Updating the resource failed")
    val FAILED_DELETE = ApiStatus(1104, "Deleting the resource failed")
    val FAILED_AUTHENTICATION = ApiStatus(1104, FAILED)
  }
}
