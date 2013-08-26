Spring XD YARN
==============

Core XD Yarn app classes are under this project. We have custom appmaster XdYarnAppmaster
and custom container XdYarnContainer. Additionally custom runner XdYarnAppmasterRunner
is used to hook Spring web application context with AdminServer.

We can't use launcher facilities from XD because we already have our own way to
bootstrap Spring apps in spring-yarn. In our own runner we basically duplicate
functionality how XD Admin is launched. XD Admin is embedded inside XdYarnAppmaster.

XD Shell depends on command set from *org/springframework/xd/shell/hadoop/YarnCommands.java* and
Yarn resource manager setting added to *org/springframework/xd/shell/hadoop/ConfigurationCommands.java*.
