package org.elastos.service;

import com.alibaba.fastjson.JSON;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.*;
import org.elastos.constants.RetCode;
import org.elastos.dao.ExchangeWalletDbRepository;
import org.elastos.dao.InternalTxRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.dto.ExchangeWalletDb;
import org.elastos.dto.InternalTxRecord;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.exception.ElastosServiceException;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.ExchangeWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ExchangeWalletsService {
    private static Logger logger = LoggerFactory.getLogger(ExchangeWalletsService.class);

    @Autowired
    private TxBasicConfiguration txBasicConfiguration;

    @Autowired
    private DepositConfiguration depositConfiguration;

    @Autowired
    private NodeConfiguration nodeConfiguration;

    @Autowired
    private ExchangeWalletDbRepository exchangeWalletDbRepository;

    @Autowired
    private InternalTxRepository internalTxRepository;

    @Autowired
    private ChainService chainService;

    @Autowired
    private WalletBalanceService walletBalanceService;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private Map<Long, ExchangeWallet> exchangeWalletMap = new HashMap<>();

    private ExchangeWallet geneElaWallet(ExchangeChain chain, ChainService chainService) {
        String mnemonic = chainService.geneMnemonic(chain);
        if (null == mnemonic) {
            return null;
        }
        ExchangeWallet exchangeWallet = new ExchangeWallet(chain, mnemonic, txBasicConfiguration.getWORKER_ADDRESS_SUM(), redisTemplate);
        ExchangeWalletDb walletDb = saveWalletInfoToDb(exchangeWallet);
        exchangeWallet.setId(walletDb.getId());
        exchangeWallet.initAddresses(chainService);
        return exchangeWallet;
    }

    private ExchangeWalletDb saveWalletInfoToDb(ExchangeWallet exchangeWallet) {
        ExchangeWalletDb walletDb = new ExchangeWalletDb();
        walletDb.setChainId(exchangeWallet.getChain().getId());
        walletDb.setSum(exchangeWallet.getSum());
        walletDb.setMnemonic(exchangeWallet.getMnemonic());
        walletDb = exchangeWalletDbRepository.save(walletDb);
        return walletDb;
    }

    void initService() {
        exchangeWalletMap.clear();
        List<ExchangeWalletDb> wallets = (List<ExchangeWalletDb>) exchangeWalletDbRepository.findAll();
        List<ExchangeChain> chainList = chainService.getChains();

        for (ExchangeChain chain : chainList) {
            //Eth transfer directly
            if (chain.getType() == ElaChainType.ETH_CHAIN) {
                continue;
            }

            ExchangeWallet exchangeWallet;
            ExchangeWalletDb ew = this.filterWalletsByChainId(wallets, chain.getId());
            if (ew != null) {
                exchangeWallet = new ExchangeWallet(chain, ew.getMnemonic(), txBasicConfiguration.getWORKER_ADDRESS_SUM(), redisTemplate);
                exchangeWallet.setId(ew.getId());
                exchangeWallet.initAddresses(chainService);
            } else {
                exchangeWallet = geneElaWallet(chain, chainService);
                if (null == exchangeWallet) {
                    throw new ElastosServiceException("initService geneElaWallet failed!");
                }
            }
            exchangeWalletMap.put(chain.getId(), exchangeWallet);
        }

        //恢复钱包状态
        walletsCheckTask();
    }

    private ExchangeWalletDb filterWalletsByChainId(List<ExchangeWalletDb> wallets, long chainId) {
        for (ExchangeWalletDb ew : wallets) {
            if (ew.getChainId() == chainId) {
                return ew;
            }
        }
        return null;
    }

    public ElaWalletAddress getExchangeAddress(long chainId, Double value) {
        ExchangeWallet wallet = exchangeWalletMap.get(chainId);
        if (null == wallet) {
            logger.error("getExchangeAddress not wallet in chainId:" + chainId);
            return null;
        }


        for (int i = 0; i < wallet.getSum(); i++) {
            ElaWalletAddress address = wallet.getExchangeAddress();
            if (null == address) {
                logger.error("getExchangeAddress failed to get address");
                continue;
            }
            Double rest = chainService.getBalancesByAddr(chainId, address.getPublicAddress());
            if (null == rest) {
                continue;
            } else {
                address.setRest(rest);
            }

            if (rest >= value) {
                return address;
            } else if (rest < txBasicConfiguration.getWORKER_ADDRESS_RENEWAL_MIN_THRESHOLD()) {
                walletBalanceService.save2ExchangeAddress(chainId, address);
            }
        }

        logger.info("getExchangeAddress not a valid fast transfer wallet address in chainId:" + chainId);
        return null;
    }


    public void walletsCheckTask() {
        List<ExchangeWallet> wallets = new ArrayList<>(exchangeWalletMap.values());
        for (ExchangeWallet wallet : wallets) {
            List<ElaWalletAddress> addressList = wallet.getAddressList();
            Double value = 0.0;

            for (ElaWalletAddress address : addressList) {
                Double rest = chainService.getBalancesByAddr(wallet.getChain(), address.getPublicAddress());
                if (null != rest) {
                    address.setRest(rest);
                    value += rest;
                }
                //Just wait for no pushing node so much.
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    logger.info("UserInputToMainDepositTask interrupted.");
                }
            }
            wallet.setValue(value);
        }
    }


    public List<Map<String, Object>> getWalletRest() {
        List<Map<String, Object>> list = new ArrayList<>();
        List<ExchangeWallet> wallets = new ArrayList<>(exchangeWalletMap.values());
        for (ExchangeWallet wallet : wallets) {
            Map<String, Object> map = new HashMap<>();
            map.put("chain_id", wallet.getChain().getId());
            map.put("wallet_sum", wallet.getSum());
            map.put("value", wallet.getValue());

            list.add(map);
        }
        return list;
    }

    Double gatherAllExchangeWallet() {
        Double gatherValue = 0.0;
        for (Map.Entry<Long, ExchangeWallet> entry : exchangeWalletMap.entrySet()) {
            Long chainId = entry.getKey();
            ExchangeChain chain = chainService.getChain(chainId);
            ExchangeWallet wallet = entry.getValue();

            List<String> priKeyList = new ArrayList<>();
            Double value = 0.0;
            List<ElaWalletAddress> addressList = wallet.getAddressList();
            for (ElaWalletAddress address : addressList) {
                Double v = chainService.getBalancesByAddr(chain, address.getPublicAddress());
                if (v > 0.0) {
                    value += v;
                    priKeyList.add(address.getPrivateKey());
                }
            }

            if (priKeyList.isEmpty()) {
                logger.info("gatherAllExchangeWallet There is no ela in walletId:" + wallet.getId());
                continue;
            }

            //If there is cross chain transaction
            if (chain.getType().equals(ElaChainType.ELA_CHAIN)) {
                value -= txBasicConfiguration.getELA_FEE();
            } else {
                value -= txBasicConfiguration.getELA_CROSS_CHAIN_FEE() * 2;
            }
            Map<String, Double> dstMap = new HashMap<>();
            dstMap.put(depositConfiguration.getAddress(), value);

            ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
            ReturnMsgEntity ret = elaDidService.transferEla(
                    chain.getType(), priKeyList,
                    ElaChainType.ELA_CHAIN, dstMap);
            if (ret.getStatus() != RetCode.SUCCESS) {
                logger.error("gatherAllExchangeWallet tx failed walletId:" + wallet.getId() + " result:" + ret.getResult());
            } else {
                InternalTxRecord internalTxRecord = new InternalTxRecord();
                internalTxRecord.setSrcChainId(chainId);
                internalTxRecord.setSrcAddr(JSON.toJSONString(priKeyList));
                internalTxRecord.setDstChainId(chainService.getChain(ElaChainType.ELA_CHAIN).getId());
                internalTxRecord.setDstAddr(depositConfiguration.getAddress());
                internalTxRecord.setTxid((String) ret.getResult());
                internalTxRecord.setValue(value);
                internalTxRepository.save(internalTxRecord);
                logger.info("gatherAllExchangeWallet tx ok. walletId:" + wallet.getId() + " txid:" + ret.getResult());
                gatherValue += value;
            }
        }
        return gatherValue;
    }
}
