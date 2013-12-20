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

package org.springframework.xd.dirt.stream.completion;

import java.util.ArrayList;
import java.util.List;

import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.dsl.CheckpointedStreamDefinitionException;
import org.springframework.xd.rest.client.domain.CompletionKind;

/**
 * A recovery strategy that will trigger if the parser failure is similar to that of some sample unfinished stream
 * definition. The match is decided by analyzing the top frames of the stack trace emitted by the parser when it
 * encounters the ill formed input.
 * 
 * @author Eric Bottard
 */
public abstract class StacktraceFingerprintlingCompletionRecoveryStrategy {

	protected XDParser parser;


	private List<List<StackTraceElement>> fingerprints = new ArrayList<List<StackTraceElement>>();

	public StacktraceFingerprintlingCompletionRecoveryStrategy(XDParser parser, String... samples) {
		this.parser = parser;
		for (String sample : samples) {
			try {
				parser.parse("dummy", sample);
			}
			catch (CheckpointedStreamDefinitionException exception) {
				computeFingerprint(parser, exception);
			}
		}
	}

	/**
	 * Extract the top frames (until the call to {@link XDParser#parse(String, String)} appears) of the given exception.
	 */
	private void computeFingerprint(XDParser parser, CheckpointedStreamDefinitionException exception) {
		boolean seenParserClass = false;
		List<StackTraceElement> fingerPrint = new ArrayList<StackTraceElement>();
		for (StackTraceElement frame : exception.getStackTrace()) {
			if (frame.getClassName().equals(parser.getClass().getName())) {
				seenParserClass = true;
			}
			else if (seenParserClass) {
				break;
			}
			fingerPrint.add(frame);
		}
		fingerprints.add(fingerPrint);
	}

	public boolean matches(CheckpointedStreamDefinitionException exception) {
		for (List<StackTraceElement> fingerPrint : fingerprints) {
			if (fingerprintMatches(exception, fingerPrint)) {
				return true;
			}
		}
		return false;

	}

	private boolean fingerprintMatches(CheckpointedStreamDefinitionException exception,
			List<StackTraceElement> fingerPrint) {
		int i = 0;
		StackTraceElement[] stackTrace = exception.getStackTrace();
		for (StackTraceElement frame : fingerPrint) {
			if (!stackTrace[i++].equals(frame)) {
				return false;
			}
		}
		return true;
	}

	abstract void use(CheckpointedStreamDefinitionException exception, List<String> result, CompletionKind kind);

}
