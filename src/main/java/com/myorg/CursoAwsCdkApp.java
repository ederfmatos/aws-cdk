package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class CursoAwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        VpcStack vpcStack = new VpcStack(app, "Vpc");

        ECSClusterStack clusterStack = new ECSClusterStack(app, "Cluster", vpcStack.vpc);
        clusterStack.addDependency(vpcStack);

        RDSStack rdsStack = new RDSStack(app, "Rds", vpcStack.vpc);
        rdsStack.addDependency(vpcStack);

        Service01 service01 = new Service01(app, "Service01", clusterStack.cluster);
        service01.addDependency(clusterStack);
        service01.addDependency(rdsStack);

        app.synth();
    }
}

