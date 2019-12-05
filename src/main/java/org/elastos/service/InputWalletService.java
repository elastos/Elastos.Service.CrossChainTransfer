package org.elastos.service;

import jnr.ffi.annotations.Synchronized;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.MainDepositConfiguration;
import org.elastos.conf.NodeConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.dao.AdminRepository;
import org.elastos.dao.InternalTxRepository;
import org.elastos.dao.InputWalletDbRepository;
import org.elastos.dto.ExchangeRecord;
import org.elastos.dto.InputWalletDb;
import org.elastos.pojo.Chain;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.InputWallet;
import org.elastos.util.RetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class InputWalletService {

    private static Logger logger = LoggerFactory.getLogger(InputWalletService.class);

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    MainDepositConfiguration mainDepositConfiguration;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    @Autowired
    NodeConfiguration nodeConfiguration;

    @Autowired
    AdminRepository adminRepository;

    @Autowired
    InputWalletDbRepository inputWalletDbRepository;

    @Autowired
    InternalTxRepository internalTxRepository;

    @Autowired
    ChainService chainService;

    @Synchronized
    private Map<Long, InputWallet> renewalWallets = new HashMap<>();

    //todo deal retireWallets wallets, gather done then set delete flag of it.
    //todo if there is no tx in this wallet, we gather it.
    @Synchronized
    private Map<Long, InputWallet> retireWallets = new HashMap<>();

    //Notice: If the renewal wallet is full, we generate a new one, so there must be more than 1 wallet for the same chain in the renewalWallets list.
    private InputWallet geneRenewalWallet(Long chainId) {
        Chain chain = chainService.getChain(chainId);
        if (null == chain) {
            logger.error("geneRenewalWallet not support chain id:" + chainId);
            return null;
        }


        String mnemonic = chain.getElaTransferService().createMnemonic();
        if (null == mnemonic) {
            logger.error("geneRenewalWallet getElaTransferService.createMnemonic failed. chain id:" + chainId);
            return null;
        }
        InputWallet inputWallet = new InputWallet(chain, mnemonic, 0, redisTemplate);
        InputWalletDb wallets = saveNewWalletInfoToDb(chainId, mnemonic);
        inputWallet.setId(wallets.getId());
        return inputWallet;
    }

    private InputWalletDb saveNewWalletInfoToDb(Long chainId, String mnemonic) {
        InputWalletDb wallets = new InputWalletDb();
        wallets.setChainId(chainId);
        wallets.setMaxUse(0);
        wallets.setMnemonic(mnemonic);
        wallets.setDel(false);
        wallets = inputWalletDbRepository.save(wallets);
        return wallets;
    }

    void initService() {
        List<InputWalletDb> wallets = inputWalletDbRepository.findByDel(false);
        for (InputWalletDb rw : wallets) {
            dealRenewalWalletDb(rw);
        }
    }

    private InputWallet dealRenewalWalletDb(InputWalletDb rw) {
        Chain chain = chainService.getChain(rw.getChainId());
        if (null == chain) {
            logger.error("dealRenewalWalletDb not support chain id:" + rw.getChainId());
            return null;
        }
        InputWallet inputWallet = new InputWallet(chain, rw.getMnemonic(), rw.getMaxUse(), redisTemplate);
        inputWallet.setId(rw.getId());
        if (rw.getMaxUse() < Integer.MAX_VALUE) {
            renewalWallets.put(rw.getChainId(), inputWallet);
        } else {
            //We put used wallet into retireWallets
            retireWallets.put(inputWallet.getId(), inputWallet);
        }
        return inputWallet;
    }

    public ElaWalletAddress geneWalletAddress(Long chainId) {
        InputWallet wallet = getRenewalWallet(chainId);
        if (null == wallet) {
            logger.error("geneWalletAddress getRenewalWallet failed. chain id:" + chainId);
            return null;
        }

        ElaWalletAddress address = wallet.geneNewAddress();
        if (null != address) {
            inputWalletDbRepository.setMaxUse(wallet.getId(), wallet.getMaxUse());
            return address;
        } else {
            logger.error("Err geneWalletAddress wallet.geneNewAddress failed. wallet id:" + wallet.getId());
            return null;
        }
    }

    private InputWallet getRenewalWallet(Long chainId) {
        InputWallet wallet = renewalWallets.get(chainId);
        if ((null == wallet) || (wallet.getMaxUse() >= Integer.MAX_VALUE)) {
            //There is not a valid wallet address(not support chain yet or just find a used wallet), we create a new wallet
            InputWallet w = geneRenewalWallet(chainId);
            if (null == w) {
                logger.error("Err getRenewalWallet geneRenewalWallet failed. chain id:" + chainId);
                return null;
            }
            renewalWallets.put(chainId, w);
            //We put used wallet in retireWallets
            if (null != wallet) {
                retireWallets.put(wallet.getId(), wallet);
            }
            return w;
        }

        return wallet;
    }

    public ElaWalletAddress findAddress(Long chainId, Long walletId, Integer srcAddrId) {
        InputWallet wallet = renewalWallets.get(chainId);
        if (null == wallet) {
            logger.error("findAddress Can not find wallet by chain id:" + chainId);
            return null;
        }

        if (!Objects.equals(wallet.getId(), walletId)) {
            InputWallet gatherWallet = retireWallets.get(walletId);
            if (null == gatherWallet) {
                logger.error("findAddress Can not find wallet by wallet id:" + walletId);
                return null;
            }
        }

        ElaWalletAddress addr = wallet.getAddress(srcAddrId);
        if (null == addr) {
            logger.error("findAddress wallet.getAddress failed. src chain id:" + chainId + " wallet id:" + walletId + " address id:" + srcAddrId);
        }
        return addr;
    }

    ExchangeRecord directTransfer(ExchangeRecord tx) {
        Long srcChainId = tx.getSrcChainId();
        Long srcWalletId = tx.getSrcWalletId();
        Integer srcAddrId = tx.getSrcAddressId();
        Long dstChainId = tx.getDstChainId();
        String dstAddress = tx.getDstAddress();
        ElaWalletAddress srcElaWAddr = findAddress(srcChainId, srcWalletId, srcAddrId);
        if (null == srcElaWAddr) {
            logger.error("Err directTransfer findAddress failed");
            return null;
        }

        Chain srcChain = chainService.getChain(srcChainId);
        if (null == srcChain) {
            logger.error("directTransfer There is no srcChain srcWalletId:" + srcWalletId);
            return null;
        }

        Chain dstChain = chainService.getChain(dstChainId);
        if (null == dstChain) {
            logger.error("directTransfer There is no dstChain id:" + dstChainId);
            return null;
        }

        if (srcChain.getExchangeChain().getType() != ElaChainType.ELA_CHAIN) {
            logger.error("directTransfer There is no support direct transfer from:" + srcChain.getExchangeChain().getType().toString()
                    + " to " + dstChain.getExchangeChain().getType().toString());
            return null;
        }

        ElaTransferService elaTransferService = srcChain.getElaTransferService();

        RetResult<Double> ret = elaTransferService.getBalance(srcElaWAddr.getCredentials().getAddress());
        if (ret.getCode() != RetCode.SUCC) {
            logger.error("directTransfer chainService.getBalancesByAddr failed. tx:" + tx.getId());
            return null;
        }
        Double value = ret.getData();

        value -= tx.getFee();
        value *= tx.getRate();
        tx.setDstValue(value);
        RetResult<String> retTxid;
        ElaChainType dstChainType = dstChain.getExchangeChain().getType();
        if (dstChainType == ElaChainType.ELA_CHAIN) {
            retTxid = elaTransferService.transfer(srcElaWAddr.getCredentials(), dstAddress, value);
        } else {
            retTxid = elaTransferService.transferToSideChain(srcElaWAddr.getCredentials(), dstChainType, dstAddress, value);
        }

        if (retTxid.getCode() == RetCode.SUCC) {
            tx.setDstTxid(retTxid.getData());
        } else {
            logger.error("directTransfer tx failed srcWalletId:" + srcWalletId + " srcAddressId:" + srcAddrId);
        }
        return tx;
    }

    String backInput(ExchangeRecord tx) {
        Long srcChainId = tx.getSrcChainId();
        Long srcWalletId = tx.getSrcWalletId();
        Integer srcAddrId = tx.getSrcAddressId();
        String backAddr = tx.getBackAddress();

        ElaWalletAddress srcElaWAddr = findAddress(srcChainId, srcWalletId, srcAddrId);
        if (null == srcElaWAddr) {
            logger.error("Err backInput findAddress failed");
            return null;
        }

        Chain srcChain = chainService.getChain(srcChainId);
        ElaTransferService transferService = srcChain.getElaTransferService();
        RetResult<Double> retValue = transferService.getBalance(srcElaWAddr.getCredentials().getAddress());
        if (retValue.getCode() != RetCode.SUCC) {
            logger.error("Err backInput chainService.getBalancesByAddr failed: tx:" + tx.getId());
            return null;
        }

        Double value = retValue.getData();
        RetResult<Double> retFee = transferService.estimateTransactionFee(tx.getSrcAddress(), srcElaWAddr.getChainType(), backAddr, value);
        if (retFee.getCode() != RetCode.SUCC) {
            logger.error("Err backInput chainService.estimateTransactionFee failed: tx:" + tx.getId());
            return null;
        }
        if (srcChain.getExchangeChain().getType().equals(ElaChainType.ETH_CHAIN)) {
            value -= txBasicConfiguration.getETH_TRANSFER_GAS_SAVE();
        } else {
            value -= retFee.getData();
        }

        RetResult<String> retTxid = transferService.transfer(srcElaWAddr.getCredentials(), backAddr, value);
        if (retTxid.getCode() != RetCode.SUCC) {
            logger.error("backInput tx failed srcWalletId:" + srcWalletId + " srcAddressId:" + srcAddrId + " result:" + retTxid.getMsg());
            return null;
        }

        return retTxid.getData();
    }
}
