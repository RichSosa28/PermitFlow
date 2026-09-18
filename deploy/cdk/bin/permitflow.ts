import * as cdk from 'aws-cdk-lib';
import { PermitflowStack } from '../lib/permitflow-stack.js';
const app = new cdk.App();
new PermitflowStack(app, 'PermitflowStack');
