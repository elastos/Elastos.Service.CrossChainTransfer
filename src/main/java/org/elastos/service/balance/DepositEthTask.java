package org.elastos.service.balance;

import org.elastos.POJO.ElaChainType;
import org.elastos.conf.MainDepositConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.pojo.Chain;
import org.elastos.pojo.DepositAddress;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.service.ChainService;
import org.elastos.service.DepositWalletsService;
import org.elastos.service.ElaTransferService;
import org.elastos.util.AsynProcSet;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class DepositEthTask {
    private Logger logger = LoggerFactory.getLogger(DepositEthTask.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private boolean onFlag = false;
    @Autowired
    ChainService chainService;

    @Autowired
    DepositWalletsService depositWalletsService;

    @Autowired
    DepositMainTask depositMainTask;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    @Autowired
    MainDepositConfiguration mainDepositConfiguration;


    //1. output余额不足，请求充值(转账提交)
    private int procPerTime = 1;
    private AsynProcSet<ElaWalletAddress> ethOutputRenewalSet = new AsynProcSet<>();
    //2. 向main 归集(timer)
    private boolean gatherFlag;

    public boolean isGatherFlag() {
        return gatherFlag;
    }

    public void setGatherFlag(boolean gatherFlag) {
        this.gatherFlag = gatherFlag;
    }

    public void saveForRenewalOutput(ElaWalletAddress elaWalletAddress) {
        ethOutputRenewalSet.save2Set(elaWalletAddress);
    }

    void renewalRestOutput(){
        for(int i = 0; i < ethOutputRenewalSet.size(); i++) {
            renewalOutput();
        }
    }

    void renewalOutput() {
        List<ElaWalletAddress> addressList = ethOutputRenewalSet.usingData(procPerTime);
        if (addressList.isEmpty()) {
            return;
        }
        Chain chain = chainService.getChain(ElaChainType.ETH_CHAIN);
        ElaTransferService transferService = chain.getElaTransferService();
        DepositAddress depositAddr = depositWalletsService.getDepositeAddress(ElaChainType.ETH_CHAIN);
        Double sum = 0.0;

        ElaWalletAddress elaAddress = addressList.get(0);
        String dstAddress = elaAddress.getCredentials().getAddress();
        RetResult<Double> valueRet = transferService.getBalance(elaAddress.getCredentials().getAddress());
        if (valueRet.getCode() != RetCode.SUCC) {
            ethOutputRenewalSet.backData(addressList);
            return;
        }
        Double outputCapability = chain.getOutputCapability();
        Double value = outputCapability - valueRet.getData();
        if (value <= 0.0) {
            //No need to renewal
            ethOutputRenewalSet.releaseData(addressList);
            return;
        }
        sum += value;

        // fee and save some eth for gas limit.
        sum += txBasicConfiguration.getETH_TRANSFER_GAS_SAVE();

        RetResult<Double> restRet = transferService.getBalance(depositAddr.getCredentials().getAddress());
        if (restRet.getCode() != RetCode.SUCC) {
            ethOutputRenewalSet.backData(addressList);
            return;
        }

        //If there is not enough eth in deposit, we renewal it.
        double rest = restRet.getData();
        if (rest < sum) {
            depositMainTask.saveForRenewal(depositAddr);
            ethOutputRenewalSet.backData(addressList);
            return;
        }

        RetResult<String> result = transferService.transfer(depositAddr.getCredentials(), dstAddress, sum);
        if (result.getCode() == RetCode.SUCC) {
            transferService.waitForTransactionReceipt(result.getData());
            ethOutputRenewalSet.releaseData(addressList);
            //If eth is less than threshold, we renewal it.
            rest -= sum;
            if (rest < chain.getDepositRenewalThreshold()) {
                depositMainTask.saveForRenewal(depositAddr);
            }
        } else {
            ethOutputRenewalSet.backData(addressList);
        }
    }

    void gatherToMainDeposit() {
        Chain chain = chainService.getChain(ElaChainType.ETH_CHAIN);
        ElaTransferService transferService = chain.getElaTransferService();
        DepositAddress depositAddr = depositWalletsService.getDepositeAddress(ElaChainType.ETH_CHAIN);
        RetResult<Double> ret = transferService.getBalance(depositAddr.getCredentials().getAddress());
        if (ret.getCode() != RetCode.SUCC) {
            return;
        }
        Double value = ret.getData() - chain.getDepositCapability()  - txBasicConfiguration.getETH_TRANSFER_CROSS_CHAIN_GAS_SAVE();
        if (value <= 0.0) {
            return;
        }

        RetResult<String> retTxid = transferService.transferToMainChain(depositAddr.getCredentials(), mainDepositConfiguration.getAddress(), value);
        if (retTxid.getCode() == RetCode.SUCC) {
            transferService.waitForTransactionReceipt(retTxid.getData());
        }
    }
}

