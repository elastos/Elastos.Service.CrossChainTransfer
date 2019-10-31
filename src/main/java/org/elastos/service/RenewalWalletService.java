package org.elastos.service;

import com.alibaba.fastjson.JSON;
import jnr.ffi.annotations.Synchronized;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.DepositConfiguration;
import org.elastos.conf.EthGatherConfiguration;
import org.elastos.conf.NodeConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constants.RetCode;
import org.elastos.dao.AdminRepository;
import org.elastos.dao.InternalTxRepository;
import org.elastos.dao.RenewalWalletDbRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.dto.ExchangeRecord;
import org.elastos.dto.InternalTxRecord;
import org.elastos.dto.RenewalWalletDb;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.RenewalWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.elastos.constants.ExchangeState.EX_STATE_DIRECT_TRANSFER_FINISH;


@Service
public class RenewalWalletService {

    private static Logger logger = LoggerFactory.getLogger(RenewalWalletService.class);

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    DepositConfiguration depositConfiguration;

    @Autowired
    EthGatherConfiguration ethGatherConfiguration;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    @Autowired
    NodeConfiguration nodeConfiguration;

    @Autowired
    AdminRepository adminRepository;

    @Autowired
    RenewalWalletDbRepository renewalWalletDbRepository;

    @Autowired
    InternalTxRepository internalTxRepository;

    @Autowired
    ChainService chainService;

    @Autowired
    WalletBalanceService walletBalanceService;

    @Synchronized
    private Map<Long, RenewalWallet> renewalWallets = new HashMap<>();

    //Notice: If the renewal wallet is full, we generate a new one, so there must be more than 1 wallet for the same chain in the renewalWallets list.
    private RenewalWallet geneElaWallet(ExchangeChain chain, ChainService chainService) {
        String mnemonic = chainService.geneMnemonic(chain);
        if (null == mnemonic) {
            return null;
        }
        RenewalWallet renewalWallet = new RenewalWallet(chain, mnemonic, 0, redisTemplate);
        RenewalWalletDb wallets = saveWalletInfoToDb(chain.getId(), mnemonic);
        renewalWallet.setId(wallets.getId());
        renewalWallets.put(renewalWallet.getId(), renewalWallet);
        return renewalWallet;
    }

    private RenewalWalletDb saveWalletInfoToDb(Long chainId, String mnemonic) {
        RenewalWalletDb wallets = new RenewalWalletDb();
        wallets.setChainId(chainId);
        wallets.setMaxUse(0);
        wallets.setMnemonic(mnemonic);
        wallets = renewalWalletDbRepository.save(wallets);
        return wallets;
    }

    void initService() {
        List<RenewalWalletDb> wallets = (List<RenewalWalletDb>) renewalWalletDbRepository.findAll();
        for (RenewalWalletDb rw : wallets) {
            geneRenewalWalletByDb(rw);
        }
    }

    private RenewalWallet geneRenewalWalletByDb(RenewalWalletDb rw) {
        ExchangeChain chain = chainService.getChain(rw.getChainId());
        if (null == chain) {
            logger.error("geneRenewalWalletByDb not support chain id:" +rw.getChainId());
            return null;
        }
        RenewalWallet renewalWallet = new RenewalWallet(chain, rw.getMnemonic(), rw.getMaxUse(), redisTemplate);
        renewalWallet.setId(rw.getId());
        renewalWallets.put(renewalWallet.getId(), renewalWallet);
        return renewalWallet;
    }

    public ElaWalletAddress geneWalletAddress(ExchangeChain chain) {
        //todo change renewal wallet chain <->wallet one to one, and reuse address
        //todo or set a wallet state: using, used. and used wallet not show in service,just for gather.
        for (Map.Entry<Long, RenewalWallet> entry : renewalWallets.entrySet()) {
            RenewalWallet wallet = entry.getValue();
            if (chain.getId().equals(wallet.getChain().getId())) {
                ElaWalletAddress address = wallet.geneNewAddress(chainService);
                if (null != address) {
                    renewalWalletDbRepository.setMaxUse(wallet.getId(), wallet.getMaxUse());
                    return address;
                } else {
                    logger.error("Err geneWalletAddress wallet.geneNewAddress 1 failed. wallet id:" + wallet.getId());
                    return null;
                }
            }
        }

        //There is not a valid wallet address, we create a new wallet
        RenewalWallet wallet = geneElaWallet(chain, chainService);
        if (null == wallet) {
            logger.error("Err geneWalletAddress geneElaWallet 2 failed. chain id:" + chain.getId());
            return null;
        }
        ElaWalletAddress address = wallet.geneNewAddress(chainService);
        if (null != address) {
            renewalWalletDbRepository.setMaxUse(wallet.getId(), wallet.getMaxUse());
            return address;
        } else {
            logger.error("Err geneWalletAddress wallet.geneNewAddress 2 failed. wallet id:" + wallet.getId());
            return null;
        }
    }

