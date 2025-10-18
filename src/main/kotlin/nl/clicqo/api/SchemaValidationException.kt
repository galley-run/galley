package nl.clicqo.api

class SchemaValidationException : ApiStatusReplyException {
  override lateinit var apiStatus: ApiStatus

  constructor(field: String, message: String) : super(
    apiStatus = ApiStatus.FAILED_VALIDATION,
    message = message,
    sourcePointer = "/data/attributes/$field",
  )
}
