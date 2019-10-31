package org.elastos.service;


import jnr.ffi.annotations.Synchronized;
import org.apache.commons.lang3.StringUtils;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.DepositConfiguration;
import org.elastos.conf.NodeConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constants.RetCode;
import org.elastos.constants.ExchangeState;
import org.elastos.dao.ExchangeRateRepository;
import org.elastos.dao.ExchangeRecordRepository;
import org.elastos.dao.GatherAddressRepository;
import org.elastos.dto.ExchangeChain;
import org.elastos.dto.ExchangeRecord;
import org.elastos.dto.ExchangeRate;
import org.elastos.dto.GatherAddress;
import org.elastos.entity.ReturnMsgEntity;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.util.ServerResponse;
import org.elastos.util.SynPairSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExchangeService {
    private static Logger logger = LoggerFactory.getLogger(ExchangeService.class);

    @Autowired
    TxBasicConfiguration txBasicConfiguration;

    @Autowired
    DepositConfiguration depositConfiguration;

    @Autowired
    NodeConfiguration nodeConfiguration;

    @Autowired
    ExchangeRateRepository exchangeRateRepository;

    @Autowired
    ExchangeRecordRepository exchangeRecordRepository;

    @Autowired
    GatherAddressRepository gatherAddressRepository;

    @Autowired
    ChainService chainService;

    @Autowired
    RenewalWalletService renewalWalletService;

    @Autowired
    ExchangeWalletsService exchangeWalletsService;

    @Autowired
    WalletBalanceService walletBalanceService;

    @Autowired
    ScheduledTaskBalance scheduledTaskBalance;

    private List<ExchangeRate> exchangeRates = new ArrayList<>();
    private SynPairSet<ExchangeRecord> runningTxSet = new SynPairSet<>();

    void initService() {
        //交易汇率填充
        exchangeRates.clear();
        List<ExchangeRate> exchangeRateList = (List<ExchangeRate>) exchangeRateRepository.findAll();
        exchangeRates.addAll(exchangeRateList);

        //交易信息恢复
        List<String> states = new ArrayList<>();
        states.add(ExchangeState.EX_STATE_RENEWAL_WAITING);
        states.add(ExchangeState.EX_STATE_TRANSFERRING);
        states.add(ExchangeState.EX_STATE_BACKING);
        states.add(ExchangeState.EX_STATE_DIRECT_TRANSFERRING);
        states.add(ExchangeState.EX_STATE_DIRECT_TRANSFERRING_WAIT_GATHER);
        runningTxSet.init();
        List<ExchangeRecord> exchangeRecordList = exchangeRecordRepository.findAllByStateIn(states);
        runningTxSet.saveAll2Set(new HashSet<>(exchangeRecordList));
    }

    public String reTransFailedExchange() {
        List<String> states = new ArrayList<>();
        states.add(ExchangeState.EX_STATE_BACK_FAILED);
        states.add(ExchangeState.EX_STATE_DIRECT_TRANSFER_FAILED);
        states.add(ExchangeState.EX_STATE_TRANSFER_FAILED);
        List<ExchangeRecord> exchangeRecordList = exchangeRecordRepository.findAllByStateIn(states);
        List<ExchangeRecord> successList = new ArrayList<>();
        String ret;
        for (ExchangeRecord ex : exchangeRecordList) {
            if (ex.getState().equals(ExchangeState.EX_STATE_BACK_FAILED)) {
                ret = backing(ex);
                if (null != ret) {
                    successList.add(ex);
                }
            }

            if (ex.getState().equals(ExchangeState.EX_STATE_TRANSFER_FAILED)) {
                ret = transferring(ex);
                if (null != ret) {
                    successList.add(ex);
                }
            }

            if (ex.getState().equals(ExchangeState.EX_STATE_DIRECT_TRANSFER_FAILED)) {
                ret = directTransferring(ex);
                if (null != ret) {
                    successList.add(ex);
                }
            }
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (ExchangeRecord tx : successList) {
            Map<String, Object> d = fillTxData(tx);
            data.add(d);
        }
        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

    public String getExchangeChainList() {
        List<Map<String, Object>> list = chainService.getChainList();
        if (list.isEmpty()) {
            return new ServerResponse().setState(RetCode.ERROR_DATA_NOT_FOUND).setMsg("Service is not ready.").toJsonString();
        }

        return new ServerResponse().setState(RetCode.SUCCESS).setData(list).toJsonString();
    }

    public String getExchangeRate(Long srcChainId, Long dstChainId) {
        if ((null == srcChainId)
                || (null == dstChainId)) {
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }

        ExchangeRate rate = findExchangeRate(srcChainId, dstChainId);
        if (rate != null) {
            return new ServerResponse().setState(RetCode.SUCCESS).setData(rate).toJsonString();
        } else {
            return new ServerResponse().setState(RetCode.ERROR_DATA_NOT_FOUND).setMsg("Not support.").toJsonString();

        }
    }

    private ExchangeRate findExchangeRate(long srcChainId, long dstChainId) {
        for (ExchangeRate rate : exchangeRates) {
            if ((rate.getSrcChainId() == srcChainId) && (rate.getDstChainId() == dstChainId)) {
                return rate;
            }
        }
        logger.error("Err findExchangeRate has no rate for src:" + srcChainId + " and dst:" + dstChainId);
        return null;
    }

    public String startNewExchange(Long srcChainId, Long dstChainId, String dstAddr, String backAddr, String did) {
        if ((null == srcChainId)
                || (null == dstChainId)
                || (StringUtils.isAnyBlank(dstAddr, did))) {
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }

        ExchangeRate rate = findExchangeRate(srcChainId, dstChainId);
        if (null == rate) {
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("Not support exchangeRecord chain").toJsonString();
        }

        ExchangeRecord exchangeRecord = new ExchangeRecord();
        exchangeRecord.setDid(did);
        exchangeRecord.setSrcChainId(srcChainId);
        ExchangeChain srcChain = chainService.getChain(srcChainId);
        if (null == srcChain) {
            return new ServerResponse().setState(RetCode.ERROR_INTERNAL).setMsg("not support chain id:" + srcChainId).toJsonString();
        }

        ElaWalletAddress srcAddr = renewalWalletService.geneWalletAddress(srcChain);
        if (null == srcAddr) {
            return new ServerResponse().setState(RetCode.ERROR_INTERNAL).setMsg("geneWalletAddress failed").toJsonString();
        }
        exchangeRecord.setSrcWalletId(srcAddr.getWalletId());
        exchangeRecord.setSrcAddressId(srcAddr.getId());
        exchangeRecord.setSrcAddress(srcAddr.getPublicAddress());
        exchangeRecord.setDstChainId(dstChainId);
        exchangeRecord.setDstAddress(dstAddr);
        exchangeRecord.setBackAddress(backAddr);
        exchangeRecord.setRate(rate.getRate());
        exchangeRecord.setFee_rate(rate.getFee_rate());
        exchangeRecord.setState(ExchangeState.EX_STATE_RENEWAL_WAITING);
        exchangeRecord = exchangeRecordRepository.save(exchangeRecord);

        runningTxSet.save2Set(exchangeRecord);

        Map<String, Object> data = new HashMap<>();
        data.put("exchange_id", exchangeRecord.getId());
        data.put("src_chain_id", exchangeRecord.getSrcChainId());
        data.put("src_chain_name", chainService.getChain(exchangeRecord.getSrcChainId()).getChainName());
        data.put("src_chain_addr", exchangeRecord.getSrcAddress());
        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }


    public String getExchangeInfo(String did, Long exchangeId) {
        if (null == exchangeId) {
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }
        Optional<ExchangeRecord> op = exchangeRecordRepository.findById(exchangeId);
        if (op.isPresent()) {
            ExchangeRecord tx = op.get();
            Map<String, Object> data = fillTxData(tx);
            return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
        } else {
            return new ServerResponse().setState(RetCode.ERROR_DATA_NOT_FOUND).setMsg("No such exchange id:" + exchangeId).toJsonString();
        }
    }

    private Map<String, Object> fillTxData(ExchangeRecord tx) {
        Map<String, Object> data = new HashMap();
        data.put("exchange_id", tx.getId());
        data.put("create_time", tx.getCreateTime());
        data.put("src_chain_id", tx.getSrcChainId());
        data.put("src_chain_name", chainService.getChain(tx.getSrcChainId()).getChainName());
        data.put("src_chain_addr", tx.getSrcAddress());
        data.put("src_value", tx.getSrcValue());
        data.put("dst_chain_id", tx.getDstChainId());
        data.put("dst_chain_name", chainService.getChain(tx.getDstChainId()).getChainName());
        data.put("dst_chain_addr", tx.getDstAddress());
        data.put("dst_value", tx.getDstValue());
        data.put("state", tx.getState());
        String state = tx.getState();
        if (state.equals(ExchangeState.EX_STATE_TRANSFER_FINISH)) {
            data.put("txid", tx.getDstTxid());
        } else if (state.equals(ExchangeState.EX_STATE_BACK_FINISH)) {
            data.put("txid", tx.getBackTxid());
        }
        return data;
    }

    public String getExchangeList(String did, Pageable pageable) {
        if (StringUtils.isBlank(did)) {
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }

        List<ExchangeRecord> exchangeRecords = exchangeRecordRepository.findAllByDid(did, pageable);
        List<Map<String, Object>> data = new ArrayList<>();
        for (ExchangeRecord tx : exchangeRecords) {
            Map<String, Object> d = fillTxData(tx);
            data.add(d);
        }
        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

    @Synchronized
    void checkRunningTxTask() {
        Set<ExchangeRecord> set = runningTxSet.useSet();
        logger.debug("checkRunningTxTask set size:" + set.size());
        for (ExchangeRecord record : set) {
            switch (record.getState()) {
                case ExchangeState.EX_STATE_RENEWAL_WAITING:
                    waitRenewal(record);
                    logger.debug("checkRunningTxTask record");
                    break;
                case ExchangeState.EX_STATE_TRANSFERRING:
                    transferring(record);
                    logger.debug("checkRunningTxTask transferring");
                    break;
                case ExchangeState.EX_STATE_BACKING:
                    backing(record);
                    logger.debug("checkRunningTxTask backing");
                    break;
                case ExchangeState.EX_STATE_DIRECT_TRANSFERRING:
                    directTransferring(record);
                    logger.debug("checkRunningTxTask direct");
                    break;
                case ExchangeState.EX_STATE_DIRECT_TRANSFERRING_WAIT_GATHER:
                    directTransferringWaitGather(record);
                    logger.debug("checkRunningTxTask direct wait");
                    break;
            }
        }
        set.clear();
    }

    private void waitRenewal(ExchangeRecord tx) {

        ExchangeChain srcChain = chainService.getChain(tx.getSrcChainId());
        if (null == srcChain) {
            return;
        }

        Double value = chainService.getBalancesByAddr(srcChain, tx.getSrcAddress());
        if (null == value) {
            runningTxSet.save2Set(tx);
            return;
        }

        tx.setSrcValue(value);

        if (value > 0.0) {
            ExchangeRate rate = findExchangeRate(tx.getSrcChainId(), tx.getDstChainId());
            if (null == rate) {
                logger.error("Err waitRenewal find ExchangeRate has no rate.");
                return;
            }
            Double min = rate.getThreshold_min();
            Double max = rate.getThreshold_max();
            Double minFee = rate.getService_min_fee();

            //value must in threshold range
            if (value < min) {
                logger.warn("waitRenewal less than threshold address:" + tx.getSrcAddress() + ". value:" + value);
                //Wait for time out or user renewal more
                runningTxSet.save2Set(tx);
            } else {
                Double fee = value * tx.getFee_rate();
                if (fee < minFee) {
                    fee = minFee;
                }
                tx.setFee(fee);
                if ((!scheduledTaskBalance.isOnFlag())
                        || (value > max)
                        || (srcChain.getType() == ElaChainType.ETH_CHAIN)) {
                    tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFERRING);
                } else {
                    tx.setState(ExchangeState.EX_STATE_TRANSFERRING);
                }
            }
            //Need next proc, we set it back to runningTxSet.
            exchangeRecordRepository.save(tx);
            runningTxSet.save2Set(tx);
        } else if (isTransactionTimeout(tx)) {
            tx.setState(ExchangeState.EX_STATE_RENEWAL_TIMEOUT);
            exchangeRecordRepository.save(tx);
        } else {
            //Still waiting
            runningTxSet.save2Set(tx);
        }
    }

    private String exchangeEla(long dstChainId, String dstAddr, Double value) {
        ElaWalletAddress walletAddress = exchangeWalletsService.getExchangeAddress(dstChainId, value);
        if (null == walletAddress) {
            logger.error("exchangeEla getExchangeAddress failed chain id:" + dstChainId + "dst address:" + dstAddr);
            return null;
        }

        ExchangeChain chain = chainService.getExchangeChain(dstChainId);
        if (null == chain) {
            logger.error("exchangeEla There is no dst chain id:" + dstChainId);
            return null;
        }

        ElaDidService elaDidService = new ElaDidService(chain.getChainUrlPrefix(), nodeConfiguration.getTestNet());
        List<String> priKeyList = new ArrayList<>();
        priKeyList.add(walletAddress.getPrivateKey());
        Map<String, Double> dstMap = new HashMap<>();
        dstMap.put(dstAddr, value);
        ReturnMsgEntity ret = elaDidService.transferEla(
                chain.getType(), priKeyList,
                chain.getType(), dstMap);
        if (ret.getStatus() != RetCode.SUCCESS) {
            logger.error("exchangeEla tx failed dst chainId:" + dstChainId + " dstAddr:" + dstAddr + " result:" + ret.getResult());
            return null;
        } else {
            if ((walletAddress.getRest() - value) < txBasicConfiguration.getWORKER_ADDRESS_RENEWAL_MIN_THRESHOLD()) {
                walletBalanceService.save2ExchangeAddress(chain.getId(), walletAddress);
            }
            return (String) ret.getResult();
        }

    }

    private String transferring(ExchangeRecord tx) {
        String ret = null;
        Double srcValue = chainService.getBalancesByAddr(tx.getSrcChainId(), tx.getSrcAddress());
        Double value = srcValue - tx.getFee();
        tx.setSrcValue(srcValue);
        tx.setDstValue(value);
        String txid = exchangeEla(tx.getDstChainId(), tx.getDstAddress(), value);
        if (null == txid) {
            tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFERRING);
            exchangeRecordRepository.save(tx);
            runningTxSet.save2Set(tx);
        } else {
            ret = txid;
            tx.setDstTxid(txid);
            tx.setState(ExchangeState.EX_STATE_TRANSFER_FINISH);
            exchangeRecordRepository.save(tx);
            save2GatherList(tx);
        }
        return ret;
    }

    private String backing(ExchangeRecord tx) {
        String ret = null;
        String txid = renewalWalletService.backRenewalEla(tx);
        if (null == txid) {
            tx.setState(ExchangeState.EX_STATE_BACK_FAILED);
        } else {
            ret = txid;
            tx.setBackTxid(txid);
            tx.setState(ExchangeState.EX_STATE_BACK_FINISH);
        }
        exchangeRecordRepository.save(tx);
        return ret;
    }

    private String directTransferring(ExchangeRecord tx) {
        ExchangeRecord ret = renewalWalletService.directTransfer(tx);
        if (null == ret.getDstTxid()) {
            tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFER_FAILED);
            exchangeRecordRepository.save(tx);
        } else {
            //Gather service fee
            ExchangeChain srcChain = chainService.getChain(tx.getSrcChainId());
            if (srcChain.getType() == ElaChainType.ETH_CHAIN) {
                //todo change to the same proc like ela: 1.save db 2.gather
                tx = renewalWalletService.ethDirectTransferGather(tx);
                exchangeRecordRepository.save(tx);
            } else {
                tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFERRING_WAIT_GATHER);
                exchangeRecordRepository.save(tx);
                runningTxSet.save2Set(tx);
            }
        }
        return tx.getDstTxid();
    }

    private void directTransferringWaitGather(ExchangeRecord tx) {
        Object o = chainService.getTransaction(tx.getSrcChainId(), tx.getDstTxid());
        if (null != o) {
            tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFER_FINISH);
            exchangeRecordRepository.save(tx);
            save2GatherList(tx);
        } else {
            runningTxSet.save2Set(tx);
        }
    }

    private void save2GatherList(ExchangeRecord tx) {
        //Add to gather address
        ElaWalletAddress addr = renewalWalletService.findAddress(tx.getSrcWalletId(), tx.getSrcAddressId());
        if (null == addr) {
            logger.error("save2GatherList failed findAddress tx id:" + tx.getId());
        } else {
            GatherAddress gatherAddress = new GatherAddress();
            gatherAddress.ElaAddressToGather(addr);
            gatherAddress.setChainId(tx.getSrcChainId());
            gatherAddressRepository.save(gatherAddress);
        }
    }

    private boolean isTransactionTimeout(ExchangeRecord tx) {
        long now = new Date().getTime();
        long createTime = tx.getCreateTime().getTime();
        long time = now - createTime;
        if (time > txBasicConfiguration.getRENEWAL_TIMEOUT() * 60 * 60 * 1000) {
            return true;
        } else {
            return false;
        }
    }

    public String getTxDetail(Long uid, Pageable pageable) {
        Page<ExchangeRecord> exchangeRecords = exchangeRecordRepository.findAll(pageable);
        List<Map<String, Object>> data = new ArrayList<>();
        for (ExchangeRecord tx : exchangeRecords) {
            Map<String, Object> d = fillTxData(tx);
            data.add(d);
        }
        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

    //用户获取充值钱包地址。
    public String getDepositAddress(Long uid) {
        String address = depositConfiguration.getAddress();
        Map<String, Object> data = new HashMap<>();
        data.put("address", address);
        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

    public String getAccountRest(Long uid) {
        List<Map<String, Object>> listExchange = exchangeWalletsService.getWalletRest();
        Map<String, Object> data = new HashMap<>();
        data.put("exchange_wallets", listExchange);
        List<ExchangeChain> chains = chainService.getChains();
        List<Map<String, Object>> depositList = new ArrayList<>();
        for (ExchangeChain chain : chains) {
            Double depositValue = chainService.getBalancesByAddr(chain, depositConfiguration.getAddress());
            Map<String, Object> da = new HashMap<>();
            da.put("chain_id", chain.getId());
            da.put("value", depositValue);
            depositList.add(da);
        }

        data.put("deposit_address", depositList);
        //todo renewal save in db wallet rest

        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

}
