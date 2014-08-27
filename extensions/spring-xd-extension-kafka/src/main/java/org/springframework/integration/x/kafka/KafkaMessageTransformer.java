/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.x.kafka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.integration.transformer.AbstractPayloadTransformer;


/**
 * A transformer that transforms the Kafka messages into {@link Map} of partition and payload.
 * Since kafka source module has a single topic, the topic information can be removed.
 *
 * @author Ilayaperumal Gopinathan
 */
public class KafkaMessageTransformer extends
		AbstractPayloadTransformer<Map<String, Map<Integer, List<Object>>>, Map<Integer, Object>> {

	/**
	 * Transform message payload to Map<Integer, Object>
	 * where partitionIndex is the key and message payload is the value.
	 */
	@Override
	protected Map<Integer, Object> transformPayload(Map<String, Map<Integer, List<Object>>> payload) throws Exception {
		Map<Integer, Object> result = new HashMap<Integer, Object>();
		for (Map<Integer, List<Object>> message : payload.values()) {
			Integer key = message.keySet().iterator().next();
			for (Object value : message.values()) {
				result.put(key, value);
			}
		}
		return result;
	}

}
