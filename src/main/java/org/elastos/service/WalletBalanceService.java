package org.elastos.service;

import com.alibaba.fastjson.JSON;
import jnr.ffi.annotations.Synchronized;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.DepositConfiguration;
import org.elastos.conf.NodeConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constants.RetCode;
import org.elastos.dao.GatherAddressRepository;
import org.elastos.dao.InternalTxRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.dto.GatherAddress;
import org.elastos.dto.InternalTxRecord;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.ServerResponse;
import org.elastos.util.SynPairSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
@Service
public class WalletBalanceService {

    private static Logger logger = LoggerFactory.getLogger(WalletBalanceService.class);

    private Map<Long, SynPairSet<ElaWalletAddress>> renewalAddressMap = new HashMap<>();
    private Map<Long, SynPairSet<ElaWalletAddress>> exchangeAddressMap = new HashMap<>();

    private Integer c = 0;

    @Autowired
    DepositConfiguration depositConfiguration;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    @Autowired
    NodeConfiguration nodeConfiguration;

    @Autowired
    GatherAddressRepository gatherAddressRepository;

    @Autowired
    InternalTxRepository internalTxRepository;

    @Autowired
    ChainService chainService;

    @Autowired
    RenewalWalletService renewalWalletService;

    @Autowired
    ExchangeWalletsService exchangeWalletsService;

    @Autowired
    private ScheduledTaskExchange scheduledTaskExchange;

    @Autowired
    private ScheduledTaskBalance scheduledTaskBalance;

    void initService() {
        renewalAddressMap.clear();
        exchangeAddressMap.clear();

        List<ExchangeChain> chainList = chainService.getChains();

        for (ExchangeChain chain : chainList) {
            SynPairSet<ElaWalletAddress> renewal = new SynPairSet<>();
            renewal.init();
            renewalAddressMap.put(chain.getId(), renewal);
            SynPairSet<ElaWalletAddress> exchange = new SynPairSet<>();
            exchange.init();
            exchangeAddressMap.put(chain.getId(), exchange);
        }
    }


    private void save2RenewalAddress(Long chainId, ElaWalletAddress address) {
        SynPairSet<ElaWalletAddress> pairSet = renewalAddressMap.get(chainId);
        pairSet.save2Set(address);
    }

    @Synchronized
    void UserInputToMainDepositTask() {
        //todo every chain gather to self chain deposit, then gather to main chain
        List<GatherAddress> gatherAddresses = (List<GatherAddress>) gatherAddressRepository.findAll();
        if (gatherAddresses.isEmpty()) {
            return;
        }

        for (GatherAddress ga : gatherAddresses) {
            ElaWalletAddress address = renewalWalletService.findAddress(ga.getWalletId(), ga.getAddressId().intValue());
            if (null == address) {
                logger.error("Err UserInputToMainDepositTask findAddress failed");
                continue;
            }
            Double value = chainService.getBalancesByAddr(ga.getChainId(), address.getPublicAddress());
            if (value > txBasicConfiguration.getELA_FEE()) {
                address.setRest(value);
                save2RenewalAddress(ga.getChainId(), address);
            } else {
                gatherAddressRepository.deleteById(ga.getId());
            }
        }

        boolean transferFlag = false;
        for (Map.Entry<Long, SynPairSet<ElaWalletAddress>> entry : renewalAddressMap.entrySet()) {
            Long chainId = entry.getKey();
            ExchangeChain chain = chainService.getChain(chainId);
            Set<ElaWalletAddress> renewalSet = entry.getValue().useSet();
            if (renewalSet.isEmpty()) {
                continue;
            }

            ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
            List<String> priKeyList = new ArrayList<>();
            Double value = 0.0;
            for (ElaWalletAddress address : renewalSet) {
                Double v = address.getRest();
                value += v;
                priKeyList.add(address.getPrivateKey());
            }

            //If there is cross chain transaction
            if (chain.getType().equals(ElaChainType.ELA_CHAIN)) {
                value -= txBasicConfiguration.getELA_FEE();
            } else {
                value -= txBasicConfiguration.getELA_CROSS_CHAIN_FEE() * 2;
            }
            Map<String, Double> dstMap = new HashMap<>();
            dstMap.put(depositConfiguration.getAddress(), value);

            ReturnMsgEntity ret = elaDidService.transferEla(
                    chain.getType(), priKeyList,
                    ElaChainType.ELA_CHAIN, dstMap);
            if (ret.getStatus() != RetCode.SUCCESS) {
                entry.getValue().saveAll2Set(renewalSet);
                logger.error("UserInputToMainDepositTask tx failed chainId:" + chainId + " result:" + ret.getResult());
            } else {
                InternalTxRecord internalTxRecord = new InternalTxRecord();
                internalTxRecord.setSrcChainId(chainId);
                internalTxRecord.setSrcAddr(JSON.toJSONString(renewalSet));
                internalTxRecord.setDstChainId(chainService.getChain(ElaChainType.ELA_CHAIN).getId());
                internalTxRecord.setDstAddr(depositConfiguration.getAddress());
                internalTxRecord.setTxid((String) ret.getResult());
                internalTxRecord.setValue(value);
                internalTxRepository.save(internalTxRecord);
                renewalSet.clear();
                transferFlag = true;
            }
        }

        //Wait all transfer of renewal address to deposit address done.
        if (transferFlag) {
            try {
                TimeUnit.MINUTES.sleep(txBasicConfiguration.getELA_CROSS_CHAIN_TRANSFER_WAIT());
            } catch (InterruptedException e) {
                logger.info("UserInputToMainDepositTask interrupted.");
            }
        }
    }

