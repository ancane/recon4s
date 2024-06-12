package recon4s.reading

import com.typesafe.config.{Config as TypesafeConfig, ConfigValue}
import java.time.*
import java.net.{URL, URI}
import java.nio.file.{Path, Paths}
import java.util.UUID
import java.io.File
import java.util.regex.Pattern
import recon4s.deriving.{Config, Configurable}

private[recon4s] trait JavaTypes:

    inline given Configurable[URL] with
        def get(config: Config, key: String): URL = new URI(config.getString(key)).toURL()

    inline given Configurable[URI] with
        def get(config: Config, key: String): URI = new URI(config.getString(key))

    inline given Configurable[Path] with
        def get(config: Config, key: String): Path = Paths.get(config.getString(key))

    inline given Configurable[UUID] with
        def get(config: Config, key: String): UUID = UUID.fromString(config.getString(key))

    inline given Configurable[File] with
        def get(config: Config, key: String): File = new File(config.getString(key))

    inline given Configurable[Pattern] with
        def get(config: Config, key: String): Pattern = Pattern.compile(config.getString(key))

    inline given Configurable[Period] with
        def get(config: Config, key: String): Period = config.getPeriod(key)

    inline given Configurable[Duration] with
        def get(config: Config, key: String): Duration = config.getDuration(key)

    inline given Configurable[TypesafeConfig] with
        def get(config: Config, key: String): TypesafeConfig = config.getConfig(key).underlying

    inline given Configurable[ConfigValue] with
        def get(config: Config, key: String): ConfigValue = config.getValue(key)
end JavaTypes

trait JavaTime:

    inline given Configurable[Instant] with
        def get(config: Config, key: String): Instant = Instant.parse(config.getString(key))

    inline given Configurable[ZoneOffset] with
        def get(config: Config, key: String): ZoneOffset = ZoneOffset.of(config.getString(key))

    inline given Configurable[ZoneId] with
        def get(config: Config, key: String): ZoneId = ZoneId.of(config.getString(key))

    inline given Configurable[LocalTime] with
        def get(config: Config, key: String): LocalTime = LocalTime.parse(config.getString(key))

    inline given Configurable[LocalDate] with
        def get(config: Config, key: String): LocalDate = LocalDate.parse(config.getString(key))

    inline given Configurable[LocalDateTime] with
        def get(config: Config, key: String): LocalDateTime = LocalDateTime.parse(config.getString(key))

    inline given Configurable[Year] with
        def get(config: Config, key: String): Year = Year.parse(config.getString(key))

    inline given Configurable[YearMonth] with
        def get(config: Config, key: String): YearMonth = YearMonth.parse(config.getString(key))

    inline given Configurable[MonthDay] with
        def get(config: Config, key: String): MonthDay = MonthDay.parse(config.getString(key))

    inline given Configurable[OffsetTime] with
        def get(config: Config, key: String): OffsetTime = OffsetTime.parse(config.getString(key))

    inline given Configurable[OffsetDateTime] with
        def get(config: Config, key: String): OffsetDateTime = OffsetDateTime.parse(config.getString(key))

    inline given Configurable[ZonedDateTime] with
        def get(config: Config, key: String): ZonedDateTime = ZonedDateTime.parse(config.getString(key))

object JavaTypes extends JavaTypes, JavaTime
