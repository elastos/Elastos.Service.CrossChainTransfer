package org.elastos.dto;

import org.elastos.pojo.ElaWalletAddress;

import javax.persistence.*;

@Entity
@Table(name = "gather_address")
public class GatherAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "chain_id", nullable = false)
    private Long chainId;
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;
    @Column(name = "address_id", nullable = false)
    private Long addressId;

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

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public void ElaAddressToGather(ElaWalletAddress address) {
        this.setAddressId(address.getId().longValue());
        this.setWalletId(address.getWalletId());
    }

}


