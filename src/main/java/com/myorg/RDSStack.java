package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnParameter;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.List;

public class RDSStack extends Stack {

    final DatabaseInstance databaseInstance;
    final CfnOutput rdsEndpoint;
    final CfnOutput rdsPassword;

    public RDSStack(final Construct scope, final String id, Vpc vpc) {
        super(scope, id, null);

        CfnParameter databasePassword = CfnParameter.Builder.create(this, "databasePassword")
                .type("String")
                .description("Database Description")
                .build();

        ISecurityGroup securityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

        IInstanceEngine engine = DatabaseInstanceEngine.mysql(
                MySqlInstanceEngineProps.builder()
                .version(MysqlEngineVersion.VER_5_7)
                .build()
        );
        CredentialsFromUsernameOptions credentials = CredentialsFromUsernameOptions.builder()
                .password(SecretValue.unsafePlainText(databasePassword.getValueAsString()))
                .build();
        SubnetSelection vpcSubnets = SubnetSelection.builder().subnets(vpc.getPrivateSubnets()).build();
        this.databaseInstance = DatabaseInstance.Builder
                .create(this, "Rds01")
                .instanceIdentifier("aws-project01-db")
                .engine(engine)
                .vpc(vpc)
                .credentials(Credentials.fromUsername("admin", credentials))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(List.of(securityGroup))
                .vpcSubnets(vpcSubnets)
                .build();

        this.rdsEndpoint = CfnOutput.Builder.create(this, "rds-endpoint")
                .exportName("rds-endpoint")
                .value(databaseInstance.getDbInstanceEndpointAddress())
                .build();

        this.rdsPassword = CfnOutput.Builder.create(this, "rds-password")
                .exportName("rds-password")
                .value(databasePassword.getValueAsString())
                .build();
    }
}
