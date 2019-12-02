package org.elastos.service;

import javafx.print.Collation;
import org.elastos.POJO.Credentials;
import org.elastos.POJO.ElaChainType;
import org.elastos.constants.AddressState;
import org.elastos.pojo.DepositAddress;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.service.balance.DepositDidTask;
import org.elastos.service.balance.DepositElaTask;
import org.elastos.service.balance.DepositEthTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class DepositWalletsService {
    @Autowired
    DepositElaTask depositElaTask;

    @Autowired
    DepositDidTask depositDidTask;

    @Autowired
    DepositEthTask depositEthTask;

    private static Logger logger = LoggerFactory.getLogger(DepositWalletsService.class);
    private Map<ElaChainType, DepositAddress> depositMap = new HashMap<>();

    public void putDepositMap(ElaChainType type, Credentials credentials) {
        DepositAddress depositAddress = new DepositAddress();
        depositAddress.setChainType(type);
        depositAddress.setCredentials(credentials);
        this.depositMap.put(type, depositAddress);
    }

    public Collection<DepositAddress> getAllDepositAddress(){
        return depositMap.values();
    }

    public DepositAddress getDepositeAddress(ElaChainType type) {
        return this.depositMap.get(type);
    }

    public void saveForRenewal(ElaWalletAddress elaAddress) {
        switch (elaAddress.getChainType()) {
            case ELA_CHAIN:
                depositElaTask.saveForRenewalOutput(elaAddress);
                break;
            case DID_CHAIN:
                depositDidTask.saveForRenewalOutput(elaAddress);
                break;
            case ETH_CHAIN:
                depositEthTask.saveForRenewalOutput(elaAddress);
                break;
            default:
                logger.error("saveForRenewal address id:"+elaAddress.getId()+" chain type:" + elaAddress.getChainType());
        }
    }

}
