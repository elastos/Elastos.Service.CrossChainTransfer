package org.elastos.util;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.elastos.POJO.Credentials;
import org.elastos.constant.RetCode;
import org.elastos.pojo.Chain;
import org.elastos.pojo.ElaWalletAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

public class InputWallet {
    private Long id;
    private Chain chain;
    private String mnemonic;
    private Integer maxUse;
    private Double value;
    private Map<Integer, ElaWalletAddress> addressMap = new HashMap<>();
    private DCountUtil dCountUtil = null;
    private static Logger logger = LoggerFactory.getLogger(InputWallet.class);

    public InputWallet(Chain chain, String m, Integer max, RedisTemplate<String, Object> redisTemplate) {
        this.chain = chain;
        mnemonic = m;
        SimpleHash hash = new SimpleHash("md5", mnemonic, this.chain.getExchangeChain().getId().toString(), 2);
        String name = hash.toHex();
        if (0 == max) {
            dCountUtil = new DCountUtil(name, -1L, redisTemplate);
        } else {
            dCountUtil = new DCountUtil(name, Long.valueOf(max) - 1, redisTemplate);
        }
        maxUse = max;
    }

    public ElaWalletAddress getAddress(int idx) {
        RetResult<Credentials> ret = chain.getElaTransferService().geneCredentials(mnemonic, idx);
        if (ret.getCode() != RetCode.SUCC) {
            logger.error("Err getAddress getElaTransferService.geneCredentials failed. " + ret.getMsg());
            return null;
        }

        if (maxUse < (idx + 1)) {
            maxUse = idx + 1;
        }

        Credentials credentials = ret.getData();
        ElaWalletAddress address = new ElaWalletAddress();
        address.setCredentials(credentials);
        address.setId(idx);
        address.setWalletId(this.getId());
        address.setChainType(chain.getExchangeChain().getType());
        addressMap.put(idx, address);
        return address;
    }

    public ElaWalletAddress geneNewAddress() {
        Long idx = dCountUtil.inc();
        maxUse = idx.intValue() + 1;
        if (idx > Integer.MAX_VALUE) {
            logger.error("Ela wallet has generated max address");
            return null;
        }

        RetResult<Credentials> ret = chain.getElaTransferService().geneCredentials(mnemonic, idx.intValue());
        if (ret.getCode() != RetCode.SUCC) {
            logger.error("Err geneNewAddress getElaTransferService.geneCredentials failed. " + ret.getMsg());
            return null;
        }

        Credentials credentials = ret.getData();
        ElaWalletAddress address = new ElaWalletAddress();
        address.setCredentials(credentials);
        address.setId(idx.intValue());
        addressMap.put(idx.intValue(), address);
        address.setWalletId(this.getId());
        address.setChainType(this.getChain().getExchangeChain().getType());
        return address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Chain getChain() {
        return chain;
    }

    public void setChain(Chain chain) {
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
