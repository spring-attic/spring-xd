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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.xd.dirt.stream.ParsingContext;
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
public abstract class StacktraceFingerprintingCompletionRecoveryStrategy<E extends Exception> implements
		CompletionRecoveryStrategy<E> {

	private final Set<List<StackTraceElement>> fingerprints = new LinkedHashSet<List<StackTraceElement>>();

	protected XDParser parser;

	private final Class<E> exceptionClass;

	/**
	 * Construct a new StacktraceFingerprintingCompletionRecoveryStrategy given the parser, and the expected exception
	 * class to be thrown for sample fragments of a stream definition that is to be parsed.
	 * 
	 * @param parser the parser used to parse the sample fragment stream definitions.
	 * @param exceptionClass the expected exception that results from parsing the sample fragment stream definitions.
	 *        Stack frames from the thrown exception are used to store the fingerprint of this exception thrown by the
	 *        parser.
	 * @param samples the sample fragments of stream definitions.
	 */
	@SuppressWarnings("unchecked")
	public StacktraceFingerprintingCompletionRecoveryStrategy(XDParser parser, Class<E> exceptionClass,
			String... samples) {
		this.parser = parser;
		this.exceptionClass = exceptionClass;
		for (String sample : samples) {
			try {
				// we're only interested in the exception, which is currently
				// not influenced by the kind of parse. Use stream for now
				parser.parse("__dummy", sample, ParsingContext.partial_stream);
			}
			catch (Exception exception) {
				if (exceptionClass.isAssignableFrom(exception.getClass())) {
					computeFingerprint(parser, (E) exception);
				}
				else if (exception instanceof RuntimeException) {
					throw (RuntimeException) exception;
				}
				else {
					throw new RuntimeException(exception);
				}
			}
		}
	}

	/**
	 * Extract the top frames (until the call to {@link XDParser#parse(String, String)} appears) of the given exception.
	 */
	private void computeFingerprint(XDParser parser, E exception) {
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

	private boolean fingerprintMatches(E exception,
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
	@SuppressWarnings("unchecked")
	public boolean shouldTrigger(Exception exception, CompletionKind kind) {
		if (!exceptionClass.isAssignableFrom(exception.getClass())) {
			return false;
		}
		for (List<StackTraceElement> fingerPrint : fingerprints) {
			if (fingerprintMatches((E) exception, fingerPrint)) {
				return true;
			}
		}
		return false;
	}


}
