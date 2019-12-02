package org.elastos.util;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.elastos.POJO.Credentials;
import org.elastos.constant.RetCode;
import org.elastos.pojo.Chain;
import org.elastos.pojo.ElaWalletAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

public class OutputWallet {
    private Long id;
    private Chain chain;
    private String mnemonic;
    private Integer sum;
    private Double value;
    private List<ElaWalletAddress> addressList = new ArrayList<>();
    private DCountUtil dCountUtil = null;

    private static Logger logger = LoggerFactory.getLogger(OutputWallet.class);

    public OutputWallet(Chain exChain, String m, Integer s, RedisTemplate<String, Object> redisTemplate) {
        chain = exChain;
        mnemonic = m;
        sum = s;
        SimpleHash hash = new SimpleHash("md5", mnemonic, exChain.getExchangeChain().getId().toString(), 2);
        String name = hash.toHex();
        dCountUtil = new DCountUtil(name, -1L, redisTemplate);
    }

    public void initAddresses() {
        int sum = this.getSum();
        for (int i = 0; i < sum; i++) {
            RetResult<Credentials>  ret = chain.getElaTransferService().geneCredentials(mnemonic, i);
            if (ret.getCode() != RetCode.SUCC) {
                logger.error("Err initAddresses getElaTransferService.geneCredentials failed. " + ret.getMsg());
                return;
            }
            Credentials credentials = ret.getData();
            ElaWalletAddress address = new ElaWalletAddress();
            address.setCredentials(credentials);
            address.setId(i);
            address.setWalletId(this.getId());
            address.setChainType(this.getChain().getExchangeChain().getType());
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

    public Chain getChain() {
        return chain;
    }

    public void setChain(Chain chain) {
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