    public ElaWalletAddress findAddress(Long walletId, Integer srcAddrId) {
        RenewalWallet wallet = renewalWallets.get(walletId);
        if (null == wallet) {
            //There must a new wallet in db. create by another service
            Optional<RenewalWalletDb> walletDb = renewalWalletDbRepository.findById(walletId);
            if (walletDb.isPresent()) {
                wallet = geneRenewalWalletByDb(walletDb.get());
            } else {
                logger.error("Can not find wallet by id:" + walletId);
                return null;
            }
        }

        ElaWalletAddress addr = wallet.getAddressMap().get(srcAddrId);
        if (null == addr) {
            addr = wallet.getAddress(srcAddrId, chainService);
        }
        return addr;
    }

    ExchangeRecord ethDirectTransferGather(ExchangeRecord tx) {
        Long srcWalletId = tx.getSrcWalletId();
        Integer srcAddrId = tx.getSrcAddressId();
        ElaWalletAddress srcElaWAddr = findAddress(srcWalletId, srcAddrId);
        if (null == srcElaWAddr) {
            logger.error("Err ethDirectTransferGather findAddress failed");
            return null;
        }

        ExchangeChain srcChain = renewalWallets.get(srcWalletId).getChain();
        if (null == srcChain) {
            logger.error("ethDirectTransferGather There is no srcChain srcWalletId:" + srcWalletId);
            return null;
        }

        Double value = chainService.getBalancesByAddr(srcChain, srcElaWAddr.getPublicAddress());
        if (null == value) {
            logger.error("ethDirectTransferGather chainService.getBalancesByAddr failed srcWalletId:" + srcWalletId + " srcAddressId:" + srcAddrId);
            return null;
        }
        value -= txBasicConfiguration.getETH_FEE();

        String txid = chainService.transfer(srcChain, srcElaWAddr.getPrivateKey(),
                srcChain.getType(),  ethGatherConfiguration.getAddress(), value);
        if (null == txid) {
            logger.error("ethDirectTransferGather tx failed srcWalletId:" + srcWalletId + " srcAddressId:" + srcAddrId);
        } else {
            tx.setDstTxid(txid);
            tx.setState(EX_STATE_DIRECT_TRANSFER_FINISH);
        }
        return tx;
    }

    ExchangeRecord directTransfer(ExchangeRecord tx) {
        Long srcWalletId = tx.getSrcWalletId();
        Integer srcAddrId = tx.getSrcAddressId();
        Long dstChainId = tx.getDstChainId();
        String dstAddress = tx.getDstAddress();
        ElaWalletAddress srcElaWAddr = findAddress(srcWalletId, srcAddrId);
        if (null == srcElaWAddr) {
            logger.error("Err directTransfer findAddress failed");
            return null;
        }

        ExchangeChain srcChain = renewalWallets.get(srcWalletId).getChain();
        if (null == srcChain) {
            logger.error("directTransfer There is no srcChain srcWalletId:" + srcWalletId);
            return null;
        }

        ExchangeChain dstChain = chainService.getExchangeChain(dstChainId);
        if (null == dstChain) {
            logger.error("directTransfer There is no dstChain id:" + dstChainId);
            return null;
        }
        
        Double value = chainService.getBalancesByAddr(srcChain, srcElaWAddr.getPublicAddress());
        if (null == value) {
            logger.error("directTransfer chainService.getBalancesByAddr failed. tx:" + tx.getId());
            return null;
        }
        value -= tx.getFee();
        value *= tx.getRate();
        tx.setDstValue(value);

        String txid = chainService.transfer(srcChain, srcElaWAddr.getPrivateKey(),
                dstChain.getType(), dstAddress, value);
        if (null == txid) {
            logger.error("directTransfer tx failed srcWalletId:" + srcWalletId + " srcAddressId:" + srcAddrId);
        } else {
            tx.setDstTxid(txid);
        }
        return tx;
    }

