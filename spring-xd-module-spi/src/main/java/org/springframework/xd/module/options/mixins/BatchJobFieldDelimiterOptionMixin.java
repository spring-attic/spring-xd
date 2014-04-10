/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.module.options.mixins;

import org.springframework.xd.module.options.spi.ModuleOption;

import javax.validation.constraints.NotNull;


/**
 * Batch job option for the delimiter in the file (csv file etc.,).
 * 
 * @author Michael Minella
 */
public class BatchJobFieldDelimiterOptionMixin {

	private String delimiter = ",";

	@ModuleOption(value = "the delimiter for the delimited file", defaultValue = ",")
	public void setDelimiter(String delimiter) {
		this.delimiter = unescape(delimiter);
	}

	@NotNull
	public String getDelimiter() {
		return delimiter;
	}

	private String unescape(String st) {

		StringBuilder sb = new StringBuilder(st.length());

		for (int i = 0; i < st.length(); i++) {
			char ch = st.charAt(i);
			if (ch == '\\') {
				char nextChar = (i == st.length() - 1) ? '\\' : st.charAt(i + 1);
				// Octal escape?
				if (nextChar >= '0' && nextChar <= '7') {
					String code = "" + nextChar;
					i++;
					if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
								&& st.charAt(i + 1) <= '7') {
						code += st.charAt(i + 1);
						i++;
						if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
									&& st.charAt(i + 1) <= '7') {
							code += st.charAt(i + 1);
							i++;
						}
					}
					sb.append((char) Integer.parseInt(code, 8));
					continue;
				}
				switch (nextChar) {
					case '\\':
						ch = '\\';
						break;
					case 'b':
						ch = '\b';
						break;
					case 'f':
						ch = '\f';
						break;
					case 'n':
						ch = '\n';
						break;
					case 'r':
						ch = '\r';
						break;
					case 't':
						ch = '\t';
						break;
					case '\"':
						ch = '\"';
						break;
					case '\'':
						ch = '\'';
						break;
					case 'u':
						if (i >= st.length() - 5) {
							ch = 'u';
							break;
						}
						int code = Integer.parseInt("" + st.charAt(i + 2) + st.charAt(i + 3)
													   + st.charAt(i + 4) + st.charAt(i + 5), 16);
						sb.append(Character.toChars(code));
						i += 5;
						continue;
				}
				i++;
			}
			sb.append(ch);
		}
		return sb.toString();	}
}
