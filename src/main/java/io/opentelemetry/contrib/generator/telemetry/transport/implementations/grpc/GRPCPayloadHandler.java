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

package io.opentelemetry.contrib.generator.telemetry.transport.implementations.grpc;

import io.opentelemetry.contrib.generator.telemetry.transport.PayloadHandler;
import io.opentelemetry.contrib.generator.telemetry.transport.auth.AuthHandler;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.*;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.HttpHeaders;
import java.util.stream.IntStream;

@Slf4j
public class GRPCPayloadHandler implements PayloadHandler {

    private final String HOST;
    private final int gRPCPORT;
    private final AuthHandler authHandler;
    private ManagedChannel managedChannel;
    private MetricsServiceGrpc.MetricsServiceBlockingStub metricsServiceBlockingStub;
    private LogsServiceGrpc.LogsServiceBlockingStub logsServiceBlockingStub;
    private TraceServiceGrpc.TraceServiceBlockingStub traceServiceBlockingStub;

    public GRPCPayloadHandler(String host, int gRPCPort, AuthHandler authHandler) {
        this.HOST = host;
        this.gRPCPORT = gRPCPort;
        this.authHandler = authHandler;
    }

    @Override
    public boolean postPayload(GeneratedMessageV3 message) {
        initClient();
        if (StringUtils.defaultString(HOST).isBlank() || gRPCPORT <= 0 || authHandler == null) {
            log.error("Missing HOST or PORT or AuthHandler");
            return false;
        }
        try {
            if (message instanceof ExportMetricsServiceRequest) {
                ExportMetricsServiceResponse response = metricsServiceBlockingStub.export((ExportMetricsServiceRequest) message);
                log.debug(response.toString());
            } else if (message instanceof ExportLogsServiceRequest) {
                ExportLogsServiceResponse response = logsServiceBlockingStub.export((ExportLogsServiceRequest) message);
                log.debug(response.toString());
            } else {
                ExportTraceServiceResponse response = traceServiceBlockingStub.export((ExportTraceServiceRequest) message);
                log.debug(response.toString());
            }
            log.info("gRPC message export succeeded");
            return true;
        } catch (StatusRuntimeException grpcException) {
            log.error("gRPC exception occurred while exporting message", grpcException);
        } catch (Exception exception) {
            log.error("Unknown exception occurred while exporting gRPC message", exception);
        }
        return false;
    }

    private void initClient() {
        if (managedChannel == null) {
            initGRPC();
        }
    }

    private void initGRPC() {
        managedChannel = initManagedChannel();
        metricsServiceBlockingStub = MetricsServiceGrpc.newBlockingStub(managedChannel);
        logsServiceBlockingStub = LogsServiceGrpc.newBlockingStub(managedChannel);
        traceServiceBlockingStub = TraceServiceGrpc.newBlockingStub(managedChannel);
    }

    private ManagedChannel initManagedChannel() {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(HOST, gRPCPORT);
        String[] postDataHeaders = getHeadersPostData();
        channelBuilder.intercept(new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        IntStream.range(0, postDataHeaders.length/2).forEach(i ->
                                headers.put(Metadata.Key.of(postDataHeaders[i*2], Metadata.ASCII_STRING_MARSHALLER), postDataHeaders[i*2+1]));
                        super.start(responseListener, headers);
                    }
                };
            }
        });
        return channelBuilder.build();
    }

    protected String[] getHeadersPostData() {
        return new String[] {HttpHeaders.AUTHORIZATION, authHandler.getAuthString()};
    }
}