    void save2ExchangeAddress(Long chainId, ElaWalletAddress address) {
        SynPairSet<ElaWalletAddress> pairSet = exchangeAddressMap.get(chainId);
        pairSet.save2Set(address);
    }

    @Synchronized
    void EveryDepositToExchangeTask() {
        for (Map.Entry<Long, SynPairSet<ElaWalletAddress>> entry : exchangeAddressMap.entrySet()) {
            Long chainId = entry.getKey();
            ExchangeChain chain = chainService.getChain(chainId);
            Set<ElaWalletAddress> exchangeSet = entry.getValue().useSet();
            //There is no need to renewal worker addresses
            if (exchangeSet.isEmpty()) {
                continue;
            }

            Double rest = chainService.getBalancesByAddr(chain, depositConfiguration.getAddress());
            if (null == rest) {
                continue;
            }

            Double fullRenewal = 0.0;
            Map<String, Double> dstMap = new HashMap<>();
            for (ElaWalletAddress address : exchangeSet) {
                Double addRest = chainService.getBalancesByAddr(chain, address.getPublicAddress());
                if (null == addRest) {
                    continue;
                }
                Double v = txBasicConfiguration.getWORKER_ADDRESS_RENEWAL_VALUE() - addRest;
                if (0.0 < v) {
                    dstMap.put(address.getPublicAddress(), v);
                    fullRenewal += v;
                }
            }

            if (dstMap.isEmpty()) {
                //No need to add anything
                continue;
            }

            //If this chain deposit address has not enough ela, we wait for renewal it first.
            if (rest < fullRenewal) {
                entry.getValue().saveAll2Set(exchangeSet);
                continue;
            }

            //Then we renewal worker addresses.
            List<String> priKeyList = new ArrayList<>();
            priKeyList.add(depositConfiguration.getPrivateKey());

            ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
            ReturnMsgEntity ret = elaDidService.transferEla(
                    chain.getType(), priKeyList,
                    chain.getType(), dstMap);
            if (ret.getStatus() != RetCode.SUCCESS) {
                entry.getValue().saveAll2Set(exchangeSet);
                logger.error("EveryDepositToExchangeTask tx failed chainId:" + chainId + " result:" + ret.getResult());
            } else {

                String txid = (String) ret.getResult();
                InternalTxRecord internalTxRecord = new InternalTxRecord();
                internalTxRecord.setSrcChainId(chainId);
                internalTxRecord.setSrcAddr(depositConfiguration.getAddress());
                internalTxRecord.setDstChainId(chainId);
                internalTxRecord.setDstAddr(JSON.toJSONString(exchangeSet));
                internalTxRecord.setTxid(txid);
                internalTxRecord.setValue(fullRenewal);
                internalTxRepository.save(internalTxRecord);
                exchangeSet.clear();
                Object waitTxFinish = waitTxFinish(chain, txid, 3, txBasicConfiguration.getELA_SAME_CHAIN_TRANSFER_WAIT());
                if (null == waitTxFinish) {
                    logger.error("EveryDepositToExchangeTask wait main chain to side chain tx failed. txid:" + txid);
                }
            }
        }
    }

