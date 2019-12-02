ELASTOS CROSS CHAIN TRANSFER SERVICE
==============

## Summary

This repo provide services for elastos exchangeRate platform.

## Build with maven

In project directory, use maven command:
```Shell
$uname mvn clean compile package
```
If there is build success, Then the package cross.chain.transfer-0.0.1.jar will be in target directory.

## Configure project properties
In project directory, create configuration file from the template:

```bash
$ pushd src/main/resources
$ cp -v application.properties.in application.properties
$ popd
```

### Configure database
First create database table use sql file in project: ela_cross_chain_transfer.sql

Change spring.datasource to your database.like:
```yaml
spring.datasource.url=jdbc:mariadb://localhost:3306/ela_cross_chain_transfer?useUnicode=true&characterEncoding=UTF-8&useSSL=false
spring.datasource.username=root
spring.datasource.password=12345678
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
```

### Configure redis 

```yaml
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0
spring.redis.timeout=10000
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-wait=-1
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=0
```


### Configure transaction basic
config transaction basic information

```yaml
txbasic.ELA_CROSS_CHAIN_SERVICE_MIN_FEE=0.0003 //Cross chain service min fee per time.
txbasic.RENEWAL_TIMEOUT=1                          //Wait user renewal time out (hour)
txbasic.OUTPUT_ADDRESS_SUM=3               //Fast transfer wallet address number.
txbasic.OUTPUT_ADDRESS_SUPPLY_THRESHOLD=0.5 // The fast transfer wallet address value is less than this value, we recharge it.
txbasic.OUTPUT_ADDRESS_CAPABILITY=1.0   //We recharge wallet address value
txbasic.ELA_SAME_CHAIN_TRANSFER_WAIT=3        //if a same chain transfer is send to node, we wait time for it on chain block. (minutes)
txbasic.ELA_CROSS_CHAIN_TRANSFER_WAIT=15  //if a cross chain transfer is send to node, we wait for it on chain block. (minutes)
```

### Configure balance wallet 
config balance wallet for recharge worker wallet address

```yaml
deposit.privateKey=17f9885d36ce7c646cd1d613708e9b375f81b81309fbdfbd922d0cd72faadb1b
deposit.address=EJqsNp9qSWkX7wkkKeKnqeubok6FxuA9un // We renewal ela to this wallet address for whole system.
deposit.renewalCapability=1 //We can renewal how many times for every same chain worker wallet address
```

## Run

Copy cross.chain.transfer-0.0.1.jar to your deploy directory.
then use jar command to run this spring boot application.

```shell
java -jar cross.chain.transfer-0.0.1.jar 
```
