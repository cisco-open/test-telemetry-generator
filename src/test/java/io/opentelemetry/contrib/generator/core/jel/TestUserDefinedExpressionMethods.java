package io.opentelemetry.contrib.generator.core.jel;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestUserDefinedExpressionMethods {

    private ExpressionProcessor jelProcessor;
    private static Map<String, Integer> counterState = new HashMap<>();

    @Test
    public void testUserDefinedResourceExpressionWithoutArgs() {
        ResourceExpressionsJELProvider.addExpression("", "",
                "io.opentelemetry.contrib.generator.core.jel.TestUserDefinedExpressionMethods", "randomUUID");
        jelProcessor = ResourceExpressionsJELProvider.getJelProcessor();
        String uuid = jelProcessor.eval("randomUUID()");
        Assert.assertNotNull(uuid);
        Assert.assertNotNull(UUID.fromString(uuid));
    }

    @Test
    public void testUserDefinedResourceExpressionWithArgs() throws NoSuchMethodException {
        ResourceExpressionsJELProvider.addExpression("", "strrev",
                this.getClass().getMethod("reverseString", String.class));
        jelProcessor = ResourceExpressionsJELProvider.getJelProcessor();
        String original = "originalString";
        String reversed = jelProcessor.eval("strrev(\"" + original + "\")");
        Assert.assertNotNull(reversed);
        Assert.assertEquals(reversed, StringUtils.reverse(original));
    }

    @Test
    public void testUserDefinedResourceExpressionWithState() {
        ResourceExpressionsJELProvider.addExpression("", "",
                "io.opentelemetry.contrib.generator.core.jel.TestUserDefinedExpressionMethods", "counterFrom");
        jelProcessor = ResourceExpressionsJELProvider.getJelProcessor();
        int initValue = jelProcessor.eval("counterFrom(\"one\", 21)");
        int nextValue = jelProcessor.eval("counterFrom(\"one\", 34)");
        int newValue = jelProcessor.eval("counterFrom(\"two\", 34)");
        int nextNewValue = jelProcessor.eval("counterFrom(\"two\", 34)");
        Assert.assertEquals(initValue, 21);
        Assert.assertEquals(nextValue, 22);
        Assert.assertEquals(newValue, 34);
        Assert.assertEquals(nextNewValue, 35);
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static String reverseString(String original) {
        return StringUtils.reverse(original);
    }

    public static int counterFrom(String stateKey, int startValue) {
        if (!counterState.containsKey(stateKey)) {
            counterState.put(stateKey, startValue);
            return startValue;
        }
        counterState.put(stateKey, counterState.get(stateKey)+1);
        return counterState.get(stateKey);
    }
}
