package org.elastos.dto;

import javax.persistence.*;

@Entity
@Table(name="exchange_wallets")
public class ExchangeWalletDb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="chain_id")
    private Long chainId;
    @Column(name="mnemonic", nullable = false, length = 256)
    private String mnemonic;
    @Column(name="sum")
    private Integer sum;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChainId() {
        return chainId;
    }

    public void setChainId(Long chainId) {
        this.chainId = chainId;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    public Integer getSum() {
        return sum;
    }

    public void setSum(Integer sum) {
        this.sum = sum;
    }
}
