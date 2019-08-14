CREATE SCHEMA `ela_cross_chain_transfer` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci ;

SET FOREIGN_KEY_CHECKS=0;

# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.5.5-10.3.13-MariaDB)
# Database: ela_cross_chain_transfer
# Generation Time: 2019-08-13 16:04:57 +0000
# ************************************************************


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
  (1,X'7573657240656C6173746F732E636F6D',X'61646D696E',X'6134313632663138633462313765366636383038646338303137346338336364',X'6433623336626263633733343062636536666461376239663235623635643361');

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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `exchange_chain` WRITE;
/*!40000 ALTER TABLE `exchange_chain` DISABLE KEYS */;

INSERT INTO `exchange_chain` (`id`, `chain_name`, `chain_url_prefix`, `elastos_chain_type`)
VALUES
  (1,X'656C61206D61696E20636861696E',X'687474703A2F2F656C612D6D61696E6E65742D6E6F64652D6C622D313430343433363438352E61702D6E6F727468656173742D312E656C622E616D617A6F6E6177732E636F6D3A3230333334',0),
  (2,X'646964207369646520636861696E',X'687474703A2F2F6469642D6D61696E6E65742D6E6F64652D6C622D313435323330393432302E61702D6E6F727468656173742D312E656C622E616D617A6F6E6177732E636F6D3A3230363034',1);

/*!40000 ALTER TABLE `exchange_chain` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table exchange_rate
# ------------------------------------------------------------

DROP TABLE IF EXISTS `exchange_rate`;

CREATE TABLE `exchange_rate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dst_chain_id` bigint(20) NOT NULL,
  `dst_chain_name` varchar(100) COLLATE utf8_bin NOT NULL,
  `fee` double NOT NULL,
  `rate` double NOT NULL,
  `src_chain_id` bigint(20) NOT NULL,
  `src_chain_name` varchar(100) COLLATE utf8_bin NOT NULL,
  `threshold_max` double NOT NULL,
  `threshold_min` double NOT NULL,
  PRIMARY KEY (`id`),
  KEY `src_chain_id_index` (`src_chain_id`),
  KEY `dst_chain_id_index` (`dst_chain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `exchange_rate` WRITE;
/*!40000 ALTER TABLE `exchange_rate` DISABLE KEYS */;

INSERT INTO `exchange_rate` (`id`, `dst_chain_id`, `dst_chain_name`, `fee`, `rate`, `src_chain_id`, `src_chain_name`, `threshold_max`, `threshold_min`)
VALUES
  (1,2,X'646964207369646520636861696E',0.0002,1,1,X'656C61206D61696E20636861696E',0.5,0.1),
  (2,1,X'656C61206D61696E20636861696E',0.0002,1,2,X'656C61207369646520636861696E',0.5,0.1);

/*!40000 ALTER TABLE `exchange_rate` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table exchange_record
# ------------------------------------------------------------

DROP TABLE IF EXISTS `exchange_record`;

CREATE TABLE `exchange_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `back_address` varchar(34) COLLATE utf8_bin NOT NULL,
  `back_txid` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `did` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `dst_address` varchar(34) COLLATE utf8_bin NOT NULL,
  `dst_chain_id` bigint(20) NOT NULL,
  `dst_txid` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `dst_value` double DEFAULT NULL,
  `fee` double NOT NULL,
  `rate` double NOT NULL,
  `src_address` varchar(34) COLLATE utf8_bin NOT NULL,
  `src_address_id` int(11) NOT NULL,
  `src_chain_id` bigint(20) NOT NULL,
  `src_value` double DEFAULT NULL,
  `src_wallet_id` bigint(20) NOT NULL,
  `state` varchar(20) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`),
  KEY `did_index` (`did`),
  KEY `src_address_index` (`src_address`),
  KEY `dst_address_index` (`dst_address`),
  KEY `state_index` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;



# Dump of table exchange_wallets
# ------------------------------------------------------------

DROP TABLE IF EXISTS `exchange_wallets`;

CREATE TABLE `exchange_wallets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `chain_id` bigint(20) DEFAULT NULL,
  `mnemonic` varchar(100) COLLATE utf8_bin NOT NULL,
  `sum` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;




# Dump of table gather_address
# ------------------------------------------------------------

DROP TABLE IF EXISTS `gather_address`;

CREATE TABLE `gather_address` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `address_id` bigint(20) NOT NULL,
  `chain_id` bigint(20) NOT NULL,
  `wallet_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;



# Dump of table internal_tx_record
# ------------------------------------------------------------

DROP TABLE IF EXISTS `internal_tx_record`;

CREATE TABLE `internal_tx_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL,
  `dst_addr` varchar(255) COLLATE utf8_bin NOT NULL,
  `dst_chain_id` bigint(20) DEFAULT NULL,
  `src_addr` varchar(255) COLLATE utf8_bin NOT NULL,
  `src_chain_id` bigint(20) DEFAULT NULL,
  `txid` varchar(64) COLLATE utf8_bin NOT NULL,
  `value` double NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `txid_index` (`txid`),
  KEY `src_addr_index` (`src_addr`),
  KEY `dst_addr_index` (`dst_addr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;



# Dump of table renewal_wallets
# ------------------------------------------------------------

DROP TABLE IF EXISTS `renewal_wallets`;

CREATE TABLE `renewal_wallets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `chain_id` bigint(20) DEFAULT NULL,
  `max_use` int(11) DEFAULT NULL,
  `mnemonic` varchar(100) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `renewal_wallets` WRITE;
/*!40000 ALTER TABLE `renewal_wallets` DISABLE KEYS */;

INSERT INTO `renewal_wallets` (`id`, `chain_id`, `max_use`, `mnemonic`)
VALUES
  (1,1,22,X'6361742067616D6520676C6164206E7572736520737461727420656E76656C6F70652076656E646F722073756464656E20656C657068616E74207175697420706C6179206465706F736974'),
  (2,2,4,X'666F72636520776174657220636174616C6F672070696720737461727420736861646F772061737369737420637265646974206C696D6220736D617274207368696E6520686F6F64');

/*!40000 ALTER TABLE `renewal_wallets` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;


