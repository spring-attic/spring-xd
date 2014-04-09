The purpose of this project is to allow the CI to execute integration tests after the nightly EC2 deployment of Spring XD.
It uses an artifact file produced by the ec2 deployment CI build, to setup the environment for the test.

## Setting up single admin and single container cluster on the same machine
Make sure that the following environment variables are set either the servers.yml or directly in the environment.
##### If you use Environment Variables use the settings below:
```
# For the XD Admin Server
	export endpoints_jmx_enabled=true
	export endpoints_jmx_uniqueNames=true
	export endpoints_jolokia_enabled=true
	export XD_JMX_ENABLED=true
	export management_port=15001
	export server.port=9393
	export PORT=9001

	#For the XD Container
	export endpoints_jmx_enabled=true
	export endpoints_jmx_uniqueNames=true
	export endpoints_jolokia_enabled=true
	export XD_JMX_ENABLED=true
	export management_port=15005
	export server_port=9395
```
##### If you use servers.yml:
```
	# For the XD Admin Server
	jmx:
	  enabled: true
	  uniqueNames: true
	jolokia:
	  enabled: true

	---

	XD_JMX_ENABLED: true

	---

	management:
	  port: 15001

	---

	server:
	  port: 9393
```

```
	# For the XD Container Server
	jmx:
	  enabled: true
	  uniqueNames: true
	jolokia:
	  enabled: true

	---

	XD_JMX_ENABLED: true

	---

	management:
	  port: 15005

	---

	server:
	  port: 9395
```

**NOTE:**
*You must set the "PORT=9001" environment variable for the admin server.  At this time XD does not recognize the PORT setting in servers.yml*


## Setting up single XD node
Make sure that the following environment variables are set either the servers.yml or directly in the environment.
**If you use Environment Variables use the settings below:**
```
	#XD Server
	export endpoints_jmx_enabled=true
	export endpoints_jmx_uniqueNames=true
	export endpoints_jolokia_enabled=true
	export XD_JMX_ENABLED=true
	export management_port=15005
	export server_port=9393
```
**If you use servers.yml:**
```
	# For the XD Server
	jmx:
	  enabled: true
	  uniqueNames: true
	jolokia:
	  enabled: true

	---

	XD_JMX_ENABLED: true

	---

	management:
	  port: 15005

	---

	server:
	  port: 9393
```
## Setting up single admin and single container cluster on different machines
Make sure that the following environment variables are set either the servers.yml or directly in the environment.

**If you use Environment Variables use the settings below:**
```
# For the XD Admin Server & ContainerServer
	export endpoints_jmx_enabled=true
	export endpoints_jmx_uniqueNames=true
	export endpoints_jolokia_enabled=true
	export XD_JMX_ENABLED=true
	export management_port=15000
	export server_port=9393
```
**If you use servers.yml:**
```
	# For the XD  Server
	jmx:
	  enabled: true
	  uniqueNames: true
	jolokia:
	  enabled: true

	---

	XD_JMX_ENABLED: true

	---

	management:
	  port: 15000

	---

	server:
	  port: 9393
```

##  Running The Test

### Running on Local Host
#### Running build from Command Line
##### Gradle Single admin and single container on same machine
* By default the tests are not active.
* To run the tests execute the following:

```
./gradlew -Drun_integration_tests=true -Dxd_container_log_dir=<your dir>/container.log :spring-xd-integration-test:build
```
##### Gradle SingleNode
* By default the tests are not active.  To run the tests execute the following:

```
./gradlew -Drun_integration_tests=true -Dxd_container_log_dir=<your dir>/singlenode.log :spring-xd-integration-test:build
```

### Running on EC2
#### Running build from Command Line

```
./gradlew  -Dxd_admin_host=http://ec2-54-197-41-192.compute-1.amazonaws.com:9393
-Dxd_containers=http://ec2-54-196-248-248.compute-1.amazonaws.com:9393 -Dxd_http_port=15000 -Dxd_jmx_port=15005 -Dxd_private_key_file=<your dir>/xd-key-pair.pem -Dxd_run_on_ec2=true -Drun_integration_tests=true :spring-xd-integration-test:build
```

### Eclipse localhost
In the run configuration of your tests add the environment variables to your VMArgs.  Use the environment variables below:
```
        -Dxd_admin_host=http://localhost:9393
        -Dxd_containers=http://localhost:9393
        -Dxd_http_port=9000
        -Dxd_jmx_port=15005
        -Dxd_private_key_file=<location of your ec2 private key file> // if you are testing ec2 cluster
        -Dxd_run_on_ec2=[false if you are testing locally | true if you are testing on ec2]
        -Dxd_container_log_dir=<the location of container/singlenode log>
```
* Execute the tests via the "Run As"->"JUnit Tests" infrastructure.
* Using the artifact
  * Setup the environment by using the ec2servers.csv file
    * Create an artifact file named ec2servers.csv and place it in the root spring-xd-integration-test directory.  The file looks like the following:

```
			adminNode,localhost,9393,15000,15005
			containerNode,localhost,9393,15000,15005
```

**Note:**
*You will still need to use set the environment variables xd_private_key_file & xd_run_on_ec2.*

### Environment variables
  * xd_admin_host - the ip and port of the administrator server
  * xd_containers - the ip and port of the container server
  * xd_http_port - the http source port
  * xd_http_jmx_port - the port for JMX communications
  * xd_private_key_file - the URI of the ec2 private key
  * xd_run_on_ec2 - If set to true, system will pull result file and logs from remote servers.


## Module Configuration

Out of the box all modules should require no additional setup for local testing.  However in a distributed environment resources such as databases, Message Queues and Hadoop servers lie in different locations.  This section will discuss how to configure these modules to work in  a distributed environment

### JDBC

The Jdbc sink test has the following parameters:
````
jdbc_username: Default: 'sa'
jdbc_database: Default: 'xdjob'
jdbc_password: Default:  ''
jdbc_driver:   Default: 'org.hsqldb.jdbc.JDBCDriver'
jdbc_url:      Default: 'jdbc:hsqldb:hsql://localhost:9101/%s'
````
By default the JDBC sink test will test against the hsqldb embedded in a singlenode deployment on the local machine.
When running an acceptance test on a singlenode on another machine or a XD Clustered deployment the parameters above must be utilized.
The %s in the jdbc_url will be populated by the jdbc_database.
An example command line would look like this if running on an acceptance test on a remote XD cluster.

