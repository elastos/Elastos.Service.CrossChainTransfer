# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.5.5-10.3.13-MariaDB)
# Database: ela_cross_chain_transfer
# Generation Time: 2019-11-29 03:11:08 +0000
# ************************************************************
CREATE SCHEMA `ela_cross_chain_transfer` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ;

SET FOREIGN_KEY_CHECKS=0;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table admin
# ------------------------------------------------------------

DROP TABLE IF EXISTS `admin`;

CREATE TABLE `admin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `nick_name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `password` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `salt` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;

INSERT INTO `admin` (`id`, `email`, `nick_name`, `password`, `salt`)
VALUES
	(1,'user@elastos.com','admin','a4162f18c4b17e6f6808dc80174c83cd','d3b36bbcc7340bce6fda7b9f25b65d3a');

/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table exchange_chain
# ------------------------------------------------------------

DROP TABLE IF EXISTS `exchange_chain`;

CREATE TABLE `exchange_chain` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `chain_name` varchar(100) COLLATE utf8_bin NOT NULL,
  `chain_url_prefix` varchar(255) COLLATE utf8_bin NOT NULL,
  `elastos_chain_type` int(11) NOT NULL,
  `is_test_net` tinyint(1) NOT NULL,
  `deposit_mnemonic` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `deposit_address_index` int(32) DEFAULT NULL,
  `threshold_min` float NOT NULL,
  `threshold_max` float NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `exchange_chain` WRITE;
/*!40000 ALTER TABLE `exchange_chain` DISABLE KEYS */;

INSERT INTO `exchange_chain` (`id`, `chain_name`, `chain_url_prefix`, `elastos_chain_type`, `is_test_net`, `deposit_mnemonic`, `deposit_address_index`, `threshold_min`, `threshold_max`)
VALUES
	(1,'ELA(main chain)','http://ela-mainnet-node-lb-1404436485.ap-northeast-1.elb.amazonaws.com:20334',0,0,'number scatter verb cube gossip toilet solve output copper credit only leisure',1,0.1,0.5),
	(2,'ELA/ETHSC(ETH side chain)','http://did-mainnet-node-lb-1452309420.ap-northeast-1.elb.amazonaws.com:20604',1,0,'flip business asset share afraid palm planet ordinary trade ketchup blood sell',1,0.1,0.5),
	(3,'ELA(DID side chain)','http://54.65.146.228:20636',2,0,'safe topic ring mask chat tourist hello can ignore margin erode gossip',1,0.1,0.5);

/*!40000 ALTER TABLE `exchange_chain` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table exchange_rate
# ------------------------------------------------------------

DROP TABLE IF EXISTS `exchange_rate`;

CREATE TABLE `exchange_rate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dst_chain_id` bigint(20) NOT NULL,
  `dst_chain_name` varchar(100) COLLATE utf8_bin NOT NULL,
  `fee_rate` double NOT NULL,
  `rate` double NOT NULL,
  `src_chain_id` bigint(20) NOT NULL,
  `src_chain_name` varchar(100) COLLATE utf8_bin NOT NULL,
  `service_min_fee` double NOT NULL,
  `threshold_max` double DEFAULT NULL,
  `threshold_min` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `src_chain_id_index` (`src_chain_id`),
  KEY `dst_chain_id_index` (`dst_chain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `exchange_rate` WRITE;
/*!40000 ALTER TABLE `exchange_rate` DISABLE KEYS */;

INSERT INTO `exchange_rate` (`id`, `dst_chain_id`, `dst_chain_name`, `fee_rate`, `rate`, `src_chain_id`, `src_chain_name`, `service_min_fee`, `threshold_max`, `threshold_min`)
VALUES
	(1,2,'ELA(DID side chain)',0.001,1,1,'ELA(main chain)',0.0003,NULL,NULL),
	(2,1,'ELA(main chain)',0.001,1,2,'ELA(DID side chain)',0.0003,NULL,NULL),
	(3,3,'ELA/ETHSC(ETH side chain)',0.001,1,1,'ELA(main chain)',0.0006,NULL,NULL),
	(4,1,'ELA(main chain)',0.001,1,3,'ELA/ETHSC(ETH side chain)',0.0006,NULL,NULL);

