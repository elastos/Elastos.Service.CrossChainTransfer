package org.elastos.service;

import org.elastos.POJO.Credentials;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.MainDepositConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.dao.InternalTxRepository;
import org.elastos.dto.InternalTxRecord;
import org.elastos.pojo.Chain;
import org.elastos.pojo.DepositAddress;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.service.balance.DepositDidProc;
import org.elastos.service.balance.DepositElaProc;
import org.elastos.service.balance.DepositEthProc;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class DepositWalletsService {
    @Autowired
    DepositElaProc depositElaProc;

    @Autowired
    DepositDidProc depositDidProc;

    @Autowired
    DepositEthProc depositEthProc;

    @Autowired
    ChainService chainService;

    @Autowired
    MainDepositConfiguration mainDepositConfiguration;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    @Autowired
    InternalTxRepository internalTxRepository;

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
                depositElaProc.saveForRenewalOutput(elaAddress);
                break;
            case DID_CHAIN:
                depositDidProc.saveForRenewalOutput(elaAddress);
                break;
            case ETH_CHAIN:
                depositEthProc.saveForRenewalOutput(elaAddress);
                break;
            default:
                logger.error("saveForRenewal address id:"+elaAddress.getId()+" chain type:" + elaAddress.getChainType());
        }
    }

    public void gatherAllToMainDeposit(){
        for (DepositAddress address : depositMap.values()) {
            String srcAddress = address.getCredentials().getAddress();
            String dstAddress = mainDepositConfiguration.getAddress();
            Chain chain = chainService.getChain(address.getChainType());
            ElaTransferService transferService = chain.getElaTransferService();
            RetResult<Double> valueRet = transferService.getBalance(srcAddress);
            if (valueRet.getCode() != RetCode.SUCC) {
                logger.error("gatherAllToMainDeposit transferService.getBalance failed. address:" + srcAddress + " msg:" + valueRet.getMsg());
                continue;
            }

            Double value = valueRet.getData();
            double fee = 0.0;
            if (address.getChainType().equals(ElaChainType.ETH_CHAIN)) {
                fee = txBasicConfiguration.getETH_TRANSFER_CROSS_CHAIN_GAS_SAVE();
            } else {
                RetResult<Double> feeRet = transferService.estimateTransactionFee(srcAddress, ElaChainType.ELA_CHAIN, dstAddress, value);
                if (feeRet.getCode() != RetCode.SUCC) {
                    logger.error("gatherAllToMainDeposit transferService.estimateTransactionFee failed. address:" + srcAddress + " msg:" + feeRet.getMsg());
                    continue;
                }
                fee = feeRet.getData();
            }

            if (value <= fee) {
                logger.info("gatherAllToMainDeposit no need gather. address:" + srcAddress + "rest:" + value);
                continue;
            }

            value -= fee;

            RetResult<String> ret;
            if(address.getChainType().equals(ElaChainType.ELA_CHAIN)) {
                ret= transferService.transfer(address.getCredentials(), dstAddress, value);
            } else {
                ret= transferService.transferToMainChain(address.getCredentials(), dstAddress, value);
            }
            if (ret.getCode() == RetCode.SUCC) {
                InternalTxRecord internalTxRecord = new InternalTxRecord();
                internalTxRecord.setSrcChainId(chain.getExchangeChain().getId());
                internalTxRecord.setSrcAddr(srcAddress);
                internalTxRecord.setDstChainId(chain.getExchangeChain().getId());
                internalTxRecord.setDstAddr(dstAddress);
                internalTxRecord.setTxid(ret.getData());
                internalTxRecord.setValue(value);
                internalTxRepository.save(internalTxRecord);
                logger.info("gatherAllToMainDeposit tx ok. address:" + srcAddress + " txid:" + ret.getData());
            } else {
                logger.info("gatherAllToMainDeposit tx failed. address:" + srcAddress + " msg:" + ret.getMsg()+ " code:" + ret.getCode());
            }

        }
    }

}
