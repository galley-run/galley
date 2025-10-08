package run.galley.cloud.db

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.flywaydb.core.Flyway

class FlywayMigrationVerticle : CoroutineVerticle() {
  override suspend fun start() {
    super.start()

    val jdbcUrl =
      "jdbc:postgresql://${config.getString("host")}:${config.getInteger("port")}/${config.getString("database")}"
    val dbUsername = config.getString("username", "galley")
    val dbPassword = config.getString("password", "")

    val flyway =
      Flyway
        .configure()
        .dataSource(jdbcUrl, dbUsername, dbPassword)
        .load()

    // Start the migration
    flyway.migrate()
  }
}
