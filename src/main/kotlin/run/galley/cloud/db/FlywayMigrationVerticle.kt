package run.galley.cloud.db

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.flywaydb.core.Flyway

class FlywayMigrationVerticle : CoroutineVerticle() {
  override suspend fun start() {
    super.start()

    val dbConfig = config.getJsonObject("db")
    val jdbcUrl = dbConfig.getString("jdbc", "jdbc:postgresql://localhost:5432/galley")
    val dbUsername = dbConfig.getString("username", "galley")
    val dbPassword = dbConfig.getString("password", "")

    val flyway = Flyway.configure()
      .dataSource(jdbcUrl, dbUsername, dbPassword)
      .load()

    // Start the migration
    flyway.migrate()
  }
}
