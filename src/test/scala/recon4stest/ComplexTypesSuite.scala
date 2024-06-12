package recon4stest

import recon4s.{*, given}
import com.typesafe.config.*
import scala.concurrent.duration.FiniteDuration

class ComplexTypesSuite extends munit.FunSuite:

    def config = ConfigFactory.load("test-config")

    test("recon4s should read complex config") {
        val actual = config.as[AppConfig]("app")
        assertEquals(actual.version, "v1.0")
        assertEquals(actual.persistance.strategies.size, 18)
    }
end ComplexTypesSuite

case class Role(privileges: Set[String])

case class User(username: String, password: String)

case class ApiSec(roles: Map[String, Role], users: Map[String, User])

case class SessionConfig(sessionCryptoKey: String, ttl: FiniteDuration)

case class WebConfig(
    session: SessionConfig,
    apiSec: ApiSec,
    metadata: Seq[String]
)

case class PersistanceConfig(
    possibleSessionExpireDelay: FiniteDuration,
    keepNSnapshots: Int,
    stashBufferSize: Int,
    stashMaxDuration: FiniteDuration,
    metadata: Map[String, String],
    responseTimeout: FiniteDuration,
    strategies: Map[String, StrategyConfig]
) derives Configurable

enum Strategy derives Configurable:
    case Allow(enabled: Boolean = true)
    case Restrict(enabled: Boolean = false)

case class StrategyConfig(
    strategy: Strategy,
    allow: Boolean,
    deny: Boolean = false,
    ignore: Boolean = true
) derives Configurable

case class CircuitBreakerConfig(
    maxFailures: Int,
    callTimeout: FiniteDuration,
    resetTimeout: FiniteDuration
) derives Configurable

final case class DestinationHost(
    version: String,
    url: String,
    apiKey: String,
    apiPassword: String,
    timeout: Long
) derives Configurable

final case class Destination(id: String, destination: DestinationHost) derives Configurable

final case class DestinationConfig(
    responseTimeout: FiniteDuration,
    circuitBreaker: CircuitBreakerConfig,
    destinations: List[Destination]
) derives Configurable

final case class CassandraConfig(
    contactPoints: Vector[String],
    keyspace: String,
    replicationFactor: Int,
    fetchSize: Int
) derives Configurable

case class AppConfig(
    version: String,
    web: WebConfig,
    cassandra: CassandraConfig,
    persistance: PersistanceConfig,
    destinations: DestinationConfig
) derives Configurable
