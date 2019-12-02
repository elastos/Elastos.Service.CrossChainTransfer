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
import org.elastos.service.ElaService;
import org.elastos.service.ElaTransferService;
import org.elastos.util.AsynProcSet;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DepositElaTask {
    private Logger logger = LoggerFactory.getLogger(DepositElaTask.class);
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
    private int procPerTime = 100;
    private AsynProcSet<ElaWalletAddress> elaOutputRenewalSet = new AsynProcSet<>();
    //2. 向main 归集(timer)
    private boolean gatherFlag;

    boolean isGatherFlag() {
        return gatherFlag;
    }

    void setGatherFlag(boolean gatherFlag) {
        this.gatherFlag = gatherFlag;
    }

    public void saveForRenewalOutput(ElaWalletAddress elaWalletAddress) {
        elaOutputRenewalSet.save2Set(elaWalletAddress);
    }

    void renewalOutput() {
        List<ElaWalletAddress> addressList = elaOutputRenewalSet.usingData(procPerTime);
        if (addressList.isEmpty()) {
            return;
        }

        Chain chain = chainService.getChain(ElaChainType.ELA_CHAIN);
        ElaService elaService = (ElaService) chain.getElaTransferService();
        DepositAddress depositAddr = depositWalletsService.getDepositeAddress(ElaChainType.ELA_CHAIN);
        Map<String, Double> dstMap = new HashMap<>();
        Double sum = 0.0;
        Double outputCapability = chain.getOutputCapability();
        for (ElaWalletAddress elaAddress : addressList) {
            String address = elaAddress.getCredentials().getAddress();
            RetResult<Double> valueRet = elaService.getBalance(elaAddress.getCredentials().getAddress());
            if (valueRet.getCode() != RetCode.SUCC) {
                elaOutputRenewalSet.backData(addressList);
                logger.debug("renewalOutput elaService.getBalance valueRet");
                return;
            }
            Double value = outputCapability - valueRet.getData();
            if (value > 0.0) {
                dstMap.put(address, value);
                sum += value;
            }
        }

        if (sum <= 0.0) {
            //No need to renewal
            elaOutputRenewalSet.releaseData(addressList);
        }

        sum += txBasicConfiguration.getELA_FEE();

        RetResult<Double> restRet = elaService.getBalance(depositAddr.getCredentials().getAddress());
        if (restRet.getCode() != RetCode.SUCC) {
            elaOutputRenewalSet.backData(addressList);
            return;
        }

        //If there is not enough ela in deposit, we renewal it.
        double rest = restRet.getData();
        if (rest < sum) {
            depositMainTask.saveForRenewal(depositAddr);
            elaOutputRenewalSet.backData(addressList);
            return;
        }

        List<String> srcPriKeys = new ArrayList<>();
        srcPriKeys.add(depositAddr.getCredentials().getKeyPair().getPrivateKey());
        RetResult<String> result = elaService.transferEla(srcPriKeys, dstMap);
        if (result.getCode() == RetCode.SUCC) {
            elaService.waitForTransactionReceipt(result.getData());
            elaOutputRenewalSet.releaseData(addressList);
            rest -= sum;
            if (rest < chain.getDepositRenewalThreshold()) {
                depositMainTask.saveForRenewal(depositAddr);
            }
        } else {
            elaOutputRenewalSet.backData(addressList);
        }
    }

    void gatherToMainDeposit() {
        Chain chain = chainService.getChain(ElaChainType.ELA_CHAIN);
        ElaTransferService transferService = chain.getElaTransferService();
        DepositAddress depositAddr = depositWalletsService.getDepositeAddress(ElaChainType.ELA_CHAIN);
        RetResult<Double> ret = transferService.getBalance(depositAddr.getCredentials().getAddress());
        if (ret.getCode() != RetCode.SUCC) {
            return;
        }

        Double capability = txBasicConfiguration.getDEPOSIT_ADDRESS_CAPABILITY()
                * txBasicConfiguration.getOUTPUT_ADDRESS_SUM()
                * txBasicConfiguration.getOUTPUT_ADDRESS_CAPABILITY()
                * chain.getExchangeChain().getThreshold_max();
        Double value = ret.getData() - capability - txBasicConfiguration.getELA_FEE();
        if (value <= 0.0) {
            return;
        }
        RetResult<String> retTxid = transferService.transfer(depositAddr.getCredentials(), mainDepositConfiguration.getAddress(), value);
        if (retTxid.getCode() == RetCode.SUCC) {
            transferService.waitForTransactionReceipt(retTxid.getData());
        }
    }
}
