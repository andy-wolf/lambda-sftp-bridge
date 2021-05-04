# Readme

### About

The lambda-sftp-bridge is a Java implementation based on Spring Cloud Function and combined with the file copying implementation from and to either an SFTP server or an S3 bucket.

For deployment as a serverless function in AWS we use the Serverless Framework ([https://www.serverless.com/open-source/][https://www.serverless.com/open-source/]).

### Deploy

`sls deploy`


### Trace logs

`sls logs -f scheduledFunction -t`

`sls logs -f s3EventFunction -t`

### Build

`mvn clean package`


### Integration testing

The tests use [https://localstack.cloud/][https://localstack.cloud/] in order to emulate an AWS S3 bucket and [https://www.testcontainers.org/][https://www.testcontainers.org/] to run localstack.
For running the tests a local docker daemon is needed.


`mvn clean test`


[https://www.serverless.com/open-source/]: https://www.serverless.com/open-source/

[https://localstack.cloud/]: https://localstack.cloud/

[https://www.testcontainers.org/]: https://www.testcontainers.org/