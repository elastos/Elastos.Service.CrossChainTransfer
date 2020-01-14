package org.elastos.service.balance;

import org.elastos.POJO.Credentials;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.MainDepositConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.pojo.Chain;
import org.elastos.pojo.DepositAddress;
import org.elastos.service.ChainService;
import org.elastos.service.DepositWalletsService;
import org.elastos.service.ElaTransferService;
import org.elastos.util.AsynProcSet;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DepositMainTask {
    private Logger logger = LoggerFactory.getLogger(DepositMainTask.class);

    @Autowired
    ChainService chainService;

    @Autowired
    DepositWalletsService depositWalletsService;

    @Autowired
    DepositElaTask depositElaTask;

    @Autowired
    DepositDidTask depositDidTask;

    @Autowired
    DepositEthTask depositEthTask;

    @Autowired
    MainDepositConfiguration mainDepositConfiguration;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    private AsynProcSet<DepositAddress> depositSet = new AsynProcSet<>();
    private int procPerTime = 1;

    public void saveForRenewal(DepositAddress address) {
        depositSet.save2Set(address);
    }

    void renewalRestDeposit() {
        for(int i = 0; i < depositSet.size(); i++) {
            renewalDeposit();
        }
    }

    void renewalDeposit() {
        List<DepositAddress> addressList = depositSet.usingData(procPerTime);
        if (addressList.isEmpty()) {
            logger.info("renewalDeposit addressList isEmpty.");
            return;
        }
        Chain chain = chainService.getChain(ElaChainType.ELA_CHAIN);
        ElaTransferService transferService = chain.getElaTransferService();
        Double sum = 0.0;

        DepositAddress depositAddress = addressList.get(0);
        String dstAddress = depositAddress.getCredentials().getAddress();
        ElaChainType dstChainType = depositAddress.getChainType();
        Chain dstChain = chainService.getChain(dstChainType);
        RetResult<Double> valueRet = dstChain.getElaTransferService().getBalance(dstAddress);
        if (valueRet.getCode() != RetCode.SUCC) {
            depositSet.backData(addressList);
            logger.info("renewalDeposit dstAddress getBalance failed."+ depositAddress.toString());
            return;
        }

        Double depositCapability = dstChain.getDepositCapability();
        Double value = depositCapability - valueRet.getData();
        sum += value;
        if (sum <= 0.0) {
            //No need to renewal
            depositSet.releaseData(addressList);
            logger.info("renewalDeposit no need to renewal."+ depositAddress.toString());
            return;
        }


        RetResult<Double> feeRet = transferService.estimateTransactionFee(
                mainDepositConfiguration.getAddress(), dstChainType, dstAddress, sum);
        if (feeRet.getCode() != RetCode.SUCC) {
            logger.info("renewalDeposit estimateTransactionFee failed."+ depositAddress.toString());
            depositSet.backData(addressList);
            return;
        }
        sum += feeRet.getData();

        RetResult<Double> restRet = transferService.getBalance(mainDepositConfiguration.getAddress());
        if (restRet.getCode() != RetCode.SUCC) {
            logger.info("renewalDeposit ela main chain getBalance failed."+ depositAddress.toString());
            depositSet.backData(addressList);
            return;
        }

        //If there is not enough eth in deposit, we renewal it.
        double rest = restRet.getData();
        if (rest < sum) {
            logger.info("renewalDeposit ela main chain not enough rest."+ depositAddress.toString());
            this.gatherDepositTask();
            depositSet.backData(addressList);
            return;
        }

        Credentials credentials = transferService.geneCredentialsByPrivateKey(mainDepositConfiguration.getPrivateKey());
        RetResult<String> result;
        if (dstChainType.equals(ElaChainType.ELA_CHAIN)) {
            result = transferService.transfer(credentials, dstAddress, sum);
        } else {
            result = transferService.transferToSideChain(credentials, dstChainType, dstAddress, sum);
        }
        if (result.getCode() == RetCode.SUCC) {
            transferService.waitForTransactionReceipt(result.getData());
            depositSet.releaseData(addressList);
            logger.info("renewalDeposit transfer finish."+ depositAddress.toString());
        } else {
            logger.info("renewalDeposit transfer failed."+ depositAddress.toString() +" result:" +result.getMsg());
            depositSet.backData(addressList);
        }
    }

    public void gatherDepositTask() {
        depositElaTask.setGatherFlag(true);
        depositDidTask.setGatherFlag(true);
        depositEthTask.setGatherFlag(true);
    }
}

