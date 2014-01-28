The purpose of this project is to allow the CI to execute integration tests after the nightly EC2 deployment of Spring XD.  
It uses an artifact file produced by the ec2 deployment CI build, to setup the environment for the test.

[Setting up the cluster]
Make sure that the following environment variables are set either the xd-config.yml or directly in the environment.  (For all variables except XD_JMX_ENABLED, replace the '_' with '.' if using the xd_config.yml

#XD Server
endpoints_jmx_enabled=true
endpoints_jmx_uniqueNames=true
endpoints_jolokia_enabled=true
XD_JMX_ENABLED=true
PORT=15003
management_port=15001
server.port=9393

#XD Server
endpoints_jmx_enabled=true
endpoints_jmx_uniqueNames=true
endpoints_jolokia_enabled=true
XD_JMX_ENABLED=true
PORT=15005
management_port=15000
server.port=9395

[Running The Test]
* When running the test from your eclipse 
   - Set the VM Arguments to -Dxd-admin-host=http://localhost:9393 -Dxd-containers=http://localhost:9395 -Dxd-http-port=15005 -Dxd-jmx-port=15000
    -or-
   - Create an artifact file named ec2servers.csv and place it in the root spring-xd-integration-test directory.
	  The file looks like the following:
			adminNode,localhost,9393,15005,15000
			containerNode,localhost,9393,15005,15000