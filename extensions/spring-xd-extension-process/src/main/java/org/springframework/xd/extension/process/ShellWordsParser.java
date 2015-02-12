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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a string into a collection of words based on the
 * <a href="http://pubs.opengroup.org/onlinepubs/009695399/toc.htm">POSIX / SUSv3 standard</a>.  The functionality in
 * this class is ported from the Ruby
 * <a href="https://github.com/rubysl/rubysl-shellwords/blob/2.0/lib/rubysl/shellwords/shellwords.rb">Shellwords</a>
 * module.
 */
final class ShellWordsParser {

    private static final Pattern SHELLWORDS = Pattern.compile("\\G\\s*(?>([^\\s\\\\'\"]+)|'([^']*)'|\"((?:[^\"\\\\]|\\\\.)*)\"|(\\\\.?)|(\\S))(\\s|\\z)?",
            Pattern.MULTILINE);

    /**
     * Parses a string into a collection of words based on POSIX standard
     *
     * @param s the string to parse
     * @return the collection of words
     */
    List<String> parse(String s) {
        List<String> words = new ArrayList<>();
        String field = "";

        Matcher matcher = SHELLWORDS.matcher(s);
        while (matcher.find()) {
            String word = matcher.group(1);
            String sq = matcher.group(2);
            String dq = matcher.group(3);
            String esc = matcher.group(4);
            String garbage = matcher.group(5);
            String sep = matcher.group(6);

            if (garbage != null) {
                throw new IllegalArgumentException("Unmatched double quote: " + s);
            }

            if (word != null) {
                field = word;
            } else if (sq != null) {
                field = sq;
            } else if (dq != null) {
                field = dq.replaceAll("\\\\(?=.)", "");
            } else if (esc != null) {
                field = esc.replaceAll("\\\\(?=.)", "");
            }

            if (sep != null) {
                words.add(field);
                field = "";
            }
        }

        return words;
    }
}
