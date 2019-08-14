package org.elastos.util;

import com.alibaba.fastjson.JSON;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.ela.ElaHdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

public class ExchangeWallet {
    private static Logger logger = LoggerFactory.getLogger(ExchangeWallet.class);
    private Long id;
    private Long chainId;
    private String mnemonic;
    private Integer sum;
    private Double value;
    private List<ElaWalletAddress> addressList = new ArrayList<>();
    private DCountUtil dCountUtil = null;

    public ExchangeWallet(Long id, String m, Integer s, RedisTemplate<String, Object> redisTemplate) {
        chainId = id;
        mnemonic = m;
        sum = s;
        SimpleHash hash = new SimpleHash("md5", mnemonic, chainId.toString(), 2);
        String name = hash.toHex();
        dCountUtil = new DCountUtil(name, -1L, redisTemplate);
    }

    public void initAddresses() {
        int sum = this.getSum();
        for (int i = 0; i < sum; i++) {
            String walletStr;
            try {
                walletStr = ElaHdSupport.generate(mnemonic, i);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Err geneNewAddress ElaHdSupport.generate Exception!!");
                return;
            }
            ElaWalletAddress address = JSON.parseObject(walletStr, ElaWalletAddress.class);
            address.setId(i);
            address.setWalletId(this.getId());
            addressList.add(address);
        }
        logger.info("Exchange wallet " + sum + " address ok");
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

    public Integer getSum() {
        return sum;
    }

    public List<ElaWalletAddress> getAddressList() {
        return addressList;
    }

    public ElaWalletAddress getExchangeAddress() {
        Long idx = dCountUtil.inc();
        if (idx >= sum) {
            dCountUtil.set(0L);
        }
        return addressList.get(idx.intValue());
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
