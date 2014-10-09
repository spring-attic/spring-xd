/**
 * Utility for extracting and reporting client requests from
 * ZooKeeper trace logs. ZooKeeper tracing must be enabled
 * in its log4j configuration file for it to log client
 * requests; for example:
 *
 *   log4j.rootCategory=TRACE, zklog
 *
 * @author Patrick Peralta
 */

if (args.length < 1) {
	println "Usage: zkTrace [log file]"
	return
}

def logFile = new File(args[0]);
if (!logFile.exists()) {
	printf("%s not found\n", logFile)
	return;
}

def requests = []
logFile.eachLine { line ->
	def tokens = line.tokenize()
	if (tokens[2] == "FinalRequestProcessor"
			&& tokens[4].startsWith("sessionid:")
			&& tokens[5] != "type:ping") {
		def request = new Request()
		request.date = tokens[0]
		request.time = tokens[1]
		request.type = tokens[5].split(":")[1]
		request.sessionId = tokens[4].split(":")[1]
		request.path = tokens[9].split(":")[1]
		requests.add(request)
	}
}

final int EXISTS       = 0
final int GET_DATA     = 1
final int GET_CHILDREN = 2
final int CREATE       = 3
final int DELETE       = 4

def sessions = [:]
requests.each { request ->
	def totals = sessions[request.sessionId]
	if (totals == null) {
		totals = [0, 0, 0, 0, 0]
		sessions[request.sessionId] = totals
	}
	switch (request.type) {
		case "exists":
			totals[EXISTS]++
			break
		case "getData":
			totals[GET_DATA]++
			break
		case "getChildren2":
			totals[GET_CHILDREN]++
			break
		case "create":
			totals[CREATE]++
			break
		case "delete":
			totals[DELETE]++
			break
	}
}

sessions.each { session ->
	printf("session:  %s%n", session.key)
	printf("requests: %d%n", session.value.sum())
	printf("\texists: %03d\tgetData: %03d\tgetChildren2: %03d\tcreate: %03d\tdelete: %03d%n%n",
			session.value[EXISTS], session.value[GET_DATA], session.value[GET_CHILDREN],
			session.value[CREATE], session.value[DELETE])
}

class Request {
	String date
	String time
	String type
	String path
	String sessionId

	@Override
	public String toString() {
		return "Request{" +
				"date='" + date + '\'' +
				", time='" + time + '\'' +
				", type='" + type + '\'' +
				", path='" + path + '\'' +
				", sessionId='" + sessionId + '\'' +
				'}';
	}
}
