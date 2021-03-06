package org.elastos.pojo;


import org.elastos.POJO.Credentials;
import org.elastos.POJO.ElaChainType;

public class ElaWalletAddress {
    private Integer id;
    private ElaChainType chainType;
    private Long walletId;
    private Credentials credentials;
    private Double value = 0.0;
    private String inTx = null;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ElaChainType getChainType() {
        return chainType;
    }

    public void setChainType(ElaChainType chainType) {
        this.chainType = chainType;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }


    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getInTx() {
        return inTx;
    }

    public void setInTx(String inTx) {
        this.inTx = inTx;
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
        return walletId.equals(other.walletId)&&id.equals(other.id)
                &&(getCredentials().getKeyPair().getPrivateKey().equals(other.getCredentials().getKeyPair().getPrivateKey()));
    }

    @Override
    public int hashCode() {
        return walletId.hashCode()+id.hashCode()+credentials.getKeyPair().getPrivateKey().hashCode();
    }
}

