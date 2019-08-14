package org.elastos.dto;

import javax.persistence.*;

@Entity
@Table(name="renewal_wallets")
public class RenewalWalletDb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="chain_id")
    private Long chainId;
    @Column(name="mnemonic", nullable = false, length = 100)
    private String mnemonic;
    @Column(name="max_use")
    private Integer maxUse;

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

    public Integer getMaxUse() {
        return maxUse;
    }

    public void setMaxUse(Integer maxUse) {
        this.maxUse = maxUse;
    }
}
