package pl.touk.nussknacker.ui.config

import java.net.URI

import com.typesafe.config.Config
import net.ceedubs.ficus.readers.ValueReader
import pl.touk.nussknacker.ui.config.AnalyticsConfig.AnalyticsEngine.AnalyticsEngine

case class AnalyticsConfig(engine: AnalyticsEngine, url: URI, siteId: String)

object AnalyticsConfig {
  import net.ceedubs.ficus.Ficus._
  import net.ceedubs.ficus.readers.EnumerationReader._
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._

  implicit val uriValueReader: ValueReader[URI] = new ValueReader[URI] {
    def read(config: Config, path: String): URI = new URI(config.getString(path))
  }

  val analyticsConfigNamespace = "analytics"

  object AnalyticsEngine extends Enumeration {
    type AnalyticsEngine = Value

    val Matomo = Value("Matomo")
  }

  def apply(config: Config): Option[AnalyticsConfig] = config.as[Option[AnalyticsConfig]](analyticsConfigNamespace)
}
