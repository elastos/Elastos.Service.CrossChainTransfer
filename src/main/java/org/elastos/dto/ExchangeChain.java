package org.elastos.dto;

import org.elastos.POJO.ElaChainType;

import javax.persistence.*;

@Entity
@Table(name = "exchange_chain")
public class ExchangeChain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "chain_name", nullable = false, length = 100)
    private String chainName;
    @Column(name = "elastos_chain_type", nullable = false)
    private ElaChainType type;
    @Column(name = "chain_url_prefix", nullable = false)
    private String chainUrlPrefix;
    @Column(name = "is_test_net", nullable = false)
    private Boolean isTest;
    @Column(name = "deposit_mnemonic", length = 256)
    private String mnemonic;
    @Column(name = "deposit_address_index")
    private Integer index;
    @Column(name = "threshold_min", nullable = false)
    private Double threshold_min;
    @Column(name = "threshold_max", nullable = false)
    private Double threshold_max;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    public String getChainUrlPrefix() {
        return chainUrlPrefix;
    }

    public void setChainUrlPrefix(String chainUrlPrefix) {
        this.chainUrlPrefix = chainUrlPrefix;
    }

    public ElaChainType getType() {
        return type;
    }

    public void setType(ElaChainType type) {
        this.type = type;
    }

    public Boolean getTest() {
        return isTest;
    }

    public void setTest(Boolean test) {
        isTest = test;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Double getThreshold_min() {
        return threshold_min;
    }

    public void setThreshold_min(Double threshold_min) {
        this.threshold_min = threshold_min;
    }

    public Double getThreshold_max() {
        return threshold_max;
    }

    public void setThreshold_max(Double threshold_max) {
        this.threshold_max = threshold_max;
    }
}
