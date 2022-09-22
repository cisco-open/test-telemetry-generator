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

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * Get next IPv4 string after the provided input string. Each octet consists of a number in the 1-255 range.
 * This method tries to increment the rightmost octet of the IP to the next number in order to obtain the resultant IP.
 * Once an octet reaches 255, it is reset to 1 and the octet to the left is also incremented as a result. If the input
 * IP is 255.255.255.255 the resultant IP is a reset value of 10.10.10.10.
 */
public class IPHelper {

    private IPHelper() {}

    public static String nextIPv4Address(String currentAddress) {
        Integer[] ipOctets = Arrays.stream(currentAddress.split("\\.")).
                map(Integer::parseInt).
                toArray(Integer[]::new);
        boolean isUpdateSuccessful = updateOctet(ipOctets);
        //Update was not successful means the input IP was 255.255.255.255, so we reset
        if (!isUpdateSuccessful) {
            return "10.10.10.10";
        }
        return StringUtils.join(ipOctets, ".");
    }

    private static boolean updateOctet(Integer[] octets) {
        var updated = false;
        for (var i=3; i>=0; i--) {
            //If the current rightmost octet is less than 255
            if (octets[i] < 255) {
                octets[i]++;
                updated = true;
                break;
            } else {
                octets[i] = 1;
            }
        }
        return updated;
    }
}
