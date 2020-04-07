package org.elastos.service.balance;

import org.elastos.POJO.ElaChainType;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.dao.GatherRecordRepository;
import org.elastos.dto.GatherRecord;
import org.elastos.pojo.Chain;
import org.elastos.pojo.DepositAddress;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.service.ChainService;
import org.elastos.service.DepositWalletsService;
import org.elastos.service.ElaTransferService;
import org.elastos.service.InputWalletService;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GatherInputTask {
    private Logger logger = LoggerFactory.getLogger(GatherInputTask.class);

    private boolean onFlag = true;

    @Autowired
    ChainService chainService;

    @Autowired
    DepositWalletsService depositWalletsService;

    @Autowired
    InputWalletService inputWalletService;

    @Autowired
    GatherRecordRepository gatherRecordRepository;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    public void setOnFlag(boolean onFlag) {
        this.onFlag = onFlag;
    }

    public boolean isOnFlag() {
        return onFlag;
    }

    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 60 * 1000)
    public void getGatherInputDataTask() {
        if (!onFlag) {
            return;
        }
        getGatherInputData();
    }

    public void getGatherInputData() {
        List<GatherRecord> gatherRecords = gatherRecordRepository.findAllByTxHashIsNull();
        if (gatherRecords.isEmpty()) {
            return;
        }

        for (GatherRecord record : gatherRecords) {
            proc(record);
        }
    }

    private RetResult proc(GatherRecord record) {
        Chain chain = chainService.getChain(record.getChainId());
        ElaTransferService transferService = chain.getElaTransferService();
        ElaWalletAddress srcElaAddress = inputWalletService.findAddress(record.getChainId(), record.getWalletId(), record.getAddressId());
        String srcAddress = srcElaAddress.getCredentials().getAddress();
        DepositAddress depositAddress = depositWalletsService.getDepositeAddress(srcElaAddress.getChainType());
        String dstAddress = depositAddress.getCredentials().getAddress();

        RetResult<Double> valueRet = transferService.getBalance(srcAddress);
        if (valueRet.getCode() != RetCode.SUCC) {
            logger.error("proc transferService.getBalance failed. address:" + srcAddress + " msg:" + valueRet.getMsg());
            return new RetResult().setCode(valueRet.getCode())
                    .setMsg("proc transferService.getBalance failed. address:" + srcAddress + " msg:" + valueRet.getMsg());
        }

        Double value = valueRet.getData();
        double fee = 0.0;
        if (chain.getExchangeChain().getType().equals(ElaChainType.ETH_CHAIN)) {
            fee = txBasicConfiguration.getETH_TRANSFER_GAS_SAVE();
        } else {
            RetResult<Double> feeRet = transferService.estimateTransactionFee(srcAddress, depositAddress.getChainType(), dstAddress, value);
            if (feeRet.getCode() != RetCode.SUCC) {
                logger.error("proc transferService.estimateTransactionFee failed. address:" + srcAddress + " msg:" + feeRet.getMsg());
                return new RetResult().setCode(feeRet.getCode())
                        .setMsg("proc transferService.estimateTransactionFee failed. address:" + srcAddress + " msg:" + feeRet.getMsg());
            }
            fee = feeRet.getData();
        }

        if (value <= fee) {
            if (value == 0.0) {
                //预转账模式，value为0， 增加等待时间, 并且如果持续未到账，需要记录
                logger.info("value is 0. address is:" + srcAddress);
                return new RetResult().setCode(RetCode.NOT_FOUND);
            }
            record.setTxHash("rest is:" + value);
            gatherRecordRepository.save(record);
            return new RetResult().setCode(RetCode.BAD_REQUEST);
        }

        value -= fee;

        RetResult<String> ret = transferService.transfer(srcElaAddress.getCredentials(), dstAddress, value);
        if (ret.getCode() == RetCode.SUCC) {
            record.setTxHash(ret.getData());
            gatherRecordRepository.save(record);
        }
        return ret;
    }
}

