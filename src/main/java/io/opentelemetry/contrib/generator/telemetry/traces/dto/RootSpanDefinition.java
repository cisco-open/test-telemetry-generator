package io.opentelemetry.contrib.generator.telemetry.traces.dto;

import io.opentelemetry.contrib.generator.core.exception.GeneratorException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class RootSpanDefinition extends SpanDefinition implements Cloneable {

    private Integer payloadCount;
    private Integer copyCount;
    private Integer payloadFrequencySeconds;
    private Boolean spanErrorEndsTrace;
    @JsonIgnore
    private int treeParts; //internal value to store the number of parts in which all the spans of the trace span tree are divided
    @JsonIgnore
    private List<SpanDefinition> treeNodesPostOrder; //nodes of the trace tree in post order fashion
    @JsonIgnore
    private Map<String, Integer> spansIndexMap; //map having the position/index of each span in the trace tree

    public void validate(String requestID, int traceCompletionTimeoutSecs, Set<String> allEntityTypes, Set<String> childSpanNames) {
        super.validate(requestID, allEntityTypes, childSpanNames);

        if (payloadCount == null || payloadCount < 1) {
            throw new GeneratorException("Root span with name " + getName() + " has missing or 0 payloadCount.");
        }

        if (copyCount == null || copyCount < 1) {
            copyCount = 1;
        }

        if (spanErrorEndsTrace == null) {
            spanErrorEndsTrace = false;
        }

        if (payloadFrequencySeconds != null && payloadFrequencySeconds > 0 && payloadFrequencySeconds > traceCompletionTimeoutSecs) {
            throw new GeneratorException("The specified payload frequency " + payloadFrequencySeconds + "for root span" + getName() +
                    " is more than trace completion timeout " + traceCompletionTimeoutSecs);
        }

        if (payloadFrequencySeconds != null && payloadFrequencySeconds < 15) {
            throw new GeneratorException("The specified payload frequency " + payloadFrequencySeconds + "for root span" + getName() +
                    " is less than 15 seconds");
        }
    }

    public RootSpanDefinition clone() throws CloneNotSupportedException {
        return (RootSpanDefinition) super.clone();
    }

    @Override
    public String toString() {
        List<String> spanNamesPostOrder = treeNodesPostOrder.stream().map(SpanDefinition::getName).collect(Collectors.toList());
        return super.toString() + ", \n" +
                "postCount: " + payloadCount + ", \n" +
                "copyCount: " + copyCount + ", \n" +
                "payloadFrequencySecs: " + payloadFrequencySeconds + ", \n" +
                "treeNodesPostOrder: " + StringUtils.join(spanNamesPostOrder) + ", \n" +
                "treeParts: " + treeParts;
    }
}
