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

import static org.springframework.xd.dirt.stream.XDParser.EntityType.stream;

import java.util.ArrayList;
import java.util.List;

import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.rest.client.domain.CompletionKind;

/**
 * A recovery strategy that will trigger if the parser failure is similar to that of some sample unfinished stream
 * definition. The match is decided by analyzing the top frames of the stack trace emitted by the parser when it
 * encounters the ill formed input. Multiple fingerprints are supported, as the control flow in the parser code may be
 * different depending on the form of the expression. For example, for the rule {@code stream = module (| module)* },
 * the pseudo code for the parser may look like
 * 
 * <pre>
 * <code>
 * stream() {
 *   module();  (1)
 *   while(moreInput()) {
 *     swallowPipe();
 *     module();  (2)
 *   }
 * }
 * </code>
 * </pre>
 * 
 * In that setup, whether we're dealing with the first module, or a subsequent module, stack frames would be different
 * (see (1) and (2)).
 * 
 * @author Eric Bottard
 */
public abstract class StacktraceFingerprintingCompletionRecoveryStrategy<E extends Throwable> implements
		CompletionRecoveryStrategy<E> {

	private List<List<StackTraceElement>> fingerprints = new ArrayList<List<StackTraceElement>>();


	protected XDParser parser;

	public StacktraceFingerprintingCompletionRecoveryStrategy(XDParser parser, String... samples) {
		this.parser = parser;
		for (String sample : samples) {
			try {
				// we're only interested in the exception, which is currently
				// not influenced by the kind of parse. Use stream for now
				parser.parse("__dummy", sample, stream);
			}
			catch (Throwable exception) {
				computeFingerprint(parser, exception);
			}
		}
	}

	/**
	 * Extract the top frames (until the call to {@link XDParser#parse(String, String)} appears) of the given exception.
	 */
	private void computeFingerprint(XDParser parser, Throwable exception) {
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

	private boolean fingerprintMatches(Throwable exception,
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

	@Override
	public boolean shouldTrigger(Throwable exception, CompletionKind kind) {
		for (List<StackTraceElement> fingerPrint : fingerprints) {
			if (fingerprintMatches(exception, fingerPrint)) {
				return true;
			}
		}
		return false;

	}


}
