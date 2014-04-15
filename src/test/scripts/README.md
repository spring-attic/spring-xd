Running script tests
=========

Apart from the unit and integration tests, the directory `src/test/scripts` contains set of scripts that run end-to-end tests on XD runtime. Please see the instructions to setup and run:

* Once XD is built (with copyInstall), from the distribution directory: `build/dist/spring-xd/xd/bin/xd/bin/xd-singlenode(.bat)`
* Setup XD_HOME environment variable that points to `build/dist/spring-xd/xd`
* From the directory `src/test/scripts`, run `basic_stream_tests`
* For the `jdbc_tests`, we need to run `install_sqlite_jar` first that installs sqlite jar into `$XD_HOME/lib`
* For the `hdfs_import_export_tests`, make sure you have setup hadoop environment and have the `xd-singlenode` started with appropriate hadoopDistro option and hadoop lib jars for the version chosen
* For `tweet_tests`, make sure you have the twitter properties updated before running the tests
