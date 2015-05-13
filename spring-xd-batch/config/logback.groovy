logger("org.springframework", INFO)
logger("org.springframework.boot", INFO)
// Suppress json-path warning until SI 4.2 is released
logger("org.springframework.integration.config.IntegrationRegistrar",ERROR)


appender("STDOUT", ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = "[%d{'yyyy-MM-dd mm:ss.SSS'}] %level{5} %thread %c{2} - %msg%n"
	}
}

root(INFO, ["STDOUT"])