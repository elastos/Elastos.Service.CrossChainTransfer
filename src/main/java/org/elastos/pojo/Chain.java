package org.elastos.pojo;

import org.elastos.conf.TxBasicConfiguration;
import org.elastos.dto.ExchangeChain;
import org.elastos.service.ElaTransferService;

public class Chain {
    ExchangeChain exchangeChain;
    ElaTransferService elaTransferService;
    Double outputCapability;
    Double outputRenewalThreshold;
    Double depositCapability;
    Double depositRenewalThreshold;

    public ExchangeChain getExchangeChain() {
        return exchangeChain;
    }

    public void setExchangeChain(ExchangeChain exchangeChain) {
        this.exchangeChain = exchangeChain;
    }

    public ElaTransferService getElaTransferService() {
        return elaTransferService;
    }

    public void setElaTransferService(ElaTransferService elaTransferService) {
        this.elaTransferService = elaTransferService;
    }

    public void initBalanceData(TxBasicConfiguration txBasicConfiguration){
        outputCapability = this.getExchangeChain().getThreshold_max() * txBasicConfiguration.getOUTPUT_ADDRESS_CAPABILITY();
        outputRenewalThreshold = outputCapability * txBasicConfiguration.getOUTPUT_ADDRESS_SUPPLY_THRESHOLD();
        depositCapability = outputCapability * txBasicConfiguration.getOUTPUT_ADDRESS_SUM() * txBasicConfiguration.getDEPOSIT_ADDRESS_CAPABILITY();
        depositRenewalThreshold= depositCapability * txBasicConfiguration.getDEPOSIT_ADDRESS_SUPPLY_THRESHOLD();
    }

    public Double getOutputCapability() {
        return outputCapability;
    }

    public void setOutputCapability(Double outputCapability) {
        this.outputCapability = outputCapability;
    }

    public Double getOutputRenewalThreshold() {
        return outputRenewalThreshold;
    }

    public void setOutputRenewalThreshold(Double outputRenewalThreshold) {
        this.outputRenewalThreshold = outputRenewalThreshold;
    }

    public Double getDepositCapability() {
        return depositCapability;
    }

    public void setDepositCapability(Double depositCapability) {
        this.depositCapability = depositCapability;
    }

    public Double getDepositRenewalThreshold() {
        return depositRenewalThreshold;
    }

    public void setDepositRenewalThreshold(Double depositRenewalThreshold) {
        this.depositRenewalThreshold = depositRenewalThreshold;
    }
}
