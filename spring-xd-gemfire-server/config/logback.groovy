import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.rolling.RollingFileAppender

appender("STDOUT", ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = "[%d{'yyyy-MM-dd mm:ss.SSS'}] %level{5} %thread %logger{2} - %msg%n"
	}
}
def logfileNameBase = "logs/gemfire-cacheserver"

appender("FILE", RollingFileAppender) {
	file = "${logfileNameBase}.log"
	append = false
	rollingPolicy(TimeBasedRollingPolicy) {
		fileNamePattern = "${logfileNameBase}-%d{yyyy-MM-dd}.%i.log"
		timeBasedFileNamingAndTriggeringPolicy(SizeAndTimeBasedFNATP) {
			maxFileSize = "100KB"
		}
	}

	encoder(PatternLayoutEncoder) {
		pattern = "[%d{'yyyy-MM-dd mm:ss.SSS'}] %level{5} %thread %logger{2} - %msg%n"
	}
}

root(WARN, ["STDOUT", "FILE"])
logger("org.springframework.xd.gemfire", DEBUG)