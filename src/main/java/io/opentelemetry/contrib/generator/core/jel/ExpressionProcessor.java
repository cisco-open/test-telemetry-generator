package io.opentelemetry.contrib.generator.core.jel;

import jakarta.el.ELProcessor;
import jakarta.el.PropertyNotFoundException;

public class ExpressionProcessor extends ELProcessor {

    public ExpressionProcessor() {
        super();
    }

    public <T> T eval(String expression) {
        try {
            return super.eval(expression);
        } catch (PropertyNotFoundException exception) {
            return (T) expression;
        }
    }
}
