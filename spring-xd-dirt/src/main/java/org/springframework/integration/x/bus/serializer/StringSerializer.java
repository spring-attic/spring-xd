
package org.springframework.integration.x.bus.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author David Turanski
 * @since 1.0
 */
public class StringSerializer extends SingleTypeSerializer<String> {

	@Override
	protected void doSerialize(String object, Kryo kryo, Output output) {

		output.writeString(object);
		output.flush();
	}

	@Override
	protected String doDeserialize(Kryo kryo, Input input) {
		return input.readString();
	}
}
