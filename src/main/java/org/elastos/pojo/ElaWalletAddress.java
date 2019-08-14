package org.elastos.pojo;


public class ElaWalletAddress {
    private Integer id;
    private Long walletId;
    private String privateKey;
    private String publicKey;
    private String publicAddress;
    private Double rest;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public void setPublicAddress(String publicAddress) {
        this.publicAddress = publicAddress;
    }

    public Double getRest() {
        return rest;
    }

    public void setRest(Double rest) {
        this.rest = rest;
    }


    @Override
    public boolean equals(Object obj) {
        // must return false if the explicit parameter is null
        if (obj == null)
            return false;
        // a quick test to see if the objects are identical
        if (this == obj)
            return true;
        // if the class don't match,they can't be equal
        if (getClass() != obj.getClass())
            return false;
        // now we know obj is non-null Employee
        ElaWalletAddress other = (ElaWalletAddress) obj;
        // test whether the fields have identical values
        return walletId.equals(other.walletId)&&id.equals(other.id)&&(privateKey.equals(other.privateKey));
    }

    @Override
    public int hashCode() {
        return walletId.hashCode()+id.hashCode()+privateKey.hashCode();
    }
}

