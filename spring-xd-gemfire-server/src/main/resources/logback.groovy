import ch.qos.logback.classic.encoder.PatternLayoutEncoder

appender("STDOUT", ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = "%d{${datePattern}} %version %level{5} %thread %category{2} - %msg%n"
	}
}

root(WARN, ["STDOUT"])
logger("org.springframework.xd.gemfire", DEBUG)
