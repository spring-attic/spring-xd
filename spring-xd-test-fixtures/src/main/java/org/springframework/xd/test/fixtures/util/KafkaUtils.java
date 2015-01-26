/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.test.fixtures.util;

import kafka.admin.AdminUtils;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.test.fixtures.AvailableSocketPorts;

import java.util.Properties;

/**
 * Utility methods for working with Kafka.  Used by {@link org.springframework.xd.test.fixtures.KafkaSource} and
 * {@link org.springframework.xd.test.fixtures.KafkaSink}
 *
 * @author Mark Pollack
 */
public class KafkaUtils {

    /**
     * Ensures that the host and port of the zookeeper connection are available
     * @param fixtureName  The name of the fixture calling htis utility method, used for error reporting
     * @param zkConnect The Zookeeper connection string
     * @param topic The name of the topic to create
     */
    public static void ensureReady(String fixtureName, String zkConnect, String topic) {
        String[] addressArray = StringUtils.commaDelimitedListToStringArray(zkConnect);
        for (String address : addressArray) {
            String[] zkAddressArray = StringUtils.delimitedListToStringArray(address, ":");
            Assert.isTrue(zkAddressArray.length == 2,
                    "zkConnect data was not properly formatted");
            String host = zkAddressArray[0];
            int port = Integer.valueOf(zkAddressArray[1]);
            AvailableSocketPorts.ensureReady(fixtureName, host, port, 2000);
        }
        ZkClient zkClient = new ZkClient(zkConnect, 6000, 6000, ZKStringSerializer$.MODULE$);
        AdminUtils.createTopic(zkClient, topic, 1, 1, new Properties());
    }
}
