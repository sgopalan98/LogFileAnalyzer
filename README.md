## Santhanagopalan Krishnamoorthy

---
## CS 441 - Engineering Distributed Objects for Cloud Computing
## Homework 3 - Http Rest and gRPC implementation in AWS

---

### Overview

The objective of this exercise is to develop two kinds of services: Http Rest and gRPC in AWS and demonstrate the usage of them. 

Explanation video: https://youtu.be/2mnSq-vMkro

### Architecture

![Architecture diagram](https://github.com/sgopalan98/LogFileAnalyzer/blob/master/src/main/resources/Architecture.jpg?raw=true)

HTTP Rest service:

- Server is hosted in AWS Lambda as a service, which is exposed as an HTTP end point using AWS API Gateway
- Client uses Akka HTTP library to call the HTTP endpoint

gRPC service:

- Server and Client is hosted locally.
- Server calls the above-mentioned HTTP end point and returns the result to the client.

### API Gateway URL

The API is deployed using AWS API Gateway at [https://3ufoc6cxd0.execute-api.us-east-2.amazonaws.com/Test/analyze](https://3ufoc6cxd0.execute-api.us-east-2.amazonaws.com/Test/analyze). 

**Sample Request**

```
curl -X GET \
  https://3ufoc6cxd0.execute-api.us-east-2.amazonaws.com/Test/analyze?time=00:00:00&dt=00:00:00
```

**Sample Response**

```json
{
    "returnValue": "5asdas52584vsdvs@@112"
}
```

### Prerequisites to build and run the project

- [SBT](https://www.scala-sbt.org/) installed on your system
- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) installed and configured on your system


## Instructions to run HttpClient

- Compile and test the project using sbt.

```
sbt clean compile test
```

- HttpClient takes in two parameters: Input-time and differential-time-period. 
- Pass these values as arguments to the class. Main class can also be run by using the default arguments provided in the application.conf

```
sbt "runMain com.loganalyzer.HttpClient 22:13:49.589 00:00:10"
```

### Example output

```
Nov 05, 2021 7:25:10 PM com.loganalyzer.HttpClient analyze
INFO: The md5 hash is74fa32de6a699b6bb3a991748dee9413
```

## Instructions to run gRPC

- Compile and test the project using sbt.

```
sbt clean compile test
```

- First, the gRPC server has to be run.

```
sbt "runMain com.loganalyzer.gRPCServer"
```

- Next, run the gRPC client with the arguments time and dt/ Use default arguments

```
sbt "runMain com.loganalyzer.gRPCClient 22:13:49.589 00:00:10"
```

### Example output

#### SERVER:

```
INFO: Server started, listening on 50051
```

#### CLIENT:

```
INFO: MD5 Hash: 74fa32de6a699b6bb3a991748dee9413
```


## Components

### EC2 Instance

- EC2 Instance generates the log file and puts it to the s3 bucket which the Http and gRPC accesses.

### Deploying the ec2 instance

1. Open the Amazon EC2 console at https://console.aws.amazon.com/ec2/.
2. Click on `Launch instance`
3. Choose AMI, Instance type, configuration
4. Download the key-pair/Choose an already existing one.
5. Connect to the instance using the key-pair.
6. Download git using yum package manager.
```
sudo yum install git
```
7. Install sbt using yum package manager.
```
sudo rm -f /etc/yum.repos.d/bintray-rpm.repo
curl -L https://www.scala-sbt.org/sbt-rpm.repo > sbt-rpm.repo
sudo mv sbt-rpm.repo /etc/yum.repos.d/
sudo yum install sbt
```
8. Clone [LogFileGenerator](https://github.com/0x1DOCD00D/LogFileGenerator) repository.
```
git clone https://github.com/0x1DOCD00D/LogFileGenerator
```
9. scp the buildAndSendLog.sh from resources in this project.
```
scp -i key-pair.pem src/main/resources/buildAndSendLog.sh ec2-user@instanceaddress:~
```
10. Modify the script to include the correct s3 bucket.
11. Run the script in the EC2 linux machine.

- This would have updated the log file in the s3 bucket.

---

### Http Rest service

- Http Rest service is hosted in Lambda, which is extended as a Http endpoint.

### Deploying the lambda service

Follow the below instructions to deploy the lambda functions on AWS.

1. Log in to your [AWS Console](https://aws.amazon.com)
2. From **Services**, search for **Lambda** and select it
3. Select **Create function**
4. In the next screen, select **Author from scratch**, and under the basic information section, specify the following and click **Create function**:
    - Function name: `LogAnalyzer`
    - Runtime: `Python3.9`
    - Role – Choose an existing role.
    - Choose Create function
    - Choose Save changes.
5. In the code section of the lambda, write your python code that parses the Http request queries and returns a Http response.

The lambda function is now deployed on AWS.

---

### Create an API Gateway endpoint to run the lambda service

1. Ensure that AWS CLI is installed and configured on your system. Follow this [guide](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) to know how to do so
2. Ensure that the user configured with your AWS CLI is having the proper IAM roles and permissions configured for modifying AWS API Gateway
3. Log in to your [AWS Console](https://aws.amazon.com)
4. From **Services**, search for **API Gateway** and select it
5. Click on **Create API**
6. Create an empty API as follows:
   1. Under Create new API, choose New API. 
   2. Under Settings:
      - For API name, enter LogFileAnalyzer. 
      - If desired, enter a description in the Description field; otherwise, leave it empty. 
      - Leave Endpoint Type set to Regional. 
   3. Choose Create API.

7. Create the helloworld resource as follows:

   1. Choose the root resource (/) in the Resources tree. 
   2. Choose Create Resource from the Actions dropdown menu. 
   3. Leave Configure as proxy resource unchecked. 
   4. For Resource Name, enter analyze. 
   5. Leave Resource Path set to /analyze. 
   6. Leave Enable API Gateway CORS unchecked. 
   7. Choose Create Resource.

8. In a proxy integration, the entire request is sent to the backend Lambda function as-is, via a catch-all ANY method that represents any HTTP method. The actual HTTP method is specified by the client at run time. The ANY method allows you to use a single API method setup for all of the supported HTTP methods: DELETE, GET, HEAD, OPTIONS, PATCH, POST, and PUT. 
   1. To set up the ANY method, do the following:
   2. In the Resources list, choose /analyze. 
   3. In the Actions menu, choose Create method. 
   4. Choose ANY from the dropdown menu, and choose the checkmark icon 
   5. Leave the Integration type set to Lambda Function. 
   6. Choose Use Lambda Proxy integration. 
   7. From the Lambda Region dropdown menu, choose the region where you created the LogAnalyzer Lambda function. 
   8. In the Lambda Function field, type any character and choose LogAnalyze from the dropdown menu. 
   9. Leave Use Default Timeout checked. 
   10. Choose Save. 
   11. Choose OK when prompted with Add Permission to Lambda Function.

9. Deploy and test the API 
   1. Deploy the API in the API Gateway console 
   2. Choose Deploy API from the Actions dropdown menu. 
   3. For Deployment stage, choose [new stage]. 
   4. For Stage name, enter test. 
   5. Choose Deploy. 
   6. Note the API's Invoke URL.

- Now Http Server is running and this can be tested using the cURL command. This can also be verified by changing the ocnfiguration in application.conf and running the HttpClient program

---

### gRPC client and server

- There is no infrastructure required to host gRPC since gRPC server runs in localhost.

#### Protobuf

This project contains the `LogAnalyzer.proto` file which defines the `LogAnalyzer` gRPC service, like so:

```proto
syntax = "proto3";

package com.loganalyzer;

// The Analyzer service definition.
service Analyzer {
  // Sends a greeting
  rpc analyzeLog (AnalyzeRequest) returns (AnalyzeReply) {}
}

// The request message containing the time and dt.
message AnalyzeRequest {
  string time = 1;
  string dt = 2;
}

// The response message containing the message
message AnalyzeReply {
  string message = 1;
}
```

The project uses [ScalaPB](https://scalapb.github.io/) to generate the stubs for the `LogAnalyzer` service and the related protobuf messages. These stubs are generated automatically when this project is compiled using `sbt compile` .


### Improvements for the future and Credits

1. Implement gRPC service on AWS using API Gateway
2. Implement communication between different components in AWS.
3. Make a cron job in EC2 to periodically generate the log file in S3.


