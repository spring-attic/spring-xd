import org.springframework.xd.dirt.util.logging.CustomLoggerConverter
import org.springframework.xd.dirt.util.logging.VersionPatternConverter
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.rolling.RollingFileAppender

// We highly recommended that you always add a status listener just
// after the last import statement and before all other statements
// NOTE - this includes logging configuration in the log and stacktraces in the event of errors
// statusListener(OnConsoleStatusListener)

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

def logfileNameBase = "${System.getProperty('xd.home')}/logs/singlenode-${System.getProperty('PID')}"

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
		pattern = "%d{${datePattern}} %version %level{5} %thread %category{2} - %msg%n"
	}
}

root(WARN, ["STDOUT", "FILE"])

logger("org.springframework.xd", WARN)
logger("org.springframework.xd.dirt.server", INFO)
logger("org.springframework.xd.dirt.util.XdConfigLoggingInitializer", INFO)
logger("xd.sink", INFO)
logger("org.springframework.xd.sqoop", INFO)
// This is for the throughput-sampler sink module
logger("org.springframework.xd.integration.throughput", INFO)

logger("org.springframework", WARN)
logger("org.springframework.boot", WARN)
logger("org.springframework.integration", WARN)
logger("org.springframework.retry", WARN)
logger("org.springframework.amqp", WARN)

// Below this line are specific settings for things that are too noisy
logger("org.springframework.beans.factory.config", ERROR)
logger("org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer", ERROR)

// This prevents the WARN level InstanceNotFoundException: org.apache.ZooKeeperService:name0=StandaloneServer_port-1
logger("org.apache.zookeeper.jmx.MBeanRegistry", ERROR)


// This prevents the WARN level about a non-static, @Bean method in Spring Batch that is irrelevant
logger("org.springframework.context.annotation.ConfigurationClassEnhancer", ERROR)

// This prevents the "Error:KeeperErrorCode = NodeExists" INFO messages
// logged by ZooKeeper when a parent node does not exist while
// invoking Curator's creatingParentsIfNeeded node builder.
logger("org.apache.zookeeper.server.PrepRequestProcessor", WARN)


// This prevents boot LoggingApplicationListener logger's misleading warning message
logger("org.springframework.boot.logging.LoggingApplicationListener", ERROR)



// This prevents Hadoop configuration warnings
logger("org.apache.hadoop.conf.Configuration", ERROR)

// Suppress json-path warning until SI 4.2 is released
logger("org.springframework.integration.config.IntegrationRegistrar", ERROR)

