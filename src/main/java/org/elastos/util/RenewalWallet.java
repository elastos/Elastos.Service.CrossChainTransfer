package org.elastos.util;

import com.alibaba.fastjson.JSON;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.elastos.exception.ElastosServiceException;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.ela.ElaHdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

public class RenewalWallet {
    private Long id;
    private Long chainId;
    private String mnemonic;
    private Integer maxUse;
    private Double value;
    private Map<Integer, ElaWalletAddress> addressMap = new HashMap<>();
    private DCountUtil dCountUtil = null;
    private static Logger logger = LoggerFactory.getLogger(RenewalWallet.class);

    public RenewalWallet(Long cid, String m, Integer max, RedisTemplate<String, Object> redisTemplate) {
        chainId = cid;
        mnemonic = m;
        SimpleHash hash = new SimpleHash("md5", mnemonic, chainId.toString(), 2);
        String name = hash.toHex();
        if (0 != max) {
            dCountUtil = new DCountUtil(name, Long.valueOf(max)-1, redisTemplate);
        } else {
            dCountUtil = new DCountUtil(name, -1L, redisTemplate);
        }
        maxUse = max;
    }

    public ElaWalletAddress getAddress(int idx) throws ElastosServiceException{
        String walletStr;
        try {
            walletStr = ElaHdSupport.generate(mnemonic, idx);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Err geneNewAddress ElaHdSupport.generate Exception!!");
            throw new ElastosServiceException("geneNewAddress ElaHdSupport.generate Exception", e);
        }
        if(maxUse < (idx+1)){
            maxUse = idx+1;
        }
        ElaWalletAddress address = JSON.parseObject(walletStr, ElaWalletAddress.class);
        address.setId(idx);
        address.setWalletId(this.getId());
        addressMap.put(idx, address);
        return address;
    }

    public ElaWalletAddress geneNewAddress() throws ElastosServiceException{
        Long idx = dCountUtil.inc();
        if (idx > Integer.MAX_VALUE) {
            logger.error("Ela wallet has generated max address");
            return null;
        }
        String walletStr;
        try {
            walletStr = ElaHdSupport.generate(mnemonic, idx.intValue());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Err geneNewAddress ElaHdSupport.generate Exception!!");
            throw new ElastosServiceException("geneNewAddress ElaHdSupport.generate Exception", e);
        }
        maxUse = idx.intValue()+1;
        ElaWalletAddress address = JSON.parseObject(walletStr, ElaWalletAddress.class);
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

    public Long getChainId() {
        return chainId;
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
