package io.opentelemetry.contrib.generator.telemetry.jel;

import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CustomExpressions {

    private static final ConcurrentMap<String, Integer> counterMaps = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Boolean> boolMaps = new ConcurrentHashMap<>();
    private static final List<Character> hexChars = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
            'v', 'w', 'x', 'y', 'z');

    public static int geometricWith2(String stateKey) {
        if (!counterMaps.containsKey(stateKey)) {
            counterMaps.put(stateKey, 1);
            return 1;
        }
        counterMaps.put(stateKey, counterMaps.get(stateKey) * 2);
        return counterMaps.get(stateKey);
    }

    public static synchronized int increaseByXDecreaseByY(String stateKey, int X, int Y, int startVal) {
        if (!counterMaps.containsKey(stateKey)) {
            counterMaps.put(stateKey, startVal);
            boolMaps.put(stateKey, true);
            return startVal;
        }
        if (boolMaps.get(stateKey)) {
            counterMaps.put(stateKey, counterMaps.get(stateKey) + X);
            boolMaps.put(stateKey, false);
        } else {
            counterMaps.put(stateKey, counterMaps.get(stateKey) - Y);
            boolMaps.put(stateKey, true);
        }
        return counterMaps.get(stateKey);
    }

    public static String increasingErrorSeverity(String stateKey) {
        if (!counterMaps.containsKey(stateKey)) {
            counterMaps.put(stateKey, 1);
            return "INFO";
        }
        counterMaps.put(stateKey, counterMaps.get(stateKey) + 1);
        int currCount = counterMaps.get(stateKey);
        boolean isError = currCount > 4 && currCount % 5 == 0;
        isError = isError || currCount > 8 && currCount % 4 == 0;
        isError = isError || currCount > 12 && currCount % 3 == 0;
        isError = isError || currCount > 16;
        return isError ? "ERROR" : "INFO";
    }

    public static String randomIPv6() {
        int groupLen = 4;
        StringBuilder ipv6Builer = new StringBuilder(randomHexadecimal(groupLen));
        for (int i=0; i<7; i++) {
            ipv6Builer.append(":").append(randomHexadecimal(groupLen));
        }
        return ipv6Builer.toString();
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static String randomHexadecimal(int length) {
        StringBuilder hexBuilder = new StringBuilder();
        for (int i=0; i<length; i++) {
            hexBuilder.append(hexChars.get(RandomUtils.nextInt(0, 36)));
        }
        return hexBuilder.toString();
    }
}
