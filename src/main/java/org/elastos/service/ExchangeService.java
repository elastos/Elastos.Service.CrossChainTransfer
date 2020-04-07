package org.elastos.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jnr.ffi.annotations.Synchronized;
import org.apache.commons.lang3.StringUtils;
import org.elastos.POJO.ElaChainType;
import org.elastos.conf.MainDepositConfiguration;
import org.elastos.conf.NodeConfiguration;
import org.elastos.conf.TxBasicConfiguration;
import org.elastos.constant.RetCode;
import org.elastos.constants.ServerResponseCode;
import org.elastos.constants.ExchangeState;
import org.elastos.dao.ExchangeRateRepository;
import org.elastos.dao.ExchangeRecordRepository;
import org.elastos.dao.GatherRecordRepository;
import org.elastos.dto.ExchangeRecord;
import org.elastos.dto.ExchangeRate;
import org.elastos.dto.GatherRecord;
import org.elastos.pojo.Chain;
import org.elastos.pojo.DepositAddress;
import org.elastos.pojo.ElaWalletAddress;
import org.elastos.service.balance.*;
import org.elastos.util.HttpUtil;
import org.elastos.util.RetResult;
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
    MainDepositConfiguration mainDepositConfiguration;

    @Autowired
    NodeConfiguration nodeConfiguration;

    @Autowired
    ExchangeRateRepository exchangeRateRepository;

    @Autowired
    ExchangeRecordRepository exchangeRecordRepository;

    @Autowired
    GatherRecordRepository gatherRecordRepository;

    @Autowired
    ChainService chainService;

    @Autowired
    InputWalletService inputWalletService;

    @Autowired
    OutputWalletsService outputWalletsService;

    @Autowired
    BalanceScheduledTask balanceScheduledTask;

    @Autowired
    DepositMainProc depositMainProc;

    @Autowired
    DepositElaProc depositElaProc;

    @Autowired
    DepositDidProc depositDidProc;

    @Autowired
    DepositEthProc depositEthProc;

    @Autowired
    DepositWalletsService depositWalletsService;

    private List<ExchangeRate> exchangeRates = new ArrayList<>();
    private SynPairSet<ExchangeRecord> runningTxSet = new SynPairSet<>();
    private boolean onFlag = true;

    public void setOnFlag(boolean onFlag) {
        this.onFlag = onFlag;
    }

    public boolean isOnFlag() {
        return onFlag;
    }

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
        runningTxSet.saveAll(new HashSet<>(exchangeRecordList));
    }

    public String reTransTxById(Long txId) {
        Optional<ExchangeRecord> exchangeRecordOptional = exchangeRecordRepository.findById(txId);
        if (exchangeRecordOptional.isPresent()) {
            ExchangeRecord record = exchangeRecordOptional.get();
            record.setState(ExchangeState.EX_STATE_RENEWAL_WAITING);
            runningTxSet.save(record);
            return new ServerResponse().setState(ServerResponseCode.SUCCESS).toJsonString();
        } else {
            return new ServerResponse().setState(ServerResponseCode.ERROR).setMsg("There is no tx id:" + txId).toJsonString();
        }
    }

    public String dealFailedExchange() {
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
        return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(data).toJsonString();
    }

    public String getExchangeChainList() {
        List<Map<String, Object>> list = chainService.getChainList();
        if (list.isEmpty()) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_DATA_NOT_FOUND).setMsg("Service is not ready.").toJsonString();
        }

        return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(list).toJsonString();
    }

    public String getExchangeRate(Long srcChainId, Long dstChainId) {
        if ((null == srcChainId)
                || (null == dstChainId)) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }

        ExchangeRate rate = findExchangeRate(srcChainId, dstChainId);
        if (rate != null) {
            return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(rate).toJsonString();
        } else {
            return new ServerResponse().setState(ServerResponseCode.ERROR_DATA_NOT_FOUND).setMsg("Not support.").toJsonString();
        }
    }

    private ExchangeRate findExchangeRate(long srcChainId, long dstChainId) {
        for (ExchangeRate rate : exchangeRates) {
            if ((rate.getSrcChainId() == srcChainId) && (rate.getDstChainId() == dstChainId)) {
                Chain dstChain = chainService.getChain(dstChainId);
                rate.setThreshold_max(dstChain.getExchangeChain().getThreshold_max());
                rate.setThreshold_min(dstChain.getExchangeChain().getThreshold_min());
                return rate;
            }
        }
        logger.error("Err findExchangeRate has no rate for src:" + srcChainId + " and dst:" + dstChainId);
        return null;
    }

    public String retryTx(Long txId) {
        if (null == txId) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }
        Optional<ExchangeRecord> txOp = exchangeRecordRepository.findById(txId);
        if (!txOp.isPresent()) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_PARAMETER).setMsg("There is no txid:" + txId).toJsonString();
        }

        ExchangeRecord ex = txOp.get();
        String ret;
        if (ex.getState().equals(ExchangeState.EX_STATE_BACK_FAILED)) {
            ret = backing(ex);
            if (null == ret) {
                ret = "backing failed";
            }
        } else if (ex.getState().equals(ExchangeState.EX_STATE_TRANSFER_FAILED)) {
            ret = transferring(ex);
            if (null == ret) {
                ret = "transferring failed";
            }
        } else if (ex.getState().equals(ExchangeState.EX_STATE_DIRECT_TRANSFER_FAILED)) {
            ret = directTransferring(ex);
            if (null == ret) {
                ret = "directTransferring failed";
            }
        } else if (ex.getState().equals(ExchangeState.EX_STATE_RENEWAL_TIMEOUT)) {
            ex.setState(ExchangeState.EX_STATE_RENEWAL_WAITING);
            runningTxSet.save(ex);
            ret = "Retrying time out tx:" + ex.getId();
        } else {
            ret = "tx" + ex.getId() + " state is:" + ex.getState();
        }

        return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(ret).toJsonString();
    }

    public String startNewExchange(Long srcChainId, Long dstChainId, String dstAddr, String backAddr, String did) {
        if (!isOnFlag()) {
            return new ServerResponse().setState(ServerResponseCode.ERROR).setMsg("Service is off").toJsonString();
        }

        if ((null == srcChainId)
                || (null == dstChainId)
                || (StringUtils.isAnyBlank(dstAddr, did))) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }

        ExchangeRate rate = findExchangeRate(srcChainId, dstChainId);
        if (null == rate) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_PARAMETER).setMsg("Not support exchangeRecord chain").toJsonString();
        }

        if (!chainService.isChainOk(srcChainId, backAddr)) {
            String chainName = chainService.getChain(srcChainId).getExchangeChain().getChainName();
            return new ServerResponse().setState(ServerResponseCode.ERROR_INTERNAL).setMsg(chainName + " is down").toJsonString();
        }

        if (!chainService.isChainOk(dstChainId, dstAddr)) {
            String chainName = chainService.getChain(dstChainId).getExchangeChain().getChainName();
            return new ServerResponse().setState(ServerResponseCode.ERROR_INTERNAL).setMsg(chainName + " is down").toJsonString();
        }

        ExchangeRecord exchangeRecord = new ExchangeRecord();
        exchangeRecord.setDid(did);
        exchangeRecord.setSrcChainId(srcChainId);

        ElaWalletAddress srcAddr = inputWalletService.geneWalletAddress(srcChainId);
        if (null == srcAddr) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_INTERNAL).setMsg("geneWalletAddress failed").toJsonString();
        }


        exchangeRecord.setSrcWalletId(srcAddr.getWalletId());
        exchangeRecord.setSrcAddressId(srcAddr.getId());
        exchangeRecord.setSrcAddress(srcAddr.getCredentials().getAddress());
        exchangeRecord.setDstChainId(dstChainId);
        exchangeRecord.setDstAddress(dstAddr);
        exchangeRecord.setBackAddress(backAddr);
        exchangeRecord.setRate(rate.getRate());
        exchangeRecord.setFee_rate(rate.getFee_rate());
        exchangeRecord.setState(ExchangeState.EX_STATE_RENEWAL_WAITING);
        exchangeRecord = exchangeRecordRepository.save(exchangeRecord);

        runningTxSet.save(exchangeRecord);

        Map<String, Object> data = new HashMap<>();
        data.put("exchange_id", exchangeRecord.getId());
        data.put("src_chain_id", exchangeRecord.getSrcChainId());
        data.put("src_chain_name", chainService.getChain(exchangeRecord.getSrcChainId()).getExchangeChain().getChainName());
        data.put("src_chain_addr", exchangeRecord.getSrcAddress());
        return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(data).toJsonString();
    }


    public String getExchangeInfo(String did, Long exchangeId) {
        if (null == exchangeId) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }
        Optional<ExchangeRecord> op = exchangeRecordRepository.findById(exchangeId);
        if (op.isPresent()) {
            ExchangeRecord tx = op.get();
            Map<String, Object> data = fillTxData(tx);
            return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(data).toJsonString();
        } else {
            return new ServerResponse().setState(ServerResponseCode.ERROR_DATA_NOT_FOUND).setMsg("No such exchange id:" + exchangeId).toJsonString();
        }
    }

    private Map<String, Object> fillTxData(ExchangeRecord tx) {
        Map<String, Object> data = new HashMap();
        data.put("exchange_id", tx.getId());
        data.put("create_time", tx.getCreateTime());
        data.put("src_chain_id", tx.getSrcChainId());
        data.put("src_chain_name", chainService.getChain(tx.getSrcChainId()).getExchangeChain().getChainName());
        data.put("src_chain_addr", tx.getSrcAddress());
        data.put("src_value", tx.getSrcValue());
        data.put("dst_chain_id", tx.getDstChainId());
        data.put("dst_chain_name", chainService.getChain(tx.getDstChainId()).getExchangeChain().getChainName());
        data.put("dst_chain_addr", tx.getDstAddress());
        data.put("dst_value", tx.getDstValue());
        data.put("state", tx.getState());
        String state = tx.getState();
        if (state.equals(ExchangeState.EX_STATE_TRANSFER_FINISH) || state.equals(ExchangeState.EX_STATE_DIRECT_TRANSFER_FINISH)) {
            data.put("txid", tx.getDstTxid());
        } else if (state.equals(ExchangeState.EX_STATE_BACK_FINISH)) {
            data.put("txid", tx.getBackTxid());
        }
        return data;
    }

    public String getExchangeList(String did, Pageable pageable) {
        if (StringUtils.isBlank(did)) {
            return new ServerResponse().setState(ServerResponseCode.ERROR_PARAMETER).setMsg("Null parameter").toJsonString();
        }

        List<ExchangeRecord> exchangeRecords = exchangeRecordRepository.findAllByDid(did, pageable);
        List<Map<String, Object>> data = new ArrayList<>();
        for (ExchangeRecord tx : exchangeRecords) {
            Map<String, Object> d = fillTxData(tx);
            data.add(d);
        }
        return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(data).toJsonString();
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

    public  static RetResult<Double> getPreValueOfAddress(String address) {
        String url = "https://node1.elaphant.app/api/1/history/";

        String result = HttpUtil.get(url + address, null);
        if (null == result) {
            return RetResult.retErr(RetCode.RESPONSE_ERROR, "no response");
        }
        JSONObject obj = JSON.parseObject(result);
        if (200 == obj.getInteger("status")) {
            JSONObject map = obj.getObject("result", JSONObject.class);
            Integer num = map.getInteger("TotalNum");
            if (1 != num) {
                return RetResult.retErr(RetCode.RESPONSE_ERROR, "more than 1 tx");
            } else {
                JSONArray array = map.getJSONArray("History");
                JSONObject data = array.getJSONObject(0);
                String type = data.getString("Type");
                String outAddress = data.getJSONArray("Outputs").getString(0);
                if (!outAddress.equals(address) || !("income".equals(type))) {
                    return RetResult.retErr(RetCode.RESPONSE_ERROR, "not output address");
                }
                Integer value = data.getInteger("Value");
                Integer fee = data.getInteger("Fee");
                Integer rest = value - fee;
                return RetResult.retOk(rest/100000000.0);
            }
        } else {
            return RetResult.retErr(RetCode.RESPONSE_ERROR, obj.getString("result"));
        }
    }

    private void waitRenewal(ExchangeRecord tx) {
        Chain srcChain = chainService.getChain(tx.getSrcChainId());
        if (null == srcChain) {
            logger.error("The tx chain id not match. src chain id:" + tx.getSrcChainId());
            return;
        }

        RetResult<Double> valueRet = srcChain.getElaTransferService().getBalance(tx.getSrcAddress());
        if (valueRet.getCode() != RetCode.SUCC) {
            runningTxSet.save(tx);
            return;
        }

        Chain dstChain = chainService.getChain(tx.getDstChainId());
        if (null == dstChain) {
            logger.error("The tx chain id not match. dst chain id:" + tx.getDstChainId());
            return;
        }

        Double min = dstChain.getExchangeChain().getThreshold_min();
        Double max = dstChain.getExchangeChain().getThreshold_max();

        Double value = valueRet.getData();
        if (value == 0.0) {
            RetResult<Double> vRet = ExchangeService.getPreValueOfAddress(tx.getSrcAddress());
            if (vRet.getCode() == RetCode.SUCC) {
                value = vRet.getData();
                //If value is bigger than max, it will direct transfer, we do not speed up it.
                if (value > max) {
                    value = 0.0;
                }
            }
        }
        tx.setSrcValue(value);

        if (value > 0.0) {
            ExchangeRate rate = findExchangeRate(tx.getSrcChainId(), tx.getDstChainId());
            if (null == rate) {
                logger.error("Err waitRenewal find ExchangeRate has no rate. src chain id:"
                        + tx.getSrcChainId() + " dst chain id:" + tx.getDstChainId());
                return;
            }
            Double minFee = rate.getService_min_fee();

            //value must in threshold range
            if (value < min) {
                logger.warn("waitRenewal less than threshold address:" + tx.getSrcAddress() + ". value:" + value);
                //Wait for time out or user renewal more
                runningTxSet.save(tx);
            } else {
                //Warning: fee must lager than all same chain transfer fee.
                Double fee = value * tx.getFee_rate();
                if (fee < minFee) {
                    fee = minFee;
                }
                tx.setFee(fee);
                if ((!balanceScheduledTask.isOnFlag())
                        || (value > max)) {
                    ElaChainType srcChainType = srcChain.getExchangeChain().getType();
                    //Only ela chain can direct transfer
                    if (srcChainType == ElaChainType.ELA_CHAIN) {
                        tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFERRING);
                    } else {
                        tx.setState(ExchangeState.EX_STATE_BACKING);
                    }
                } else {
                    tx.setState(ExchangeState.EX_STATE_TRANSFERRING);
                }
            }
            //Need next proc, we set it back to runningTxSet.
            exchangeRecordRepository.save(tx);
            runningTxSet.save(tx);
        } else if (isTransactionTimeout(tx)) {
            tx.setState(ExchangeState.EX_STATE_RENEWAL_TIMEOUT);
            exchangeRecordRepository.save(tx);
        } else {
            //Still waiting
            runningTxSet.save(tx);
        }
    }

    private String exchange(long dstChainId, String dstAddr, Double value) {
        ElaWalletAddress walletAddress = outputWalletsService.getAddressForExchange(dstChainId, value);
        if (null == walletAddress) {
            logger.info("exchange getAddressForExchange not valid address chain id:" + dstChainId + "dst address:" + dstAddr);
            return null;
        }

        Chain chain = chainService.getChain(dstChainId);
        if (null == chain) {
            logger.error("exchange There is no dst chain id:" + dstChainId);
            return null;
        }

        ElaTransferService transferService = chain.getElaTransferService();

        RetResult<String> txidRet = transferService.transfer(walletAddress.getCredentials(), dstAddr, value);
        if (txidRet.getCode() == RetCode.SUCC) {
            outputWalletsService.usingAddress(walletAddress);

            if ((walletAddress.getValue() - value) < txBasicConfiguration.getOUTPUT_ADDRESS_SUPPLY_THRESHOLD()) {
                depositWalletsService.saveForRenewal(walletAddress);
            }
            return txidRet.getData();
        } else {
            logger.error("exchange tx failed dst chainId:" + dstChainId + " dstAddr:" + dstAddr + " result:" + txidRet.getMsg());
            return null;
        }
    }

    private String transferring(ExchangeRecord tx) {
        String ret = null;
        Double srcValue = tx.getSrcValue();
        Double value = srcValue - tx.getFee();
        value *= tx.getRate();
        tx.setSrcValue(srcValue);
        tx.setDstValue(value);
        String txid = exchange(tx.getDstChainId(), tx.getDstAddress(), value);
        if (null != txid) {
            ret = txid;
            tx.setDstTxid(txid);
            tx.setState(ExchangeState.EX_STATE_TRANSFER_FINISH);
            exchangeRecordRepository.save(tx);
            save2GatherList(tx);
        } else {
            Chain chain = chainService.getChain(tx.getSrcChainId());
            ElaChainType srcChainType = chain.getExchangeChain().getType();
            if (ElaChainType.ELA_CHAIN.equals(srcChainType)) {
                tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFERRING);
            } else {
                tx.setState(ExchangeState.EX_STATE_BACKING);
            }
            exchangeRecordRepository.save(tx);
            runningTxSet.save(tx);
        }
        return ret;
    }

    private String backing(ExchangeRecord tx) {
        String ret = null;
        String txid = inputWalletService.backInput(tx);
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
        ExchangeRecord ret = inputWalletService.directTransfer(tx);
        if ((null != ret) && (null != ret.getDstTxid())) {
            //Gather service fee
            tx.setDstTxid(ret.getDstTxid());
            tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFERRING_WAIT_GATHER);
        } else {
            tx.setState(ExchangeState.EX_STATE_BACKING);
        }
        exchangeRecordRepository.save(tx);
        runningTxSet.save(tx);
        return tx.getDstTxid();
    }

    private void directTransferringWaitGather(ExchangeRecord tx) {
        String receipt = chainService.getTransactionReceipt(tx.getSrcChainId(), tx.getDstTxid());
        if (null != receipt) {
            tx.setState(ExchangeState.EX_STATE_DIRECT_TRANSFER_FINISH);
            exchangeRecordRepository.save(tx);
            save2GatherList(tx);
        } else {
            runningTxSet.save(tx);
        }
    }

    private void save2GatherList(ExchangeRecord tx) {
        //Add to gather address
        ElaWalletAddress addr = inputWalletService.findAddress(tx.getSrcChainId(), tx.getSrcWalletId(), tx.getSrcAddressId());
        if (null == addr) {
            logger.error("save2GatherList failed findAddress tx id:" + tx.getId());
        } else {
            GatherRecord gatherRecord = new GatherRecord();
            gatherRecord.ElaAddressToGather(addr);
            gatherRecord.setChainId(tx.getSrcChainId());
            gatherRecordRepository.save(gatherRecord);
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
        return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(data).toJsonString();
    }

    //用户获取充值钱包地址。
    public String getDepositAddress(Long uid) {
        String address = mainDepositConfiguration.getAddress();
        Map<String, Object> data = new HashMap<>();
        data.put("address", address);
        return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(data).toJsonString();
    }

    public String getAccountRest(Long uid) {
        List<Map<String, Object>> listExchange = outputWalletsService.getWalletRest();
        Map<String, Object> data = new HashMap<>();
        data.put("exchange_wallets", listExchange);

        List<Map<String, Object>> depositList = new ArrayList<>();
        Collection<DepositAddress> depositAddresses = depositWalletsService.getAllDepositAddress();

        for (DepositAddress address : depositAddresses) {
            Chain chain = chainService.getChain(address.getChainType());
            Map<String, Object> da = new HashMap<>();
            da.put("chain_id", chain.getExchangeChain().getId());
            da.put("chain_name", chain.getExchangeChain().getChainName());
            RetResult<Double> retValue = chain.getElaTransferService().getBalance(address.getCredentials().getAddress());
            if (retValue.getCode() == RetCode.SUCC) {
                da.put("value", retValue.getData());
            } else {
                logger.error("getAccountRest getBalance failed. chain name:" + chain.getExchangeChain().getChainName());
            }
            depositList.add(da);
        }

        Map<String, Object> da = new HashMap<>();
        da.put("name", "Main deposit in ela main chain");
        Chain chain = chainService.getChain(ElaChainType.ELA_CHAIN);
        RetResult<Double> retValue = chain.getElaTransferService().getBalance(mainDepositConfiguration.getAddress());
        if (retValue.getCode() == RetCode.SUCC) {
            da.put("value", retValue.getData());
        } else {
            logger.error("getAccountRest getBalance failed. ela main chain main deposit");
        }
        depositList.add(da);
        data.put("deposit_address", depositList);

        return new ServerResponse().setState(ServerResponseCode.SUCCESS).setData(data).toJsonString();
    }

}
