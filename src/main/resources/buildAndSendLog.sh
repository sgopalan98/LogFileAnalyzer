cd LogFileGenerator
sbt clean compile assembly
cd ..
echo "Going to send the file"
aws s3 cp LogFileGenerator/log/LogFileGenerator.log  s3://logstats-441