    String backRenewalEla(ExchangeRecord tx) {
        Long srcWalletId = tx.getSrcWalletId();
        Integer srcAddrId = tx.getSrcAddressId();
        String backAddr = tx.getBackAddress();

        ElaWalletAddress srcElaWAddr = findAddress(srcWalletId, srcAddrId);
        if (null == srcElaWAddr) {
            logger.error("Err backRenewalEla findAddress failed");
            return null;
        }

        ExchangeChain chain = renewalWallets.get(srcWalletId).getChain();

        ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
        List<String> priKeyList = new ArrayList<>();
        priKeyList.add(srcElaWAddr.getPrivateKey());
        Map<String, Double> dstMap = new HashMap<>();
        Double value = chainService.getBalancesByAddr(chain, srcElaWAddr.getPublicAddress());
        if (null == value) {
            logger.error("Err backRenewalEla chainService.getBalancesByAddr failed: tx:" + tx.getId());
            return null;
        }
        dstMap.put(backAddr, value - txBasicConfiguration.getELA_FEE());
        ReturnMsgEntity ret = elaDidService.transferEla(
                chain.getType(), priKeyList,
                chain.getType(), dstMap);
        if (ret.getStatus() != RetCode.SUCCESS) {
            logger.error("backRenewalEla tx failed srcWalletId:" + srcWalletId + " srcAddressId:" + srcAddrId + " result:" + ret.getResult());
            return null;
        }

        return (String) ret.getResult();
    }

    Double gatherAllRenewalWallet() {
        Double gatherValue = 0.0;
        for (Map.Entry<Long, RenewalWallet> entry : renewalWallets.entrySet()) {
            Long chainId = entry.getKey();
            ExchangeChain chain = chainService.getChain(chainId);
            RenewalWallet wallet = entry.getValue();

            List<String> priKeyList = new ArrayList<>();
            Double value = 0.0;
            for (int i = 0; i < wallet.getMaxUse(); i++) {
                ElaWalletAddress address = wallet.getAddress(i, chainService);
                if (null == address) {
                    logger.error("gatherAllRenewalWallet wallet.getAddress wallet id:" + wallet.getId() + " address id:" + i);
                    continue;
                }
                Double v = chainService.getBalancesByAddr(chain, address.getPublicAddress());
                if (null == v) {
                    logger.error("gatherAllRenewalWallet chainService.getBalancesByAddr wallet id:" + wallet.getId() + " address id:" + i);
                    continue;
                }
                if (v > 0.0) {
                    value += v;
                    priKeyList.add(address.getPrivateKey());
                }
            }

            if (priKeyList.isEmpty()) {
                logger.info("gatherAllRenewalWallet There is no ela in walletId:" + wallet.getId());
                continue;
            }

            //If there is cross chain transaction
            if (chain.getType().equals(ElaChainType.ELA_CHAIN)) {
                value -= txBasicConfiguration.getELA_FEE();
            } else {
                value -= txBasicConfiguration.getELA_CROSS_CHAIN_FEE() * 2;
            }
            Map<String, Double> dstMap = new HashMap<>();
            dstMap.put(depositConfiguration.getAddress(), value);

            ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
            ReturnMsgEntity ret = elaDidService.transferEla(
                    chain.getType(), priKeyList,
                    ElaChainType.ELA_CHAIN, dstMap);
            if (ret.getStatus() != RetCode.SUCCESS) {
                logger.error("gatherAllRenewalWallet tx failed walletId:" + wallet.getId() + " result:" + ret.getResult());
            } else {
                InternalTxRecord internalTxRecord = new InternalTxRecord();
                internalTxRecord.setSrcChainId(chainId);
                internalTxRecord.setSrcAddr(JSON.toJSONString(priKeyList));
                internalTxRecord.setDstChainId(chainService.getChain(ElaChainType.ELA_CHAIN).getId());
                internalTxRecord.setDstAddr(depositConfiguration.getAddress());
                internalTxRecord.setTxid((String) ret.getResult());
                internalTxRecord.setValue(value);
                internalTxRepository.save(internalTxRecord);
                logger.info("gatherAllRenewalWallet tx ok. walletId:" + wallet.getId() + " txid:" + ret.getResult());
                gatherValue += value;
            }
        }
        return gatherValue;
    }

}
