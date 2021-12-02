import json
import boto3
from datetime import datetime, timedelta
from time import strftime
import hashlib
import re

s3 = boto3.client('s3')

def lambda_handler(event, context):
    bucket = 'logstats-441'
    key = 'LogFileGenerator.log'
    format =  "%H:%M:%S.%f"
    returnValue = False
    

    try:
        data = s3.get_object(Bucket=bucket, Key=key)
        json_data = data['Body'].read().decode('utf-8')

        inputTime = datetime.strptime(event["queryStringParameters"]['time'], format )
        inputTimeList = (event["queryStringParameters"]['dt']).split(":")

        startTime = (inputTime - timedelta(hours=int(inputTimeList[0])) - timedelta(minutes=int(inputTimeList[1])) - timedelta(seconds=int(inputTimeList[2]))).strftime(format)
        endTime = (inputTime + timedelta(hours=int(inputTimeList[0])) + timedelta(minutes=int(inputTimeList[1])) + timedelta(seconds=int(inputTimeList[2]))).strftime(format)

        logData = json_data.splitlines()
        startValue = 0
        endValue = len(logData) - 1
        print(startTime, endTime)
        while startValue <= endValue:
            midValue = (startValue + endValue) // 2
            timestampValue = logData[midValue].split(" ")[0].split(".")[0]
            if startTime > timestampValue:
                startValue = midValue + 1
            elif endTime < timestampValue:
                endValue = midValue - 1
            else:
                returnValue = True
                break
        
        finalMessage = ""
        if returnValue:
            logList = logData[startValue:endValue+1]
            regex = r"([a-c][e-g][0-3]|[A-Z][5-9][f-w]){5,15}"
            
            for logMessage in logList:
                print(logMessage)
                if(re.search(regex, logMessage)):
                    result = hashlib.md5(logMessage.encode())
                    finalMessage = result.hexdigest()
                    print(finalMessage)
                    break
        
        
        # 2. Construct the body of the response object
        
        transactionResponse = {}
        if returnValue:
            transactionResponse['returnValue'] = str(finalMessage)
        else:
            transactionResponse['returnValue'] = "NOTFOUND"

        # Construct http resonse
        if returnValue:
            responseObject = {}
            responseObject['statusCode'] = 200
            responseObject['headers'] = {}
            responseObject['headers']['Content-Type'] = 'application/json'
            responseObject['body'] = json.dumps(transactionResponse)
        else:
            responseObject = {}
            responseObject['statusCode'] = 404
            responseObject['headers'] = {}
            responseObject['headers']['Content-Type'] = 'application/json'
            responseObject['body'] = json.dumps(transactionResponse)

        # 4. Return the response object
        return responseObject

    except Exception as e:
        print(e)
        raise e
