package org.elastos.pojo;


import org.elastos.POJO.Credentials;
import org.elastos.POJO.ElaChainType;

public class DepositAddress {
    private ElaChainType chainType;
    private Credentials credentials;

    public ElaChainType getChainType() {
        return chainType;
    }

    public void setChainType(ElaChainType chainType) {
        this.chainType = chainType;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
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
        DepositAddress other = (DepositAddress) obj;
        // test whether the fields have identical values
        return chainType.equals(other.chainType)
                &&(getCredentials().getKeyPair().getPrivateKey().equals(other.getCredentials().getKeyPair().getPrivateKey()));
    }

    @Override
    public int hashCode() {
        return chainType.hashCode()+credentials.getKeyPair().getPrivateKey().hashCode();
    }

    public String toString(){
        String ret = "chain type:"+this.getChainType().toString()+" address:"+this.getCredentials().getAddress();
        return ret;
    }
}

