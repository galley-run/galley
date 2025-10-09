package nl.clicqo.api

import nl.clicqo.web.HttpStatus

data class ApiResponseOptions(
  var contentType: String = "application/json",
  var httpStatus: HttpStatus = HttpStatus.NoContent,
)
