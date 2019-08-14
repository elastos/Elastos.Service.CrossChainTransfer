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
    @Column(name = "chain_url_prefix", nullable = false)
    private String chainUrlPrefix;
    @Column(name = "elastos_chain_type", nullable = false)
    private ElaChainType type;

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
}
