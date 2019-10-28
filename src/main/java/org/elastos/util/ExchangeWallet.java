package org.elastos.util;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.elastos.POJO.ChainCredentials;
import org.elastos.dto.ExchangeChain;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.service.ChainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

public class ExchangeWallet {
    private static Logger logger = LoggerFactory.getLogger(ExchangeWallet.class);
    private Long id;
    private ExchangeChain chain;
    private String mnemonic;
    private Integer sum;
    private Double value;
    private List<ElaWalletAddress> addressList = new ArrayList<>();
    private DCountUtil dCountUtil = null;

    public ExchangeWallet(ExchangeChain exChain, String m, Integer s, RedisTemplate<String, Object> redisTemplate) {
        chain = exChain;
        mnemonic = m;
        sum = s;
        SimpleHash hash = new SimpleHash("md5", mnemonic, exChain.getId().toString(), 2);
        String name = hash.toHex();
        dCountUtil = new DCountUtil(name, -1L, redisTemplate);
    }

    public void initAddresses(ChainService chainService) {
        int sum = this.getSum();
        for (int i = 0; i < sum; i++) {
            ChainCredentials credentials = chainService.geneAddress(chain, mnemonic, i);
            if (null == credentials) {
                logger.error("Err initAddresses ChainService.geneAddress failed");
                return;
            }

            ElaWalletAddress address = new ElaWalletAddress();
            address.setPrivateKey(credentials.getKeyPair().getPrivateKey());
            address.setPublicKey(credentials.getKeyPair().getPublicKey());
            address.setPublicAddress(credentials.getAddress());
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

    public ExchangeChain getChain() {
        return chain;
    }

    public void setChain(ExchangeChain chain) {
        this.chain = chain;
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
