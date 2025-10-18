package run.galley.cloud.data

import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import nl.clicqo.ext.CoroutineEventBusSupport

open class PostgresDataVerticle :
  CoroutineVerticle(),
  CoroutineEventBusSupport {
  protected lateinit var pool: Pool

  override suspend fun start() {
    super.start()

    val dbConfig = config.getJsonObject("db")

    // Connection options
    val connectOptions =
      PgConnectOptions()
        .setPort(dbConfig.getInteger("port", 5432))
        .setHost(dbConfig.getString("host", "localhost"))
        .setDatabase(dbConfig.getString("database", "galley"))
        .setUser(dbConfig.getString("username", "galley"))
        .setPassword(dbConfig.getString("password", ""))

    // Pool options
    val poolOptions =
      PoolOptions()
        .setMaxSize(dbConfig.getInteger("pool_size", 5))
        .setShared(true)
        .setName("data-pool")
        .setEventLoopSize(dbConfig.getInteger("event_loop_size", 4))

    // Create the pool
    pool =
      PgBuilder
        .pool()
        .with(poolOptions)
        .connectingTo(connectOptions)
        .using(vertx)
        .build()
  }
}
