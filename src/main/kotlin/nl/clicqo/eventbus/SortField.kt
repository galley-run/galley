package nl.clicqo.eventbus

data class SortField(
  val field: String,
  val direction: SortDirection = SortDirection.ASC
)
