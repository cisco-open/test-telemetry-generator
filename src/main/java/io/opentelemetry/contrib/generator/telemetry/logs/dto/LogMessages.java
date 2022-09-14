package io.opentelemetry.contrib.generator.telemetry.logs.dto;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
@Data
public class LogMessages {

    public static List<String> normalSeverityReasons = Arrays.asList("Synced:RESOURCE_TYPE synced successfully",
            "SuccessfulCreate:Create RESOURCE_TYPE successful",
            "SuccessfulUpdate:Update RESOURCE_TYPE successful",
            "SuccessfulDelete:Delete RESOURCE_TYPE successful",
            "WaitForFirstConsumer:waiting for first consumer to be created before binding",
            "Started:Started RESOURCE_TYPE",
            "Killing:Killing RESOURCE_TYPE");
    public static List<String> warningSeverityReasons = Arrays.asList("NetworkNotReady:network is not ready",
            "Failed:Error making RESOURCE_TYPE metadata",
            "FailedSync:Error determining status",
            "PortNotAllocated:Port 7841 is not allocated; repairing",
            "InvalidNodeInfo:Kubelet cannot get node info");
    public static List<String> severeSeverityReasons = Arrays.asList("InvalidDiskCapacity:invalid capacity 0 on image filesystem",
            "HostNetworkNotSupported:Host Network not supported",
            "FailedCreate:Failed to create RESOURCE_TYPE",
            "FailedDelete:Failed to delete RESOURCE_TYPE",
            "FailedUpdate:Failed to update RESOURCE_TYPE",
            "PortAlreadyAllocated:Port 2130 was assigned to multiple services; please recreate service",
            "PortOutOfRange:Port 71600 is not within the port range; please recreate service",
            "UnknownError:Unable to allocate port 8001 due to an unknown error");

    public static List<String> infoMessages = Arrays.asList("Getting artifact:Pulling artifact from repository",
            "Ready:RESOURCE_TYPE ready for operation",
            "Started:Started operation successfully for RESOURCE_TYPE in 35 ms.",
            "Completed:Completed operation successfully for RESOURCE_TYPE in 53 s.",
            "Registration successful:Registration with central monitor successful. Will proceed for subsequent processing for RESOURCE_TYPE",
            "Validation successful:Config validation successful for RESOURCE_TYPE. Loading configuration before system initialization");
    public static List<String> warnMessages = Arrays.asList("Validation failed:Validation failed for RESOURCE_TYPE",
            "Precheck failed:Pre-check failed for RESOURCE_TYPE",
            "Missing config:Missing configuration variables for RESOURCE_TYPE in system",
            "Start failed:RESOURCE_TYPE will be rebooted in 60 s, cannot start process",
            "Sync failed:Failed to sync RESOURCE_TYPE with the public repository server. Retrying with a longer timeout of 180 s.",
            "Cannot connect:Cannot connect to the specified URL at https://www.appdynamics.com. Check your network connection and retry");
    public static List<String> errorMessages = Arrays.asList("Start failed:Failed to create/start RESOURCE_TYPE",
            "Unknown error:Unknown error occurred at line 93",
            "Timeout:Timed out waiting for RESOURCE_TYPE to be ready after 60 s",
            "Invalid address:Invalid IPv6 address provided in configuration",
            "Cannot delete:Unable to delete resource RESOURCE_TYPE from the system as it still has running processes. Retry after some time",
            "Out of space:No space left on storage disk device. Unable to proceed without disk space, please clean and retry. Aborting.");
    public static List<String> debugMessages = Arrays.asList("Config load:Loading config from file system",
            "Not found:Wireless not found, using ethernet",
            "Init complete:Initialization of RESOURCE_TYPE completed in 215 s in normal mode",
            "Processing:Processed 12 of 32 of incoming requests for RESOURCE_TYPE",
            "Processing:765 packets processed, 32 packets rejected and 6 are in processing. See log file on disk for details on rejected packets",
            "Connected:Connected to 3 peers at [114.199.241.147, 45.246.30.180, 240.201.30.156]. Average upload 315kBps Average download 980kBps");

}
