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
# Generation Time: 2019-10-24 15:40:11 +0000
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

#todo Change admin password on product
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `exchange_chain` WRITE;
/*!40000 ALTER TABLE `exchange_chain` DISABLE KEYS */;

INSERT INTO `exchange_chain` (`id`, `chain_name`, `chain_url_prefix`, `elastos_chain_type`)
VALUES
  (1,'ela main chain','http://54.64.220.165:21334',0),
  (2,'did side chain','http://54.64.220.165:21604',1),
  (3,'eth side chain','http://rpc.elaeth.io:8545',2);

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
  `threshold_max` double NOT NULL,
  `threshold_min` double NOT NULL,
  `service_min_fee` double NOT NULL,
  PRIMARY KEY (`id`),
  KEY `src_chain_id_index` (`src_chain_id`),
  KEY `dst_chain_id_index` (`dst_chain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

LOCK TABLES `exchange_rate` WRITE;
/*!40000 ALTER TABLE `exchange_rate` DISABLE KEYS */;

INSERT INTO `exchange_rate` (`id`, `dst_chain_id`, `dst_chain_name`, `fee_rate`, `rate`, `src_chain_id`, `src_chain_name`, `threshold_max`, `threshold_min`, `service_min_fee`)
VALUES
  (1,2,'did side chain',0.001,1,1,'ela main chain',0.5,0.1,0.0003),
  (2,1,'ela main chain',0.001,1,2,'did side chain',0.5,0.1,0.0003),
  (3,3,'eth side chain',0.001,1,1,'ela main chain',0.5,0.1,0.0006),
  (4,1,'ela main chain',0.001,1,3,'eth side chain',0.5,0.1,0.0006);

/*!40000 ALTER TABLE `exchange_rate` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;


