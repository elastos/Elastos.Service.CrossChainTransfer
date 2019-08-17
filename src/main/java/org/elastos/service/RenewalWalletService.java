package org.elastos.service;

import com.alibaba.fastjson.JSON;
import jnr.ffi.annotations.Synchronized;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.DepositConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constants.RetCode;
import org.elastos.dao.AdminRepository;
import org.elastos.dao.InternalTxRepository;
import org.elastos.dao.RenewalWalletDbRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.dto.InternalTxRecord;
import org.elastos.dto.RenewalWalletDb;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.exception.ElastosServiceException;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.RenewalWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class RenewalWalletService {

    private static Logger logger = LoggerFactory.getLogger(RenewalWalletService.class);

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    DepositConfiguration depositConfiguration;

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

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
    private RenewalWallet geneElaWallet(Long chainId) {
        ElaDidService elaDidService = new ElaDidService();
        String mnemonic = elaDidService.createMnemonic();
        RenewalWallet renewalWallet = new RenewalWallet(chainId, mnemonic, 0, redisTemplate);
        RenewalWalletDb wallets = saveWalletInfoToDb(chainId, mnemonic);
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
        RenewalWallet renewalWallet = new RenewalWallet(rw.getChainId(), rw.getMnemonic(), rw.getMaxUse(), redisTemplate);
        renewalWallet.setId(rw.getId());
        renewalWallets.put(renewalWallet.getId(), renewalWallet);
        return renewalWallet;
    }

    public ElaWalletAddress geneWalletAddress(long chainId) {
        for (Map.Entry<Long, RenewalWallet> entry : renewalWallets.entrySet()) {
            RenewalWallet wallet = entry.getValue();
            if (chainId == wallet.getChainId()) {
                ElaWalletAddress address = wallet.geneNewAddress();
                if (null != address) {
                    renewalWalletDbRepository.setMaxUse(wallet.getId(), wallet.getMaxUse());
                    return address;
                }
            }
        }

        //There is not a valid wallet address, we create a new wallet
        RenewalWallet wallet = geneElaWallet(chainId);
        ElaWalletAddress address = wallet.geneNewAddress();
        if (null != address) {
            renewalWalletDbRepository.setMaxUse(wallet.getId(), wallet.getMaxUse());
            return address;
        } else {
            throw new RuntimeException("Generate no wallet in chain:" + chainId);
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
                throw new ElastosServiceException("Can not find wallet by id:" + walletId);
            }
        }

        ElaWalletAddress addr = wallet.getAddressMap().get(srcAddrId);
        if (null == addr) {
            addr = wallet.getAddress(srcAddrId);
        }
        return addr;
    }

    String backRenewalEla(Long srcWalletId, Integer srcAddrId, String backAddr) {
        ElaWalletAddress srcElaWAddr = findAddress(srcWalletId, srcAddrId);
        Long chainId = renewalWallets.get(srcWalletId).getChainId();
        ExchangeChain chain = chainService.getExchangeChain(chainId);
        if (null == chain) {
            logger.error("backRenewalEla There is no chain id:" + chainId);
            return null;
        }

        ElaDidService elaDidService = new ElaDidService();
        List<String> priKeyList = new ArrayList<>();
        priKeyList.add(srcElaWAddr.getPrivateKey());
        Map<String, Double> dstMap = new HashMap<>();
        Double value = chainService.getBalancesByAddr(chainId, srcElaWAddr.getPublicAddress());
        dstMap.put(backAddr, value - txBasicConfiguration.getFEE());
        ReturnMsgEntity ret = elaDidService.transferEla(chain.getChainUrlPrefix(),
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
                ElaWalletAddress address = wallet.getAddress(i);
                Double v = chainService.getBalancesByAddr(chainId, address.getPublicAddress());
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
                value -= txBasicConfiguration.getFEE();
            } else {
                value -= txBasicConfiguration.getCROSS_CHAIN_FEE() * 2;
            }
            Map<String, Double> dstMap = new HashMap<>();
            dstMap.put(depositConfiguration.getAddress(), value);

            ElaDidService elaDidService = new ElaDidService();
            ReturnMsgEntity ret = elaDidService.transferEla(chain.getChainUrlPrefix(),
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
