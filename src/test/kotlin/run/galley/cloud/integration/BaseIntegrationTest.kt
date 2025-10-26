package run.galley.cloud.integration

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import run.galley.cloud.MainVerticle
import run.galley.cloud.web.JWT
import java.time.Instant

@Testcontainers
@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseIntegrationTest {
  protected lateinit var client: WebClient

  companion object {
    @Container
    @JvmStatic
    val postgresContainer =
      PostgreSQLContainer("postgres:17")
        .withDatabaseName("galley_test")
        .withUsername("test")
        .withPassword("test")
  }

  lateinit var vertx: Vertx
  lateinit var testConfig: JsonObject
  var httpPort: Int = 0
  private var deploymentId: String? = null

  // Connection options
  lateinit var connectOptions: PgConnectOptions

  // Shared PgPool for all tests
  lateinit var pg: Pool

  @BeforeAll
  fun setup() =
    runBlocking {
      // Build connection options for direct SQL in tests
      connectOptions =
        PgConnectOptions()
          .setPort(postgresContainer.getMappedPort(5432))
          .setHost(postgresContainer.host)
          .setDatabase(postgresContainer.databaseName)
          .setUser(postgresContainer.username)
          .setPassword(postgresContainer.password)

      // Run Flyway migrations
      val flyway =
        Flyway
          .configure()
          .dataSource(
            postgresContainer.jdbcUrl,
            postgresContainer.username,
            postgresContainer.password,
          ).locations("filesystem:src/main/resources/db/migration")
          .load()
      flyway.migrate()

      // Create Vert.x instance
      vertx = Vertx.vertx()

      // Shared PgPool for all tests
      pg =
        PgBuilder
          .pool()
          .connectingTo(connectOptions)
          .using(vertx)
          .with(PoolOptions().setMaxSize(20))
          .build()

      // Choose random HTTP port for tests
      httpPort = "4${Instant.now().toEpochMilli().toString().substring(9)}".toInt()

      // Create test config with Testcontainers PostgreSQL
      testConfig =
        JsonObject()
          .put("http", JsonObject().put("port", httpPort))
          .put(
            "jwt",
            JsonObject()
              .put("secret", "test-secret-key-for-testing-only-not-production")
              .put("keystore", "test.keystore.p12")
              .put("type", "pkcs12")
              .put("pepper", "test-pepper-key-for-testing-only-not-production")
              .put("ttl", JsonObject().put("access", 90)),
          ).put(
            "api",
            JsonObject()
              .put("openApiFile", "openapi.yaml")
              .put("host", "localhost")
              .put("cors", listOf(".*")),
          ).put(
            "webapp",
            JsonObject()
              .put("host", "localhost")
              .put("cors", listOf(".*")),
          ).put(
            "db",
            JsonObject()
              .put("port", postgresContainer.getMappedPort(5432))
              .put("database", postgresContainer.databaseName)
              .put("host", postgresContainer.host)
              .put("pool_size", 5)
              .put("username", postgresContainer.username)
              .put("password", postgresContainer.password),
          )

      // Deploy MainVerticle using coroutines
      val deployOptions =
        DeploymentOptions()
          .setConfig(testConfig)

      deploymentId = vertx.deployVerticle(MainVerticle(), deployOptions).coAwait()
    }

  @BeforeEach
  open fun setupEach() {
    client = WebClient.create(vertx, WebClientOptions().setDefaultPort(httpPort).setDefaultHost("localhost"))
  }

  @AfterAll
  fun teardown() =
    runTest {
      // Undeploy and cleanup
      deploymentId?.let { id ->
        vertx.undeploy(id).coAwait()
      }
      // Close shared pool
      pg.close().coAwait()
      vertx.close().coAwait()
    }

  fun getJWTAuth() =
    JWT
      .authProvider(vertx, testConfig)
}
