package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class Service01 extends Stack {
    final ApplicationLoadBalancedFargateService service01;

    public Service01(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service01(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);

        LogGroup logGroup = LogGroup.Builder.create(this, "Service01LogGroup")
                .logGroupName("Service01")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        AwsLogDriverProps logDriverProps = AwsLogDriverProps.builder()
                .logGroup(logGroup)
                .streamPrefix("Service01")
                .build();

        ApplicationLoadBalancedTaskImageOptions taskImageOptions = ApplicationLoadBalancedTaskImageOptions.builder()
                .containerName("aws_project_01")
                .image(ContainerImage.fromRegistry("ederfmatos/product-api:1.0.1"))
                .containerPort(8080)
                .logDriver(LogDriver.awsLogs(logDriverProps))
                .build();

        this.service01 = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB01")
                .serviceName("service-01")
                .cluster(cluster)
                .cpu(512)
                .desiredCount(2)
                .listenerPort(8080)
                .memoryLimitMiB(1024)
                .taskImageOptions(taskImageOptions)
                .publicLoadBalancer(true)
                .build();

        HealthCheck healthCheck = new HealthCheck.Builder()
                .path("/actuator/health")
                .port("8080")
                .healthyHttpCodes("200")
                .build();

        service01.getTargetGroup().configureHealthCheck(healthCheck);

        EnableScalingProps scalingProps = EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build();
        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(scalingProps);

        CpuUtilizationScalingProps cpuUtilizationScalingProps = CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.minutes(1))
                .scaleOutCooldown(Duration.minutes(1))
                .build();
        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", cpuUtilizationScalingProps);
    }
}
