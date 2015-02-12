/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.extension.process;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public final class ShellWordsParserTest {

    private final ShellWordsParser parser = new ShellWordsParser();

    @Test
    public void honorsQuotedStrings() {
        assertParse("a \"b b\" a",
                "a", "b b", "a");
    }

    @Test
    public void honorsEscapedDoubleQuotes() {
        assertParse("a \"\\\"b\\\" c\" d",
                "a", "\"b\" c", "d");
    }

    @Test
    public void honorsEscapedSingleQuotes() {
        assertParse("a \"'b' c\" d",
                "a", "'b' c", "d");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenDoubleQuotedStringsAreMisQuoted() {
        this.parser.parse("a \"b c d e");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenSingleQuotedStringsAreMisQuoted() {
        this.parser.parse("a 'b c d e");
    }

    @Test
    public void bashCommand() {
        assertParse("bash -c 'while read LINE ; do echo $LINE ; done'",
                "bash", "-c", "while read LINE ; do echo $LINE ; done");
    }

    private void assertParse(String s, String... words) {
        assertEquals(Arrays.asList(words), this.parser.parse(s));
    }

}
