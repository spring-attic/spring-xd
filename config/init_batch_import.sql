-- Database initialization for the JDBC batch import job module
-- This script is referenced in the batch-jdbc.properties

-- uses stream name as table name by default
-- column definitions are created from the "names" parameter used to configure the module
drop table #table;
create table #table (#columns);
