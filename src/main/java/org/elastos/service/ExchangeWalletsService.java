package org.elastos.service;

import org.elastos.conf.*;
import org.elastos.dao.ExchangeWalletDbRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.dto.ExchangeWalletDb;
import org.elastos.exception.ElastosServiceException;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.ExchangeWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExchangeWalletsService {
    private static Logger logger = LoggerFactory.getLogger(ExchangeWalletsService.class);

    @Autowired
    private TxBasicConfiguration txBasicConfiguration;

    @Autowired
    private ExchangeWalletDbRepository exchangeWalletDbRepository;

    @Autowired
    private ChainService chainService;

    @Autowired
    private WalletBalanceService walletBalanceService;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private Map<Long, ExchangeWallet> exchangeWalletMap = new HashMap<>();

    private ExchangeWallet geneElaWallet(Long chainId) {
        ElaDidService elaDidService = new ElaDidService();
        String mnemonic = elaDidService.createMnemonic();
        ExchangeWallet exchangeWallet = new ExchangeWallet(chainId, mnemonic, txBasicConfiguration.getWORKER_ADDRESS_SUM(), redisTemplate);
        ExchangeWalletDb walletDb = saveWalletInfoToDb(exchangeWallet);
        exchangeWallet.setId(walletDb.getId());
        exchangeWallet.initAddresses();
        return exchangeWallet;
    }

    private ExchangeWalletDb saveWalletInfoToDb(ExchangeWallet exchangeWallet) {
        ExchangeWalletDb walletDb = new ExchangeWalletDb();
        walletDb.setChainId(exchangeWallet.getChainId());
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
            ExchangeWallet exchangeWallet;
            ExchangeWalletDb ew = this.filterWalletsByChainId(wallets, chain.getId());
            if (ew != null) {
                exchangeWallet = new ExchangeWallet(ew.getChainId(), ew.getMnemonic(), txBasicConfiguration.getWORKER_ADDRESS_SUM(), redisTemplate);
                exchangeWallet.setId(ew.getId());
                exchangeWallet.initAddresses();
            } else {
                exchangeWallet = geneElaWallet(chain.getId());
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


        for(int i = 0; i < wallet.getSum(); i++){
            ElaWalletAddress address = wallet.getExchangeAddress();
            if (null == address) {
                throw new ElastosServiceException("getExchangeAddress failed to get address");
            }
            Double rest = chainService.getBalancesByAddr(chainId, address.getPublicAddress());
            if (rest >= value) {
                return address;
            } else if(rest < txBasicConfiguration.getWORKER_ADDRESS_MIN_THRESHOLD()){
                walletBalanceService.save2ExchangeAddress(chainId, address);
            }
        }

        walletBalanceService.EveryDepositToExchangeTask();
        logger.error("getExchangeAddress not a valid wallet address in chainId:" + chainId);
        return null;
    }

    public void walletsCheckTask() {
        List<ExchangeWallet> wallets = new ArrayList<>(exchangeWalletMap.values());
        for (ExchangeWallet wallet : wallets) {
            List<ElaWalletAddress> addressList = wallet.getAddressList();
            Double value = 0.0;

            for (ElaWalletAddress address : addressList) {
                Double rest = chainService.getBalancesByAddr(wallet.getChainId(), address.getPublicAddress());
                if (null != rest) {
                    address.setRest(rest);
                    if (rest < txBasicConfiguration.getWORKER_ADDRESS_MIN_THRESHOLD()) {
                        walletBalanceService.save2ExchangeAddress(wallet.getChainId(), address);
                    }
                    value += rest;
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
            map.put("chain_id", wallet.getChainId());
            map.put("wallet_sum", wallet.getSum());
            map.put("value", wallet.getValue());

            list.add(map);
        }
        return list;
    }
}