/*!40000 ALTER TABLE `exchange_rate` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table exchange_record
# ------------------------------------------------------------

DROP TABLE IF EXISTS `exchange_record`;

CREATE TABLE `exchange_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `back_address` varchar(256) COLLATE utf8_bin NOT NULL,
  `back_txid` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `did` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `dst_address` varchar(256) COLLATE utf8_bin NOT NULL,
  `dst_chain_id` bigint(20) NOT NULL,
  `dst_txid` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `dst_value` double DEFAULT NULL,
  `fee` double DEFAULT NULL,
  `fee_rate` double NOT NULL,
  `rate` double NOT NULL,
  `src_address` varchar(256) COLLATE utf8_bin NOT NULL,
  `src_address_id` int(11) NOT NULL,
  `src_chain_id` bigint(20) NOT NULL,
  `src_value` double DEFAULT NULL,
  `src_wallet_id` bigint(20) NOT NULL,
  `state` varchar(40) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`),
  KEY `did_index` (`did`),
  KEY `src_address_index` (`src_address`),
  KEY `dst_address_index` (`dst_address`),
  KEY `state_index` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;



# Dump of table gather_record
# ------------------------------------------------------------

DROP TABLE IF EXISTS `gather_record`;

CREATE TABLE `gather_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `address_id` int(20) NOT NULL,
  `chain_id` bigint(20) NOT NULL,
  `wallet_id` bigint(20) NOT NULL,
  `tx_hash` varchar(256) CHARACTER SET utf8 DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;



# Dump of table input_wallets
# ------------------------------------------------------------

DROP TABLE IF EXISTS `input_wallets`;

CREATE TABLE `input_wallets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `chain_id` bigint(20) DEFAULT NULL,
  `max_use` int(11) DEFAULT NULL,
  `mnemonic` varchar(256) COLLATE utf8_bin NOT NULL,
  `del` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `input_wallets` WRITE;
/*!40000 ALTER TABLE `input_wallets` DISABLE KEYS */;

INSERT INTO `input_wallets` (`id`, `chain_id`, `max_use`, `mnemonic`, `del`)
VALUES
	(1,1,11,'icon kick movie exercise crunch total unit pudding boost waste try update',0),
	(2,3,12,'combine never copper main alone shield anchor outside list strategy zone solve',0);

/*!40000 ALTER TABLE `input_wallets` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table internal_tx_record
# ------------------------------------------------------------

DROP TABLE IF EXISTS `internal_tx_record`;

CREATE TABLE `internal_tx_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL,
  `dst_addr` varchar(256) COLLATE utf8_bin NOT NULL,
  `dst_chain_id` bigint(20) DEFAULT NULL,
  `src_addr` varchar(256) COLLATE utf8_bin NOT NULL,
  `src_chain_id` bigint(20) DEFAULT NULL,
  `txid` varchar(256) COLLATE utf8_bin NOT NULL,
  `value` double NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `txid_index` (`txid`),
  KEY `src_addr_index` (`src_addr`),
  KEY `dst_addr_index` (`dst_addr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;



# Dump of table output_wallets
# ------------------------------------------------------------

DROP TABLE IF EXISTS `output_wallets`;

CREATE TABLE `output_wallets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `chain_id` bigint(20) DEFAULT NULL,
  `mnemonic` varchar(256) COLLATE utf8_bin NOT NULL,
  `sum` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `output_wallets` WRITE;
/*!40000 ALTER TABLE `output_wallets` DISABLE KEYS */;

INSERT INTO `output_wallets` (`id`, `chain_id`, `mnemonic`, `sum`)
VALUES
	(1,1,'cheap rookie elephant cool picnic minute since pink dizzy kid art merry',3),
	(3,2,'lottery miracle frost lemon tomato ready brand ice erase dismiss loan settle',3),
	(4,3,'inner soft husband involve wisdom scissors faith fetch snack render gap stable',3);

/*!40000 ALTER TABLE `output_wallets` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
