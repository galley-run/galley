package nl.clicqo.api

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import java.math.RoundingMode

class ApiPagination(
  val page: Int = DEFAULT_PAGE,
  var limit: Int? = null,
  private var total: Int? = 0,
) {
  fun toJsonObject(): JsonObject =
    JsonObject()
      .put("page", page)
      .put("limit", limit ?: DEFAULT_LIMIT)
      .put("total", total)

  fun setLimit(limit: Int): ApiPagination {
    this.limit = limit
    return this
  }

  fun setTotal(total: Int): ApiPagination {
    this.total = total
    return this
  }

  val sqlOffset: Int
    get() {
      return (page - 1) * (limit ?: DEFAULT_LIMIT)
    }

  val previousPage: Int
    get() = (page - 1).run { if (this < 1) 1 else this }

  val totalPages: Int
    get() =
      if ((total ?: 0) < (limit ?: DEFAULT_LIMIT)) {
        1
      } else {
        (total ?: 0).toBigDecimal().setScale(5, RoundingMode.UP).let {
          (it / (limit ?: DEFAULT_LIMIT).toBigDecimal().setScale(5, RoundingMode.UP)).setScale(0, RoundingMode.UP).toInt()
        }
      }

  val isLastPage
    get() = totalPages == page

  val firstRange: List<Int>
    get() = listOf(1, 2, 3).filter { it <= totalPages }

  val lastRange: List<Int>
    get() = listOf(totalPages - 2, totalPages - 1, totalPages).filter { totalPages > 3 && it > 3 }

  val nextPage: Int
    get() = (page + 1).run { if (this > totalPages || this == 0) totalPages else this }

  companion object {
    const val DEFAULT_PAGE = 1
    const val DEFAULT_LIMIT = 15

    fun from(pagination: JsonObject): ApiPagination =
      ApiPagination(
        pagination.getInteger("page", DEFAULT_PAGE),
        pagination.getInteger("limit", null),
        pagination.getInteger("total", null),
      )

    fun from(routingContext: RoutingContext): ApiPagination =
      ApiPagination(
        page =
          try {
            routingContext.request().getParam("page").toInt()
          } catch (e: Exception) {
            DEFAULT_PAGE
          },
        limit =
          try {
            routingContext.request().getParam("limit").toInt()
          } catch (e: Exception) {
            null
          },
      )
  }
}
