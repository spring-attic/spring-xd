This module provides the infrastructure for automated multi JVM functional testing
for Spring XD. The tests under this module specifically test functionality that
* requires multiple containers, i.e. cannot be tested in single node mode
* requires the shutdown of one or more containers to verify correct recovery
 
To execute these tests, invoke the build with the system property `run_distributed_tests`
set to true; for example:
 
```
./gradlew -Drun_distributed_tests=true clean build
```
 
The tests may be executed individually in the IDE or through the gradle build
as described above. The build is configured to execute the tests in a Junit suite.
This is done in order to start up the test infrastructure *once* and to tear it 
down after all tests are executed. The infrastructure includes:
* ZooKeeper
* HSQL
* Spring XD admin server
  
Therefore, when adding new test classes, each class **must** be added to the 
`@Suite.SuiteClasses` annotation in `org.springframework.xd.distributed.test.DistributedTestSuite`
so that the tests are included in the build.
  
Infrastructure for launching JVMs provided by the [Oracle Tools framework](https://java.net/projects/oracletools).
