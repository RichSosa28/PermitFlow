import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecsPatterns from 'aws-cdk-lib/aws-ecs-patterns';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secrets from 'aws-cdk-lib/aws-secretsmanager';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cwActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';

export class PermitflowStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);
    const vpc = new ec2.Vpc(this, 'Vpc', { maxAzs: 2, natGateways: 1 });
    const db = new rds.DatabaseInstance(this, 'Database', {
      engine: rds.DatabaseInstanceEngine.postgres({ version: rds.PostgresEngineVersion.VER_16 }),
      vpc, allocatedStorage: 20, maxAllocatedStorage: 50, multiAz: false,
      credentials: rds.Credentials.fromGeneratedSecret('permitflow'), databaseName: 'permitflow',
      deletionProtection: false, removalPolicy: cdk.RemovalPolicy.SNAPSHOT
    });
    const jwtSecret = new secrets.Secret(this, 'JwtSecret', { generateSecretString: { passwordLength: 48, excludePunctuation: true } });
    const cluster = new ecs.Cluster(this, 'Cluster', { vpc });
    const repository = ecr.Repository.fromRepositoryName(this, 'Repository', 'permitflow');
    const service = new ecsPatterns.ApplicationLoadBalancedFargateService(this, 'Api', {
      cluster, publicLoadBalancer: true, desiredCount: 2, cpu: 512, memoryLimitMiB: 1024,
      taskImageOptions: {
        image: ecs.ContainerImage.fromEcrRepository(repository, 'latest'), containerPort: 8080,
        environment: { DATABASE_URL: `jdbc:postgresql://${db.dbInstanceEndpointAddress}:${db.dbInstanceEndpointPort}/permitflow`, DATABASE_USER: 'permitflow' },
        secrets: { DATABASE_PASSWORD: ecs.Secret.fromSecretsManager(db.secret!, 'password'), JWT_SECRET: ecs.Secret.fromSecretsManager(jwtSecret) },
        logDriver: ecs.LogDrivers.awsLogs({ streamPrefix: 'api', logRetention: logs.RetentionDays.ONE_MONTH })
      }
    });
    db.connections.allowDefaultPortFrom(service.service, 'API to PostgreSQL');
    service.targetGroup.configureHealthCheck({ path: '/actuator/health' });
    const topic = new sns.Topic(this, 'OperationsAlarmTopic');
    new cloudwatch.Alarm(this, 'Alb5xxAlarm', { metric: service.loadBalancer.metrics.httpCodeElb(elbv2.HttpCodeElb.ELB_5XX_COUNT, { period: cdk.Duration.minutes(5) }), threshold: 5, evaluationPeriods: 1 }).addAlarmAction(new cwActions.SnsAction(topic));
    new cdk.CfnOutput(this, 'ApiUrl', { value: `http://${service.loadBalancer.loadBalancerDnsName}` });
  }
}
