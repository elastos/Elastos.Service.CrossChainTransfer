package org.elastos.service;

import org.elastos.POJO.Credentials;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.NodeConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.dao.ExchangeChainRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.exception.ElastosServiceException;
import org.elastos.pojo.Chain;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChainService {
    private static Logger logger = LoggerFactory.getLogger(ChainService.class);
    @Autowired
    NodeConfiguration nodeConfiguration;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    @Autowired
    ExchangeChainRepository exchangeChainRepository;

    @Autowired
    DepositWalletsService depositWalletsService;

    final Integer deposit_index = 1;

    private Map<Long, Chain> chainMap = new HashMap<>();

    public List<Chain> getChains() {
        return new ArrayList<>(chainMap.values());
    }

    public Chain getChain(Long chainId) {
        return chainMap.get(chainId);
    }

    public Chain getChain(ElaChainType chainType) {
        for (Map.Entry<Long, Chain> entry : chainMap.entrySet()) {
            Chain chain = entry.getValue();
            ExchangeChain exChain = chain.getExchangeChain();
            if (exChain.getType().equals(chainType)) {
                return chain;
            }
        }
        return null;
    }

    public boolean isChainOk(Long chainId, String address){
        Chain chain = getChain(chainId);
        RetResult<Double> ret = chain.getElaTransferService().getBalance(address);
        if (ret.getCode() == RetCode.SUCC) {
            return true;
        } else {
            return false;
        }
    }

    void initService() {
        List<ExchangeChain> chains = (List<ExchangeChain>) exchangeChainRepository.findAll();
        for (ExchangeChain exChain : chains) {
            Chain chain = new Chain();
            chain.setExchangeChain(exChain);
            ElaTransferService elaTransferService = ElaTransferService.getInstance(exChain.getType(), exChain.getChainUrlPrefix(), exChain.getTest());
            chain.setElaTransferService(elaTransferService);
            chain.initBalanceData(txBasicConfiguration);
            chainMap.put(exChain.getId(), chain);
            boolean isSave = false;
            String mnemonic = exChain.getMnemonic();
            if (null == mnemonic) {
                isSave = true;
                mnemonic = elaTransferService.createMnemonic();
                exChain.setMnemonic(mnemonic);
            }
            Integer idx = exChain.getIndex();
            if (null == idx) {
                isSave = true;
                idx = deposit_index;
                exChain.setIndex(idx);
            }

            RetResult<Credentials> ret = elaTransferService.geneCredentials(mnemonic, idx);
            if (ret.getCode() != RetCode.SUCC) {
                throw new ElastosServiceException("create credentials failed. id:" + exChain.getId());
            }

            if (isSave) {
                exchangeChainRepository.save(exChain);
            }

            depositWalletsService.putDepositMap(exChain.getType(), ret.getData());
        }
    }

    public List<Map<String, Object>> getChainList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Chain chain : chainMap.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("chain_id", chain.getExchangeChain().getId());
            m.put("chain_name", chain.getExchangeChain().getChainName());
            m.put("chain_type", chain.getExchangeChain().getType().ordinal());
            list.add(m);
        }
        return list;
    }

    public String getTransactionReceipt(Long chainId, String txid) {
        Chain chain = chainMap.get(chainId);
        if (null == chain) {
            logger.error("Err getTransactionReceipt there is no chain id:" + chainId);
            return null;
        }

        RetResult<String> tx = chain.getElaTransferService().getTransactionReceipt(txid);
        if (tx.getCode() == RetCode.SUCC) {
            return tx.getData();
        } else {
            return null;
        }
    }
}