    private Object waitTxFinish(ExchangeChain chain, String txid, int waitSum, int waitTime) {
        for (int i = 0; i < waitSum; i++) {
            try {
                TimeUnit.MINUTES.sleep(waitTime);
            } catch (InterruptedException e) {
                logger.info("waitTxFinish interrupted");
            }
            Object o = chainService.getTransaction(chain.getId(), txid);
            logger.debug("waitTxFinish ing " + i);
            if (null != o) {
                logger.debug("waitTxFinish ok " + i);
                return o;
            }
        }
        return null;
    }

    @Synchronized
    void MainDepositToSideChainDepositTask() {
        c++;
        logger.debug("MainDepositToSideChainDepositTask in" + c);

        ExchangeChain mainChain = chainService.getChain(ElaChainType.ELA_CHAIN);
        if (null == mainChain) {
            logger.error("Err MainDepositToSideChainDepositTask there is no main chain!!!");
            return;
        }

        Double fullRenewal = depositConfiguration.getRenewalCapability() * txBasicConfiguration.getWORKER_ADDRESS_RENEWAL_VALUE() * txBasicConfiguration.getWORKER_ADDRESS_SUM();
        fullRenewal += depositConfiguration.getRenewalCapability() * txBasicConfiguration.getELA_FEE();

        Double mainRest = chainService.getBalancesByAddr(mainChain, depositConfiguration.getAddress());
        if (mainRest == null) {
            logger.info("MainDepositToSideChainDepositTask chainService.getBalancesByAddr failed main chain.");
            return;
        }
        if (mainRest <= fullRenewal) {
            logger.info("MainDepositToSideChainDepositTask there is not enough ela in main chain.");
            return;
        }

        List<ExchangeChain> chains = chainService.getChains();
        for (ExchangeChain chain : chains) {
            if (chain.getType().equals(ElaChainType.ELA_CHAIN) || (chain.getType()==ElaChainType.ETH_CHAIN)) {
                //We do not renewal main chain deposit here
                //We do not proc eth now
                continue;
            }

            Double sideRest = chainService.getBalancesByAddr(chain, depositConfiguration.getAddress());
            if (null == sideRest) {
                logger.info("Err MainDepositToSideChainDepositTask chainService.getBalancesByAddr chain:" + chain.getType());
               continue;
            }
            if (sideRest >= fullRenewal) {
                //There is enough rest in this chain deposit address, no need to renewal.
                continue;
            }

            mainRest = chainService.getBalancesByAddr(mainChain, depositConfiguration.getAddress());
            if (null == mainRest) {
                logger.info("Err MainDepositToSideChainDepositTask chainService.getBalancesByAddr main chain address:" + depositConfiguration.getAddress());
                continue;
            }
            if (mainRest <= fullRenewal) {
                logger.info("Err MainDepositToSideChainDepositTask there is not enough ela in main chain!!!");
                continue;
            }

            List<String> depositPriKeyList = new ArrayList<>();
            depositPriKeyList.add(depositConfiguration.getPrivateKey());
            Map<String, Double> depositMap = new HashMap<>();
            depositMap.put(depositConfiguration.getAddress(), fullRenewal - sideRest);
            logger.debug("MainDepositToSideChainDepositTask elaDidService" + c);
            ElaDidService elaDidService = new ElaDidService(mainChain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
            ReturnMsgEntity ret = elaDidService.transferEla(
                    ElaChainType.ELA_CHAIN, depositPriKeyList,
                    chain.getType(), depositMap);
            if (ret.getStatus() != RetCode.SUCCESS) {
                logger.error("MainDepositToSideChainDepositTask main chain to side chain deposit failed chainId:" + chain.getId() + " result:" + ret.getResult());
                logger.debug("MainDepositToSideChainDepositTask elaDidService failed" + c);
                continue;
            }
            String txid = (String) ret.getResult();
            InternalTxRecord internalTxRecord = new InternalTxRecord();
            internalTxRecord.setSrcChainId(mainChain.getId());
            internalTxRecord.setSrcAddr(depositConfiguration.getAddress());
            internalTxRecord.setDstChainId(chain.getId());
            internalTxRecord.setDstAddr(depositConfiguration.getAddress());
            internalTxRecord.setTxid(txid);
            internalTxRecord.setValue(fullRenewal - sideRest);
            internalTxRepository.save(internalTxRecord);

            //There is only main chain deposit address as a source, we must wait the tx is finish, then we renewal another chain deposit.
            logger.debug("MainDepositToSideChainDepositTask waitTxFinish start" + c);
            Object waitTxFinish = waitTxFinish(chain, txid, 3, txBasicConfiguration.getELA_CROSS_CHAIN_TRANSFER_WAIT());
            logger.debug("MainDepositToSideChainDepositTask waitTxFinish end" + c);
            if (null == waitTxFinish) {
                logger.error("MainDepositToSideChainDepositTask wait main chain to side chain tx failed. txid:" + txid);
            }
        }
        logger.debug("MainDepositToSideChainDepositTask out" + c);
    }


    private Double gatherAllDeposit() {
        List<ExchangeChain> chains = chainService.getChains();
        Double gatherValue = 0.0;
        ExchangeChain mainChain = chainService.getChain(ElaChainType.ELA_CHAIN);
        if (null == mainChain) {
            logger.error("Err gatherAllDeposit there is no main chain!!!");
            return gatherValue;
        }
        for (ExchangeChain chain : chains) {
            if (chain.getType().equals(ElaChainType.ELA_CHAIN)) {
                //We do not gather main chain deposit here
                continue;
            }

            Double sideRest = chainService.getBalancesByAddr(chain, depositConfiguration.getAddress());
            if (null == sideRest) {
                logger.info("gatherAllDeposit failed chainService.getBalancesByAddr:" + depositConfiguration.getAddress());
                continue;
            }
            if (txBasicConfiguration.getELA_CROSS_CHAIN_FEE() * 2 > sideRest) {
                //There is not enough rest in this chain deposit address, no need to gather.
                logger.info("gatherAllDeposit There is no ela in chainId:" + chain.getId());
                continue;
            }

            List<String> depositPriKeyList = new ArrayList<>();
            depositPriKeyList.add(depositConfiguration.getPrivateKey());
            Map<String, Double> depositMap = new HashMap<>();
            Double value = sideRest - (txBasicConfiguration.getELA_CROSS_CHAIN_FEE() * 2);
            depositMap.put(depositConfiguration.getAddress(), value);
            ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
            ReturnMsgEntity ret = elaDidService.transferEla(
                    chain.getType(), depositPriKeyList,
                    ElaChainType.ELA_CHAIN, depositMap);
            if (ret.getStatus() != RetCode.SUCCESS) {
                logger.error("gatherAllDeposit gather side chain deposit failed. chainId:" + chain.getId() + " result:" + ret.getResult());
                continue;
            }
            String txid = (String) ret.getResult();
            InternalTxRecord internalTxRecord = new InternalTxRecord();
            internalTxRecord.setSrcChainId(chain.getId());
            internalTxRecord.setSrcAddr(depositConfiguration.getAddress());
            internalTxRecord.setDstChainId(mainChain.getId());
            internalTxRecord.setDstAddr(depositConfiguration.getAddress());
            internalTxRecord.setTxid(txid);
            internalTxRecord.setValue(value);
            internalTxRepository.save(internalTxRecord);
            gatherValue += value;
            logger.info("gatherAllDeposit tx ok. chainId:" + chain.getId() + " txid:" + txid);
        }
        return gatherValue;
    }

    public String adaptScheduledTask(boolean exchangeFlag, boolean balanceFlag) {
        scheduledTaskExchange.setOnFlag(exchangeFlag);
        scheduledTaskBalance.setOnFlag(balanceFlag);
        return new ServerResponse().setState(RetCode.SUCCESS).toJsonString();
    }

    public String gatherAllEla() {
        //todo eth can not gather more to one. so change strategy
        Double value = 0.0;
        value += renewalWalletService.gatherAllRenewalWallet();
        value += exchangeWalletsService.gatherAllExchangeWallet();
        value += this.gatherAllDeposit();

        Map<String, Object> data = new HashMap<>();
        data.put("value", value);
        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

}
