The purpose of this project is to allow the CI to execute integration tests after the nightly EC2 deployment of Spring XD.  
It uses an artifact file produced by the ec2 deployment CI build, to setup the environment for the test.

[Setting up single admin and single container cluster]
Make sure that the following environment variables are set either the xd-config.yml or directly in the environment.  (For all variables except XD_JMX_ENABLED, replace the '_' with '.' if using the xd_config.yml

#XD Server
endpoints_jmx_enabled=true
endpoints_jmx_uniqueNames=true
endpoints_jolokia_enabled=true
XD_JMX_ENABLED=true
PORT=15003
management_port=15001
server.port=9393

#XD Container
endpoints_jmx_enabled=true
endpoints_jmx_uniqueNames=true
endpoints_jolokia_enabled=true
XD_JMX_ENABLED=true
PORT=15000
management_port=15005
server.port=9395

[Setting up single XD node]
Make sure that the following environment variables are set either the xd-config.yml or directly in the environment.  (For all variables except XD_JMX_ENABLED, replace the '_' with '.' if using the xd_config.yml

#XD Server
endpoints_jmx_enabled=true
endpoints_jmx_uniqueNames=true
endpoints_jolokia_enabled=true
XD_JMX_ENABLED=true
PORT=15000
management_port=15005
server.port=9393

[Running The Test] 
 # Running build from Command Line 
   - Gradle 
      By default the tests are not active.  To run the tests execute the following:
	     ./gradlew -Drun_integration_tests=true -Dxd_container_log_dir=/Users/renfrg/projects/spring-xd/build/dist/spring-xd/xd/logs/container.log :spring-xd-integration-test:build

# Changing default environment variables
   - Environment Variables
      * -Dxd_admin_host=http://localhost:9393 
      * -Dxd_containers=http://localhost:9393 
      * -Dxd_http_port=15000 
      * -Dxd_jmx_port=15005
      * -Dxd_private_key_file=<location of your ec2 private key file> // if you are testing ec2 cluster
      * -Dxd_run_on_ec2=[false if you are testing locally | true if you are testing on ec2]
    - For example ./gradlew  -Dxd_admin_host=http://ec2-54-197-41-192.compute-1.amazonaws.com:9393 -Dxd_containers=http://ec2-54-196-248-248.compute-1.amazonaws.com:9393 -Dxd_http_port=15000 -Dxd_jmx_port=15005 -Dxd_private_key_file=/Users/renfrg/ec2/xd-key-pair.pem -Dxd_run_on_ec2=true -Drun_integration_tests=true :spring-xd-integration-test:build			

   - Using the artifact
   	  * Setup the environment by using the ec2servers.csv file
        > Create an artifact file named ec2servers.csv and place it in the root spring-xd-integration-test directory.
	      The file looks like the following:
			adminNode,localhost,9393,15000,15005
			containerNode,localhost,9393,15000,15005 
	  * You will still need to use set the environment variables xd_private_key_file & xd_run_on_ec2.  
   - Eclipse 
      * In the run configuration of your tests add the environment variables to your VMArgs.  Use the environment variables above.  
   - Eclipse
   	  Execute the tests via the "Run As"->"JUnit Tests" infrastructure.
