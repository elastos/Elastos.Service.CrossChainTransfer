package org.elastos.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import org.apache.commons.lang3.StringUtils;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.NodeConfiguration;
import org.elastos.conf.RetCodeConfiguration;
import org.elastos.dao.ExchangeChainRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.dto.ExchangeRate;
import org.elastos.util.HttpKit;
import org.elastos.util.HttpUtil;
import org.jetbrains.annotations.Nullable;
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
    private static Logger logger = LoggerFactory.getLogger(LoginService.class);
    @Autowired
    NodeConfiguration nodeConfiguration;

    @Autowired
    ExchangeChainRepository exchangeChainRepository;

    @Autowired
    RetCodeConfiguration retCodeConfiguration;

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
                nodeConfiguration.setPrefix(chain.getChainUrlPrefix());
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

    private Long setChain(Long chainId) {
        ExchangeChain chain = exchangeChainMap.get(chainId);
        if (null == chain) {
            logger.error("Err there is no chain id:" + chainId);
            return null;
        }

        nodeConfiguration.setPrefix(chain.getChainUrlPrefix());
        return chainId;
    }

    private Long setChain(ElaChainType chainType) {
        for (Map.Entry<Long, ExchangeChain> entry : exchangeChainMap.entrySet()) {
            ExchangeChain chain = entry.getValue();
            if (chain.getType().equals(chainType)) {
                nodeConfiguration.setPrefix(chain.getChainUrlPrefix());
                return chain.getId();
            }
        }
        return null;
    }

    public Map getTransaction(Long chainId, String txid) {
        Long id = setChain(chainId);
        if (null == id) {
            logger.error("Err getTxid setChain");
            return null;
        }

        Map map = getElaChainInfo(nodeConfiguration.getTransaction() + "/" + txid, Map.class);
        if (null != map) {
            return map;
        } else {
            return null;
        }
    }

    public Double getBalancesByAddr(Long chainId, String address) {
        Long id = setChain(chainId);
        if (null == id) {
            logger.error("Err getUtxoListByAddr setChain");
            return null;
        }

        return getRest(address);
    }

    @Nullable
    private Double getRest(String address) {
        Double obj = getElaChainInfo(nodeConfiguration.getBalanceByAddr() + "/" + address, Double.class);
        if (null != obj) {
            return obj;
        } else {
            throw new RuntimeException("getRest http failed");
        }
    }

    public Double getBalancesByAddr(ElaChainType type, String address) {
        Long id = setChain(type);
        if (null == id) {
            logger.error("Err getUtxoListByAddr setChain");
            return null;
        }

        return getRest(address);
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
}
