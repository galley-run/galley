package run.galley.cloud.db

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.flywaydb.core.Flyway

class FlywayMigrationVerticle : CoroutineVerticle() {
  override suspend fun start() {
    super.start()

    val dbConfig = config.getJsonObject("db")
    val jdbcUrl =
      "jdbc:postgresql://${config.getString("host")}:${config.getInteger("port")}/${config.getString("database")}"
    val dbUsername = dbConfig.getString("username", "galley")
    val dbPassword = dbConfig.getString("password", "")

    val flyway = Flyway.configure()
      .dataSource(jdbcUrl, dbUsername, dbPassword)
      .load()

    // Start the migration
    flyway.migrate()
  }
}
