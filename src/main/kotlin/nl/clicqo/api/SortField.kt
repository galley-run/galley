package nl.clicqo.api

data class SortField(
  val field: String,
  val direction: SortDirection = SortDirection.ASC
)
