package org.elastos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.elastos.POJO.ChainCredentials;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.NodeConfiguration;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constants.RetCode;
import org.elastos.dao.ExchangeChainRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.util.HttpUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.core.methods.response.EthTransaction;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.pow;

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
    RetCodeConfiguration retCodeConfiguration;

    private EthService ethService;

    private ElaDidService elaDidService;

    private Map<Long, ExchangeChain> exchangeChainMap = new HashMap<>();

    public List<ExchangeChain> getChains() {
        return new ArrayList<>(exchangeChainMap.values());
    }

    public ExchangeChain getChain(Long chainId) {
        return exchangeChainMap.get(chainId);
    }

    public ExchangeChain getChain(ElaChainType chainType) {
        for (Map.Entry<Long, ExchangeChain> entry : exchangeChainMap.entrySet()) {
            ExchangeChain chain = entry.getValue();
            if (chain.getType().equals(chainType)) {
                return chain;
            }
        }
        return null;
    }

    void initService() {
        List<ExchangeChain> exchangeChains = (List<ExchangeChain>) exchangeChainRepository.findAll();
        for (ExchangeChain chain : exchangeChains) {
            exchangeChainMap.put(chain.getId(), chain);
        }

        ExchangeChain chain = this.getChain(ElaChainType.ETH_CHAIN);
        ethService = new EthService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());

    }

    public ExchangeChain getExchangeChain(long chainId) {
        return exchangeChainMap.get(chainId);
    }

    public List<Map<String, Object>> getChainList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ExchangeChain chain : exchangeChainMap.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("chain_name", chain.getChainName());
            m.put("chain_id", chain.getId());
            list.add(m);
        }
        return list;
    }

    public Object getTransaction(Long chainId, String txid) {
        ExchangeChain chain = exchangeChainMap.get(chainId);
        if (null == chain) {
            logger.error("Err getTransaction there is no chain id:" + chainId);
            return null;
        }

        switch (chain.getType()) {
            case ETH_CHAIN:
                EthTransaction ethTransaction = ethService.getTransaction(txid);
                return ethTransaction;
            case ELA_CHAIN:
            case DID_CHAIN:
                Map map = getElaChainInfo(chain.getChainUrlPrefix() + nodeConfiguration.getTransaction() + "/" + txid, Map.class);
                return map;
            default:
                logger.error("Err getTransaction not support chain id:" + chainId + "chain type:" + chain.getType());
                break;
        }
        return null;
    }

    public Double getBalancesByAddr(Long chainId, String address) {
        ExchangeChain chain = exchangeChainMap.get(chainId);
        if (null == chain) {
            logger.error("Err getTransaction there is no chain id:" + chainId);
            return null;
        }

        return getBalancesByAddr(chain, address);
    }

    public Double getBalancesByAddr(ExchangeChain chain, String address) {

        Double rest = null;
        switch (chain.getType()) {
            case ETH_CHAIN:
                rest = ethService.getBalence(address);
                break;
            case ELA_CHAIN:
            case DID_CHAIN:
                rest = getElaChainInfo(chain.getChainUrlPrefix() + nodeConfiguration.getBalanceByAddr() + "/" + address, Double.class);
                break;
            default:
                logger.error("getRest chain type not support: chain type:" + chain.getType());
                break;
        }

        if (null != rest) {
            return rest;
        } else {
            throw new RuntimeException("getRest http failed");
        }
    }

    public Double getTransferFee(ElaChainType src, ElaChainType dst) {
        if (src == dst) {
            if (src == ElaChainType.ETH_CHAIN) {
                return txBasicConfiguration.getETH_FEE();
            } else {
                return txBasicConfiguration.getELA_FEE();
            }
        } else {
            if (src == ElaChainType.ETH_CHAIN) {
                return txBasicConfiguration.getETH_CROSS_CHAIN_FEE();
            } else {
                return txBasicConfiguration.getELA_CROSS_CHAIN_FEE();
            }
        }
    }

    public String transfer(ExchangeChain srcChain, String srcPrivateKey, ElaChainType dstChainType, String dstAddress, double value) {
        try {
            switch (srcChain.getType()) {
                case ELA_CHAIN:
                case DID_CHAIN:
                    return elaTransfer(srcChain, srcPrivateKey, dstChainType, dstAddress, value);
                case ETH_CHAIN:
                    return ethTransfer(srcChain, srcPrivateKey, dstChainType, dstAddress, value);
                default:
                    logger.error("transfer not support src chain:" + srcChain.getType().toString());
                    return null;
            }
        } catch (Exception e) {
            logger.error("transfer exception:" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String ethTransfer(ExchangeChain srcChain, String srcPrivateKey, ElaChainType dstChainType, String dstAddress, double value) {
        if (dstChainType == ElaChainType.ETH_CHAIN) {
            return ethService.transfer(srcPrivateKey, dstAddress, value);
        } else if (dstChainType == ElaChainType.ELA_CHAIN) {
            return ethService.withdrawEla(srcPrivateKey, dstAddress, value);
        } else {
            logger.error("ethTransfer not support dst chain:" + dstChainType.toString());
            return null;
        }
    }

    @Nullable
    private String elaTransfer(ExchangeChain srcChain, String srcPrivateKey, ElaChainType dstChainType, String dstAddress, double value) {
        List<String> depositPriKeyList = new ArrayList<>();
        depositPriKeyList.add(srcPrivateKey);
        Map<String, Double> depositMap = new HashMap<>();
        depositMap.put(dstAddress, value);
        ElaDidService elaDidService = new ElaDidService(srcChain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
        ReturnMsgEntity ret = elaDidService.transferEla(
                srcChain.getType(), depositPriKeyList,
                dstChainType, depositMap);
        if (ret.getStatus() != RetCode.SUCCESS) {
            logger.error("transfer elaDidService.transferEla failed. chainId:" + srcChain.getId() + " result:" + ret.getResult());
            return null;
        } else {
            String txid = (String) ret.getResult();
            return txid;
        }
    }


    private <T> T getElaChainInfo(String url, Class<T> clazz) {
        String response = HttpUtil.get(url, null);
        if (StringUtils.isBlank(response)) {
            return null;
        } else {
            JSONObject msg = JSON.parseObject(response);
            if (msg.getInteger("Error") == 0) {
                return msg.getObject("Result", clazz);
            } else {
                logger.error("Err url:" + url + " Error:" + msg.getInteger("Error") + " Result:" + msg.get("Result"));
                return null;
            }
        }
    }

    public ChainCredentials geneAddress(ExchangeChain chain, String mnemonic, int i) {
        ChainCredentials credentials = null;
        if (chain.getType() == ElaChainType.ETH_CHAIN) {
            credentials = ethService.geneCredentials(mnemonic, i);
        } else {
            ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
            try {
                credentials = elaDidService.geneCredentials(mnemonic, i);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException | CipherException e) {
                e.printStackTrace();
            }
        }

        return credentials;
    }

    public String geneMnemonic(ExchangeChain chain) {
        String mnemonic = null;
        if (chain.getType() == ElaChainType.ETH_CHAIN) {
            mnemonic = ethService.createMnemonic();
        } else {
            ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
            mnemonic = elaDidService.createMnemonic();
        }
        return mnemonic;
    }
}
