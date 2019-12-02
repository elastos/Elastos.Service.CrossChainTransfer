package org.elastos.service;

import org.elastos.POJO.ElaChainType;
import org.elastos.conf.*;
import org.elastos.constant.RetCode;
import org.elastos.dao.OutputWalletDbRepository;
import org.elastos.dao.InternalTxRepository;
import org.elastos.dto.OutputWalletDb;
import org.elastos.exception.ElastosServiceException;
import org.elastos.pojo.Chain;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.service.balance.DepositDidTask;
import org.elastos.service.balance.DepositElaTask;
import org.elastos.service.balance.DepositEthTask;
import org.elastos.util.AsynProcSet;
import org.elastos.util.OutputWallet;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OutputWalletsService {
    private static Logger logger = LoggerFactory.getLogger(OutputWalletsService.class);

    @Autowired
    private TxBasicConfiguration txBasicConfiguration;

    @Autowired
    private OutputWalletDbRepository outputWalletDbRepository;

    @Autowired
    private ChainService chainService;

    @Autowired
    DepositWalletsService depositWalletsService;


    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private AsynProcSet<ElaWalletAddress> usingAddressSet = new AsynProcSet<>();
    private Map<Long, OutputWallet> exchangeWalletMap = new HashMap<>();

    private OutputWallet geneExchangeWallet(Chain chain) {
        String mnemonic = chain.getElaTransferService().createMnemonic();
        if (null == mnemonic) {
            return null;
        }
        OutputWallet outputWallet = new OutputWallet(chain, mnemonic, txBasicConfiguration.getOUTPUT_ADDRESS_SUM(), redisTemplate);
        OutputWalletDb walletDb = saveWalletInfoToDb(outputWallet);
        outputWallet.setId(walletDb.getId());
        outputWallet.initAddresses();
        return outputWallet;
    }

    private OutputWalletDb saveWalletInfoToDb(OutputWallet outputWallet) {
        OutputWalletDb walletDb = new OutputWalletDb();
        walletDb.setChainId(outputWallet.getChain().getExchangeChain().getId());
        walletDb.setSum(outputWallet.getSum());
        walletDb.setMnemonic(outputWallet.getMnemonic());
        walletDb = outputWalletDbRepository.save(walletDb);
        return walletDb;
    }

    void initService() {
        exchangeWalletMap.clear();
        List<OutputWalletDb> dbWallets = (List<OutputWalletDb>) outputWalletDbRepository.findAll();
        List<Chain> chainList = chainService.getChains();

        for (Chain chain : chainList) {
            OutputWallet outputWallet;
            OutputWalletDb ew = this.getWalletFromDB(dbWallets, chain.getExchangeChain().getId());
            if (ew != null) {
                outputWallet = new OutputWallet(chain, ew.getMnemonic(), txBasicConfiguration.getOUTPUT_ADDRESS_SUM(), redisTemplate);
                outputWallet.setId(ew.getId());
                outputWallet.initAddresses();
            } else {
                //There is a new chain. we create exchange wallet for it.
                outputWallet = geneExchangeWallet(chain);
                if (null == outputWallet) {
                    throw new ElastosServiceException("initService geneExchangeWallet failed!");
                }
            }
            exchangeWalletMap.put(chain.getExchangeChain().getId(), outputWallet);
        }

        //恢复钱包状态
        getWalletsValue();
    }

    private OutputWalletDb getWalletFromDB(List<OutputWalletDb> walletDbList, long chainId) {
        for (OutputWalletDb ew : walletDbList) {
            if (ew.getChainId() == chainId) {
                return ew;
            }
        }
        return null;
    }

    public ElaWalletAddress getAddressForExchange(long chainId, Double value) {
        OutputWallet wallet = exchangeWalletMap.get(chainId);
        if (null == wallet) {
            logger.error("getAddressForExchange not wallet in chainId:" + chainId);
            return null;
        }

        for (int i = 0; i < wallet.getSum(); i++) {
            ElaWalletAddress address = wallet.getExchangeAddress();
            if (null == address) {
                logger.error("getAddressForExchange failed to get address");
                continue;
            }

            if (address.getInTx() != null) {
                continue;
            }

            RetResult<Double> ret = wallet.getChain().getElaTransferService().getBalance(address.getCredentials().getAddress());
            if (ret.getCode() != RetCode.SUCC) {
                continue;
            }
            Double rest = ret.getData();
            address.setValue(rest);

            Double spend = value;
            if (address.getChainType().equals(ElaChainType.ETH_CHAIN)) {
                spend += txBasicConfiguration.getETH_TRANSFER_GAS_SAVE();
            }

            if (rest >= spend) {
                return address;
            } else if (rest < txBasicConfiguration.getOUTPUT_ADDRESS_SUPPLY_THRESHOLD()) {
                depositWalletsService.saveForRenewal(address);
            }
        }

        logger.info("getAddressForExchange not a valid fast transfer wallet address in chainId:" + chainId);
        return null;
    }

    public void getWalletsValue() {
        List<OutputWallet> wallets = new ArrayList<>(exchangeWalletMap.values());
        for (OutputWallet wallet : wallets) {
            List<ElaWalletAddress> addressList = wallet.getAddressList();
            Double value = 0.0;

            for (ElaWalletAddress address : addressList) {
                RetResult<Double> ret = wallet.getChain().getElaTransferService().getBalance(address.getCredentials().getAddress());
                if (ret.getCode() == RetCode.SUCC) {
                    Double rest = ret.getData();
                    address.setValue(rest);
                    if (rest < txBasicConfiguration.getOUTPUT_ADDRESS_SUPPLY_THRESHOLD()) {
                        depositWalletsService.saveForRenewal(address);
                    }
                    value += rest;
                }
                //Just wait for node to catch a breath.
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
            }
            wallet.setValue(value);
        }
    }


    public List<Map<String, Object>> getWalletRest() {
        getWalletsValue();
        List<Map<String, Object>> list = new ArrayList<>();
        List<OutputWallet> wallets = new ArrayList<>(exchangeWalletMap.values());
        for (OutputWallet wallet : wallets) {
            Map<String, Object> map = new HashMap<>();
            map.put("chain_id", wallet.getChain().getExchangeChain().getId());
            map.put("chain_name", wallet.getChain().getExchangeChain().getChainName());
            map.put("address_sum", wallet.getSum());
            map.put("value", wallet.getValue());

            list.add(map);
        }
        return list;
    }

    Double gatherAllExchangeWallet() {
        Double gatherValue = 0.0;
        for (Map.Entry<Long, OutputWallet> entry : exchangeWalletMap.entrySet()) {
            Long chainId = entry.getKey();
            OutputWallet wallet = entry.getValue();

//            List<String> priKeyList = new ArrayList<>();
//            Double value = 0.0;
//            List<ElaWalletAddress> addressList = wallet.getAddressList();
//            for (ElaWalletAddress address : addressList) {
//                Double v = chainService.getBalancesByAddr(chain, address.getPublicAddress());
//                if (null == v) {
//                    logger.error("gatherAllExchangeWallet chainService.getBalancesByAddr " + address.getPublicAddress());
//                    continue;
//                }
//                if (v > 0.0) {
//                    value += v;
//                    priKeyList.add(address.getPrivateKey());
//                }
//            }
//
//            if (priKeyList.isEmpty()) {
//                logger.info("gatherAllExchangeWallet There is no ela in walletId:" + wallet.getId());
//                continue;
//            }
//
//            //If there is cross chain transaction
//            if (chain.getType().equals(ElaChainType.ELA_CHAIN)) {
//                value -= txBasicConfiguration.getELA_FEE();
//            } else {
//                value -= txBasicConfiguration.getELA_CROSS_CHAIN_FEE() * 2;
//            }
//            Map<String, Double> dstMap = new HashMap<>();
//            dstMap.put(depositConfiguration.getAddress(), value);
//
//            ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
//            ReturnMsgEntity ret = elaDidService.transferEla(
//                    chain.getType(), priKeyList,
//                    ElaChainType.ELA_CHAIN, dstMap);
//            if (ret.getStatus() != ServerResponseCode.SUCCESS) {
//                logger.error("gatherAllExchangeWallet tx failed walletId:" + wallet.getId() + " result:" + ret.getResult());
//            } else {
//                InternalTxRecord internalTxRecord = new InternalTxRecord();
//                internalTxRecord.setSrcChainId(chainId);
//                internalTxRecord.setSrcAddr(JSON.toJSONString(priKeyList));
//                internalTxRecord.setDstChainId(chainService.getChain(ElaChainType.ELA_CHAIN).getId());
//                internalTxRecord.setDstAddr(depositConfiguration.getAddress());
//                internalTxRecord.setTxid((String) ret.getResult());
//                internalTxRecord.setValue(value);
//                internalTxRepository.save(internalTxRecord);
//                logger.info("gatherAllExchangeWallet tx ok. walletId:" + wallet.getId() + " txid:" + ret.getResult());
//                gatherValue += value;
//            }
        }
        return gatherValue;
    }

    public void usingAddress(ElaWalletAddress address) {
        usingAddressSet.save2Set(address);
    }

    public void checkAddressFree() {
        if (usingAddressSet.isEmpty()) {
            return;
        }
        List<ElaWalletAddress> unDoneList = new ArrayList<>();
        List<ElaWalletAddress> addressList = usingAddressSet.usingAllData();
        for (ElaWalletAddress address : addressList) {
            ElaChainType chainType = address.getChainType();
            Chain chain = chainService.getChain(chainType);
            ElaTransferService transferService = chain.getElaTransferService();
            if (null != address.getInTx()) {
                RetResult<String> retResult = transferService.getTransactionReceipt(address.getInTx());
                if (retResult.getCode() == RetCode.SUCC) {
                    address.setInTx(null);
                } else {
                    unDoneList.add(address);
                }
            }
        }

        usingAddressSet.releaseData(addressList);
        for (ElaWalletAddress address : unDoneList) {
            usingAddressSet.save2Set(address);
        }
    }
}
