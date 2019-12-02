package org.elastos.dto;

import org.elastos.pojo.ElaWalletAddress;

import javax.persistence.*;

@Entity
@Table(name = "gather_record")
public class GatherRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "chain_id", nullable = false)
    private Long chainId;
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;
    @Column(name = "address_id", nullable = false)
    private Integer addressId;
    @Column(name = "tx_hash", length = 256)
    private String txHash;

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

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public void ElaAddressToGather(ElaWalletAddress address) {
        this.setAddressId(address.getId());
        this.setWalletId(address.getWalletId());
    }

}


