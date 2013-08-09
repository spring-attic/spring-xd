/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Http commands.
 * 
 * @author Jon Brisbin
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Eric Bottard
 */

@Component
public class HttpCommands implements CommandMarker {

	private static final String DEFAULT_MEDIA_TYPE = MediaType.TEXT_PLAIN_VALUE + "; Charset=UTF-8";

	private static final String POST_HTTPSOURCE = "http post";

	@CliCommand(value = { POST_HTTPSOURCE }, help = "POST data to http endpoint")
	public String postHttp(
			@CliOption(mandatory = true, key = { "", "target" }, help = "the location to post to", unspecifiedDefaultValue = "http://localhost:9000")
			String target,
			@CliOption(mandatory = false, key = "data", help = "the text payload to post. exclusive with file. Note JSON fields should be separated by a comma without any spaces")
			String data,
			@CliOption(mandatory = false, key = "file", help = "filename to read data from. exclusive with data")
			File file,
			@CliOption(mandatory = false, key = "contentType", help = "the content-type to use. file is also read using the specified charset", unspecifiedDefaultValue = DEFAULT_MEDIA_TYPE)
			MediaType mediaType) throws IOException {
		Assert.isTrue(file != null || data != null, "One of 'file' or 'data' must be set");
		Assert.isTrue(file == null || data == null, "Only one of 'file' or 'data' must be set");
		if (mediaType.getCharSet() == null) {
			mediaType = new MediaType(mediaType, Collections.singletonMap("charset", Charset.defaultCharset()
					.toString()));
		}

		if (file != null) {
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), mediaType.getCharSet());
			data = FileCopyUtils.copyToString(isr);
		}

		final StringBuilder buffer = new StringBuilder();
		URI requestURI = URI.create(target);
		RestTemplate restTemplate = new RestTemplate();

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		final HttpEntity<String> request = new HttpEntity<String>(data, headers);

		try {
			restTemplate.setErrorHandler(new ResponseErrorHandler() {

				@Override
				public boolean hasError(ClientHttpResponse response) throws IOException {
					HttpStatus status = response.getStatusCode();
					return (status == HttpStatus.BAD_GATEWAY || status == HttpStatus.GATEWAY_TIMEOUT || status == HttpStatus.INTERNAL_SERVER_ERROR);
				}

				@Override
				public void handleError(ClientHttpResponse response) throws IOException {
					outputError(response.getStatusCode(), buffer);
				}
			});
			outputRequest("POST", requestURI, mediaType, data, buffer);
			ResponseEntity<String> response = restTemplate.postForEntity(requestURI, request, String.class);
			outputResponse(response, buffer);
			String status = (response.getStatusCode().equals(HttpStatus.OK) ? "Success" : "Error");
			return String.format(buffer.toString() + status + " sending data '%s' to target '%s'", data, target);
		}
		catch (ResourceAccessException e) {
			return String.format(buffer.toString() + "Failed to access http endpoint %s", target);
		}
		catch (Exception e) {
			return String.format(buffer.toString() + "Failed to send data to http endpoint %s", target);
		}
	}

	private void outputRequest(String method, URI requestUri, MediaType mediaType, String requestData,
			StringBuilder buffer) {
		buffer.append("> ").append(method).append(" (").append(mediaType.toString()).append(") ")
				.append(requestUri.toString()).append(" ").append(requestData).append(OsUtils.LINE_SEPARATOR);
	}

	private void outputResponse(ResponseEntity<String> response, StringBuilder buffer) {
		buffer.append("> ").append(response.getStatusCode().value()).append(" ")
				.append(response.getStatusCode().name()).append(OsUtils.LINE_SEPARATOR);
		for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
			buffer.append("> ").append(entry.getKey()).append(": ");
			boolean first = true;
			for (String s : entry.getValue()) {
				if (!first) {
					buffer.append(",");
				}
				else {
					first = false;
				}
				buffer.append(s);
			}
			buffer.append(OsUtils.LINE_SEPARATOR);
		}
		buffer.append("> ").append(OsUtils.LINE_SEPARATOR);
		if (null != response.getBody()) {
			buffer.append(response.getBody());
		}
	}

	private void outputError(HttpStatus status, StringBuilder buffer) {
		buffer.append("> ").append(status.value()).append(" ").append(status.name()).append(OsUtils.LINE_SEPARATOR);
	}

}
