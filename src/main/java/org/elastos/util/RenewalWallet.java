package org.elastos.util;

import com.alibaba.fastjson.JSON;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.elastos.POJO.ChainCredentials;
import org.elastos.dto.ExchangeChain;
import org.elastos.exception.ElastosServiceException;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.service.ChainService;
import org.elastos.util.ela.ElaHdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

public class RenewalWallet {
    private Long id;
    private ExchangeChain chain;
    private String mnemonic;
    private Integer maxUse;
    private Double value;
    private Map<Integer, ElaWalletAddress> addressMap = new HashMap<>();
    private DCountUtil dCountUtil = null;
    private static Logger logger = LoggerFactory.getLogger(RenewalWallet.class);

    public RenewalWallet(ExchangeChain exchangeChain, String m, Integer max, RedisTemplate<String, Object> redisTemplate) {
        chain = exchangeChain;
        mnemonic = m;
        SimpleHash hash = new SimpleHash("md5", mnemonic, chain.getId().toString(), 2);
        String name = hash.toHex();
        if (0 != max) {
            dCountUtil = new DCountUtil(name, Long.valueOf(max) - 1, redisTemplate);
        } else {
            dCountUtil = new DCountUtil(name, -1L, redisTemplate);
        }
        maxUse = max;
    }

    public ElaWalletAddress getAddress(int idx, ChainService chainService) {
        ChainCredentials credentials = chainService.geneAddress(chain, mnemonic, idx);
        if (null == credentials) {
            logger.error("Err getAddress ChainService.geneAddress failed");
            return null;
        }

        if (maxUse < (idx + 1)) {
            maxUse = idx + 1;
        }

        ElaWalletAddress address = new ElaWalletAddress();
        address.setPrivateKey(credentials.getKeyPair().getPrivateKey());
        address.setPublicKey(credentials.getKeyPair().getPublicKey());
        address.setPublicAddress(credentials.getAddress());
        address.setId(idx);
        address.setWalletId(this.getId());
        addressMap.put(idx, address);
        return address;
    }

    public ElaWalletAddress geneNewAddress(ChainService chainService) {
        Long idx = dCountUtil.inc();
        if (idx > Integer.MAX_VALUE) {
            logger.error("Ela wallet has generated max address");
            return null;
        }
        ChainCredentials credentials = chainService.geneAddress(chain, mnemonic, idx.intValue());
        if (null == credentials) {
            logger.error("Err geneNewAddress ChainService.geneAddress failed");
            return null;
        }

        maxUse = idx.intValue() + 1;
        ElaWalletAddress address = new ElaWalletAddress();
        address.setPrivateKey(credentials.getKeyPair().getPrivateKey());
        address.setPublicKey(credentials.getKeyPair().getPublicKey());
        address.setPublicAddress(credentials.getAddress());
        address.setId(idx.intValue());
        addressMap.put(idx.intValue(), address);
        address.setWalletId(this.getId());
        return address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExchangeChain getChain() {
        return chain;
    }

    public void setChain(ExchangeChain chain) {
        this.chain = chain;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public Integer getMaxUse() {
        return maxUse;
    }

    public Map<Integer, ElaWalletAddress> getAddressMap() {
        return addressMap;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
