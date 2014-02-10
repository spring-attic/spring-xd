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

#Running The Test
* When running the test set up the environment based on your execution strategy:
   - Command Line : 
   	Setup the following environment variables in the integration test environment.
    * export xd_admin_host=http://localhost:9393 
    * export xd_containers=http://localhost:9393 
    * export xd_http_port=15000 
    * export xd_jmx_port=15005
    * export xd_private_key_file=<location of your ec2 private key file> // if you are testing ec2 cluster
    * export xd_run_on_ec2=[false if you are testing locally | true if you are testing on ec2]
   - Eclipse 
   In the run configuration for your test select the environment tab.  Add the environment variables from the command line instruction above.
    -or-
   - Create an artifact file named ec2servers.csv and place it in the root spring-xd-integration-test directory.
	  The file looks like the following:
			adminNode,localhost,9393,15000,15005
			containerNode,localhost,9393,15000,15005
			
