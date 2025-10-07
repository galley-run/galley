package nl.clicqo.web

enum class HttpStatus(val code: Int, val statusMessage: String) {
  Ok(200, "OK"),
  Created(201, "Created"),
  Accepted(202, "Accepted"),
  NoContent(204, "No Content"),
  NeedsRefresh(210, "Needs Refresh"),
  Found(302, "Found"),
  NotModified(304, "Not Modified"),
  MovedTemporary(307, "Moved temporary"),
  BadRequest(400, "Bad Request"),
  Unauthorized(401, "Unauthorized"),
  PaymentRequired(402, "Payment Required"),
  Forbidden(403, "Forbidden"),
  NotFound(404, "Not Found"),
  MethodNotAllowed(405, "Method Not Allowed"),
  NotAcceptable(406, "Not Acceptable"),
  Conflict(409, "Conflict"),
  Gone(410, "Gone"),
  PreconditionFailed(412, "Precondition Failed"),
  UnsupportedMediaType(415, "Unsupported Media Type"),
  Teapot(418, "I'm a teapot"),

  // Custom Status
  NeedsUpdate(419, "Needs update"),

  // Custom Status
  AuthenticationVerificationFailed(420, "Authentication Verification Failed"),
  Locked(423, "Locked"),
  InternalServerError(500, "Internal Server Error"),
  NotImplemented(501, "Not Implemented"),
  BadGateway(502, "Bad Gateway"),
  ServiceUnavailable(503, "Service Unavailable"),
  GatewayTimeout(504, "Gateway Timeout"),
  ;

  init {
    if (code < 100 || code > 999) {
      throw IllegalArgumentException(
        "code: $code (expected: 100-999)",
      )
    }

    for (i in 0 until statusMessage.length) {
      val c = statusMessage[i]
      // Check prohibited characters.
      when (c) {
        '\n', '\r' -> throw IllegalArgumentException(
          "statusMessage contains one of the following prohibited characters: " +
            "\\r\\n: " + statusMessage,
        )
      }
    }
  }
}
