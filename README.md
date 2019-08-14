ELASTOS CROSS CHAIN TRANSFER SERVICE
==============

## Summary

This repo provide services for elastos exchangeRate platform.

## Build with maven

In project directory, use maven command:
```Shell
$uname mvn clean compile package
```
If there is build success, Then the package micro.services.api-0.0.1.jar will be in target directory.

## Configure project properties
In project directory, create configuration file from the template:

```bash
$ pushd src/main/resources
$ cp -v application.properties.in application.properties
$ popd
```

### Configure database
First create database table use sql file in project: micro_services.sql

Change spring.datasource to your database.like:
```yaml
spring.datasource.url=jdbc:mariadb://localhost:3306/micro_services?useUnicode=true&characterEncoding=UTF-8&useSSL=false
spring.datasource.username=root
spring.datasource.password=12345678
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
```

### Configure did side chain address
Change "node.didPrefix" to your did side chain node url.

### Configure did up chain wallets sum 
Change "wallet.sum" to change the amount of the wallets to be used to up chain. 
the more up chain wallets the more up chain exchangeRecord at the same time and the more resource to be used.
So we recommend the number is bigger than 100 and less than 1000.

### Configure provenance property
config provenance did info and a path to save provenance file info:

```yaml
## provenance did
provenance.didPriKey = E12471FA668968F1CB71FAFB755FCFA5B17799AEE386A80AACC9458521818CF7
provenance.didPubKey  = 03133599DBEC3A6EE7F8C18E32265A76BECE2C7481BEABFD79399A11C57A2FB77B
provenance.did       = ijVhhgNd1Sq3vL3QtNVjZFEoXzdv962fnZ
## provenance file save path
provenance.saveAddr   = /var/elastos/microservice/provenancefile/
## download file expire time (min)
provenance.fileExpire = 1 
```
Change "node.didPrefix" to your did side chain node url.

## Run

Copy did.chain.exchangeRate-0.0.1.jar to your deploy directory.
then use jar command to run this spring boot application.

```shell
$uname java -jar did.chain.agent-0.0.1.jar
```
