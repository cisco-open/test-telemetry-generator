/*
 * Copyright 2022 AppDynamics Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.contrib.generator.core.jel.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlphanumericHelper {

    private static List<Character> alphanums;
    private static int alphanumsLength;

    private AlphanumericHelper() {}

    /**
     * Get next alphanumeric string after the provided input string. Each alphanumeric character can consist of 0-9, a-z & A-Z
     * set of characters. This method tries to flip the rightmost character of the string to the next character in the
     * 0 -> 9 -> a -> z -> A -> Z order to obtain the resultant string. Once a character reaches Z, it is reset to 0 and the
     * character to the left is also flipped as a result. If the input string consists of only Z characters, the whole string
     * will be reset to 0s and another zero is added to avoid collision.
     * @param string input string
     * @return next string
     */
    public static String getNext(String string) {
        if (alphanums == null) {
            initAlphanums();
        }
        char[] input = string.toCharArray();
        boolean isUpdateSuccessful = updateChar(input);
        if (!isUpdateSuccessful) {
            return String.valueOf(input) + "0";
        }
        return String.valueOf(input);
    }

    private static boolean updateChar(char[] string) {
        var updated = false;
        for (int i = string.length-1; i >= 0; i--) {
            int alphanumIndex = alphanums.indexOf(string[i]);
            //If the current rightmost character is not Z
            if (alphanumIndex < alphanumsLength - 1) {
                string[i] = alphanums.get(alphanumIndex + 1);
                updated = true;
                break;
            } else {
                string[i] = '0';
            }
        }
        //If all the characters were Z, all of them have been reset to 0, and we are returning false to indicate this
        return updated;
    }

    private static void initAlphanums() {
        alphanums = new ArrayList<>();
        List<Character> nums = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
        List<Character> a2z = Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
                'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');
        List<Character> A2Z = Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
                'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');
        alphanums.addAll(nums);
        alphanums.addAll(a2z);
        alphanums.addAll(A2Z);
        alphanumsLength = alphanums.size();
    }
}
