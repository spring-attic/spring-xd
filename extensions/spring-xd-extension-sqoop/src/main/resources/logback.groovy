import org.springframework.xd.dirt.util.logging.CustomLoggerConverter
import org.springframework.xd.dirt.util.logging.VersionPatternConverter
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

// Emulates Log4j formatting
conversionRule("category", CustomLoggerConverter)

//XD Version
conversionRule("version", VersionPatternConverter)

def ISO8601 = "yyyy-MM-dd'T'HH:mm:ssZ"
def datePattern = ISO8601

appender("STDOUT", ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = "%d{${datePattern}} %version %level{5} %thread %category{2} - %msg%n"
	}
}

root(INFO, ["STDOUT"])

logger("org.springframework.xd.sqoop", INFO)
logger("org.apache.sqoop", INFO)
logger("org.springframework", INFO)
/* Suppress configuration warnings */
logger("org.apache.hadoop.conf.Configuration", ERROR)
