The purpose of this project is run a comprehensive set of system integration tests for several configurations of the XD runtime.
The goal is to have all source, sink, processors and jobs that are provided as part of the XD distribution to be tested across
all supported transports and analytic peristence options.  


The tests are run in the [CI server with XD deployed onto EC2](https://build.spring.io/browse/XD-ATEC2) to act as a general regression test against nightly builds of XD off master.  You can also run the tests against a locally deployed XD, either singlenode or distributed.

## Prerequisites

To successfully run all the tests in a local deployment, you will need to install several other components

* ActiveMQ
* Hadoop 2.2
* RabbitMQ with MQTT Management Plugin Enabled.
* Twitter development account for authorization keys

## Setting up single XD node
If you run these tests against an xd-singlenode instance, it will use all the default values found in [application.yml](https://github.com/spring-projects/spring-xd/blob/master/spring-xd-dirt/src/main/resources/application.yml).  The tests for singlenode will read values from the file [application-singlenode.properties](https://github.com/spring-projects/spring-xd/blob/master/spring-xd-integration-test/src/test/resources/application-singlenode.properties).  If you change any values, such as port numbers, in application-singlenode.properties you will need to update values in servers.yml or environment variables to the port numbers match.

If you want to change default values for ports, here are examples of how to do that.

**If you use Environment Variables**
```
	#XD Server
	export management_port=15005
	export server_port=9393
```
**If you use servers.yml**
```
	management:
	  port: 15005

	---

	server:
	  port: 9393
```


## Setting up single admin and single container cluster on the same machine
Make sure that the following environment variables are set either the servers.yml or directly in the environment.
##### If you use Environment Variables use the settings below:
```
# For the XD Admin Server
	export management_port=15001
	export server_port=9393
	export PORT=9001
```

```
# For the XD Container
	export management_port=15005
	export server_port=9395
```
##### If you use servers.yml:
```
# For the XD Admin Server

	management:
	  port: 15001

	---

	server:
	  port: 9393
```

```
# For the XD Container Server

	management:
	  port: 15005

	---

	server:
	  port: 9395
```

**NOTE:**
*You must set the "PORT=9001" environment variable for the admin server.  At this time XD does not recognize the PORT setting in servers.yml*



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

### Profiles

The acceptance tests utilizes Spring profiles to to configure the run.  This way if you want to run against a local instances of XD and then run the same test against an XD instance on EC2 all you need to do is change your active profile.
Out of the box the acceptance tests will be configured using the provided application-singlenode.properties.  To create and use a new profile:

1. Create a new properties file with the following format for the name application-*__profile name__*.properties.  For example application-__mycluster__.properties
2. Copy the contents of the application-singlenode.properties to you new properties file.
3. Change the settings to your needs.
4. Save the changes.
5. In your environment set the new spring\_profiles\_active
  1. On Mac and Unix:  export spring\_profiles\_active=mycluster
6. Now your profile  __mycluster__ is active and when you startup up acceptance tests it will use the application-mycluster.properties to setup you acceptance tests.

### Running on Local Host SingleNode

There are 2 steps to running a your acceptance tests.  
1) Set your XD_HOME
* While in your spring-xd project, use your favorite editor to open spring-xd-integration-test/src/test/resources/application-singlenode.properties
* set the XD_HOME property to the location where you deployed XD. For Example:

```
#Location
XD_HOME=/Users/renfrg/projects/spring-xd/build/dist/spring-xd/xd
```
2) Run All Acceptance tests

```
./gradlew -Drun_integration_tests=true :spring-xd-integration-test:build
```

**What if I want to run just a single test?**  
In this case you can add the -Dtest.single=  along with the test you want ot run.  For Example:
```
./gradlew -Drun_integration_tests=true -Dtest.single=HttpTest :spring-xd-integration-test:build
```
### Running on Local Host XD Clustered

Following the Singlenode instructions above, you will only need to make 3 additional changes (Assuming you are running the hsqldb-server).  
1) Since we will be running an Admin and Container combination, the logs for which the Acceptance tests will be monitoring will be the container's.  So, the xd\_container\_log\_dir will have to be updated as shown below:
```
xd_container_log_dir=${XD_HOME}/logs/container.log
```
2) Update the xd_jmx_port to 9395 in the application-singlenode.properties, for example:
```
xd_jmx_port=9395
```
3) Before starting the container set the following environment variable XD_MGMT_PORT to 9395, for example:
```
export XD_MGMT_PORT=9395
```

### Running on EC2
Using the application-ec2.properties provided you will need to update the following properties:

1. xd\_admin\_host
2. xd\_containers
3. xd\_private\_keyfile 
4. JDBC Settings 
  * jdbc_url
  * jdbc_driver
  * jdbc_password
  * jdbc_database
  * jdbc_username

For Example:
```
xd_admin_host=http://ec2-23-22-34-139.compute-1.amazonaws.com:9393
xd_containers=http://ec2-54-82-119-240.compute-1.amazonaws.com:9393
...

#Ec2 Settings
xd_pvt_keyfile=/Users/renfrg/ec2/xd-key-pair.pem
...

#JDBC Test Setting
jdbc_url=jdbc:mysql://xdjobrepo.adsfa.us-east-1.rds.amazonaws.com:3306/%s
jdbc_driver=com.mysql.jdbc.Driver
jdbc_password=mypassword
jdbc_database=xdjob
jdbc_username=myuser
```

#### Running build from Command Line

1) Set the spring\_profiles\_active environment variable to ec2.  For Example (Mac/Unix):

```
export spring_profiles_active=ec2
```
2) Run the Acceptance Tests.

```
./gradlew  -Drun_integration_tests=true :spring-xd-integration-test:build
```

### Eclipse 
You can run acceptance tests from Eclipse. 

#### Single Node Acceptance Tests:

1. Make sure that you have setup the acceptance-singlenode.properties according to Step 1 in the " Running on Local Host SingleNode" section of this document.  
2. In the run configuration of your tests select Environment tab. 
3. Click the new button
4. In the name field enter, __spring_profiles_active__
5. In the value field enter the profile you are using.  For example: singlenode
6. Click the ok button.
7. Execute the tests via the "Run As"->"JUnit Tests" infrastructure.


##### Using the artifact
  * Setup the environment by using the ec2servers.csv file
    * Create an artifact file named ec2servers.csv and place it in the root spring-xd-integration-test directory.  The file looks like the following:

```
			adminNode,localhost,9393,15000,15005
			containerNode,localhost,9393,15000,15005
```


## Module Configuration

Some modules require additional setup for resources such as databases, Message Queues and Hadoop servers lie in different locations.  This section will discuss how to configure these modules to work in  a distributed environment

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

### MQTT

1. Verify that your MQTT plugin is enabled on your rabbit instance.  If not, enable it based on rabbit's instructions.
2. The JmxTest will look for the Rabbit MQTT instance on the host declared by the xd\_admin\_host declared in the application-<your profile>.properties file that you are using. 

### Log test

Container logs are written in the format container-${PID}.log by default.  To support the ability to test the log module for both singlenode and clustered environments, the xd_container_log_dir property
will accept a log file like ```${XD_HOME}/xd/logs/singlenode.log``` or place the PID in the log name like ```${XD_HOME}/xd/logs/container-[PID].log```.  Where [PID] will be replaced
with the container's PID.  For example: ```${XD_HOME}/xd/logs/container-1234.log```.
