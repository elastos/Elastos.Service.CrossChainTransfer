package org.elastos.service;

import com.alibaba.fastjson.JSON;
import jnr.ffi.annotations.Synchronized;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.DepositConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constants.RetCode;
import org.elastos.dao.GatherAddressRepository;
import org.elastos.dao.InternalTxRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.dto.GatherAddress;
import org.elastos.dto.InternalTxRecord;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.pojo.ElaWalletAddress;
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
    GatherAddressRepository gatherAddressRepository;

    @Autowired
    InternalTxRepository internalTxRepository;

    @Autowired
    ChainService chainService;

    @Autowired
    RenewalWalletService renewalWalletService;

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
        List<GatherAddress> gatherAddresses = (List<GatherAddress>) gatherAddressRepository.findAll();
        if (gatherAddresses.isEmpty()) {
            return;
        }

        for (GatherAddress ga : gatherAddresses) {
            ElaWalletAddress address = renewalWalletService.findAddress(ga.getWalletId(), ga.getAddressId().intValue());
            Double value = chainService.getBalancesByAddr(ga.getChainId(), address.getPublicAddress());
            if (value > txBasicConfiguration.getFEE()) {
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

            ElaDidService elaDidService = new ElaDidService();
            List<String> priKeyList = new ArrayList<>();
            Double value = 0.0;
            for (ElaWalletAddress address : renewalSet) {
                Double v = address.getRest();
                value += v;
                priKeyList.add(address.getPrivateKey());
            }

            //If there is cross chain transaction
            if (chain.getType().equals(ElaChainType.ELA_CHAIN)) {
                value -= txBasicConfiguration.getFEE();
            } else {
                value -= txBasicConfiguration.getCROSS_CHAIN_FEE() * 2;
            }
            Map<String, Double> dstMap = new HashMap<>();
            dstMap.put(depositConfiguration.getAddress(), value);

            ReturnMsgEntity ret = elaDidService.transferEla(chain.getChainUrlPrefix(),
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
                TimeUnit.MINUTES.sleep(txBasicConfiguration.getCROSS_CHAIN_TRANSFER_WAIT());
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

            Double rest = chainService.getBalancesByAddr(chainId, depositConfiguration.getAddress());
            Double fullRenewal = 0.0;
            Map<String, Double> dstMap = new HashMap<>();
            for (ElaWalletAddress address : exchangeSet) {
                Double addRest = chainService.getBalancesByAddr(chainId, address.getPublicAddress());
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
                this.MainDepositToSideChainDepositTask();
                continue;
            }

            //Then we renewal worker addresses.
            List<String> priKeyList = new ArrayList<>();
            priKeyList.add(depositConfiguration.getPrivateKey());

            ElaDidService elaDidService = new ElaDidService();
            ReturnMsgEntity ret = elaDidService.transferEla(chain.getChainUrlPrefix(),
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
                Map<String, Object> objectMap = waitTxFinish(chain, txid, 3, txBasicConfiguration.getSAME_CHAIN_TRANSFER_WAIT());
                if (null == objectMap) {
                    logger.error("EveryDepositToExchangeTask wait main chain to side chain tx failed. txid:" + txid);
                }
            }
        }
    }

    private Map<String, Object> waitTxFinish(ExchangeChain chain, String txid, int waitSum, int waitTime) {
        for (int i = 0; i < waitSum; i++) {
            try {
                TimeUnit.MINUTES.sleep(waitTime);
            } catch (InterruptedException e) {
                logger.info("waitTxFinish interrupted");
            }
            Map<String, Object> objectMap = chainService.getTransaction(chain.getId(), txid);
            logger.debug("waitTxFinish ing " +i);
            if (null != objectMap) {
                return objectMap;
            }
        }
        return null;
    }

    @Synchronized
    void MainDepositToSideChainDepositTask() {
        c++;
        logger.debug("MainDepositToSideChainDepositTask in" + c);
        List<ExchangeChain> chains = chainService.getChains();
        for (ExchangeChain chain : chains) {
            if (chain.getType().equals(ElaChainType.ELA_CHAIN)) {
                //We do not renewal main chain deposit here
                continue;
            }

            Double fullRenewal = depositConfiguration.getRenewalCapability() * txBasicConfiguration.getWORKER_ADDRESS_RENEWAL_VALUE() * txBasicConfiguration.getWORKER_ADDRESS_SUM();
            fullRenewal += depositConfiguration.getRenewalCapability() * txBasicConfiguration.getFEE();

            Double sideRest = chainService.getBalancesByAddr(chain.getId(), depositConfiguration.getAddress());
            if (sideRest >= fullRenewal) {
                //There is enough rest in this chain deposit address, no need to renewal.
                continue;
            }

            ExchangeChain mainChain = chainService.getChain(ElaChainType.ELA_CHAIN);
            if (null == mainChain) {
                logger.error("Err MainDepositToSideChainDepositTask there is no main chain!!!");
                continue;
            }

            Double mainRest = chainService.getBalancesByAddr(mainChain.getId(), depositConfiguration.getAddress());
            if (mainRest <= fullRenewal) {
                logger.info("Err MainDepositToSideChainDepositTask there is not enough ela in main chain!!!");
                this.UserInputToMainDepositTask();
                continue;
            }

            List<String> depositPriKeyList = new ArrayList<>();
            depositPriKeyList.add(depositConfiguration.getPrivateKey());
            Map<String, Double> depositMap = new HashMap<>();
            depositMap.put(depositConfiguration.getAddress(), fullRenewal - sideRest);
            logger.debug("MainDepositToSideChainDepositTask elaDidService" + c);
            ElaDidService elaDidService = new ElaDidService();
            ReturnMsgEntity ret = elaDidService.transferEla(mainChain.getChainUrlPrefix(),
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
            Map<String, Object> objectMap = waitTxFinish(chain, txid, 3, txBasicConfiguration.getCROSS_CHAIN_TRANSFER_WAIT());
            logger.debug("MainDepositToSideChainDepositTask waitTxFinish end" + c);
            if (null == objectMap) {
                logger.error("MainDepositToSideChainDepositTask wait main chain to side chain tx failed. txid:" + txid);
            }
        }
        logger.debug("MainDepositToSideChainDepositTask out" + c);
    }
}
