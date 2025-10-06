package nl.clicqo.eventbus

data class Pagination(
  val offset: Int = 0,
  val limit: Int = 50
) {
  companion object {
    const val DEFAULT_LIMIT = 50
    const val MAX_LIMIT = 1000
  }
}
