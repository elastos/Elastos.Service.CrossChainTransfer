package org.elastos.service.balance;

import org.elastos.constants.ServerResponseCode;
import org.elastos.service.ExchangeTask;
import org.elastos.service.InputWalletService;
import org.elastos.service.OutputWalletsService;
import org.elastos.util.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class BalanceScheduledTask {
    private Logger logger = LoggerFactory.getLogger(BalanceScheduledTask.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private boolean onFlag = true;

    @Autowired
    DepositMainTask depositMainTask;

    @Autowired
    DepositElaTask depositElaTask;

    @Autowired
    DepositDidTask depositDidTask;

    @Autowired
    DepositEthTask depositEthTask;

    @Autowired
    GatherInputTask gatherInputTask;

    @Autowired
    ExchangeTask exchangeTask;

    @Autowired
    InputWalletService inputWalletService;

    @Autowired
    OutputWalletsService outputWalletsService;

    public void setOnFlag(boolean onFlag) {
        this.onFlag = onFlag;
    }

    public boolean isOnFlag() {
        return onFlag;
    }

    @Scheduled(fixedDelay = 3 * 60 * 1000)
    public void elaDepositTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("elaDepositTask begin at:" + dateFormat.format(new Date()));
        depositElaTask.renewalOutput();
        if (depositElaTask.isGatherFlag()) {
            depositElaTask.gatherToMainDeposit();
            depositElaTask.setGatherFlag(false);
        }
        logger.debug("elaDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(fixedDelay = 3 * 60 * 1000)
    public void didDepositTask() {
        if ( !onFlag) {
            return;
        }
        logger.debug("didDepositTask begin at:" + dateFormat.format(new Date()));
        depositDidTask.renewalOutput();
        if (depositDidTask.isGatherFlag()) {
            depositDidTask.gatherToMainDeposit();
            depositDidTask.setGatherFlag(false);
        }
        logger.debug("didDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(fixedDelay = 60 * 1000)
    public void ethDepositTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("ethDepositTask begin at:" + dateFormat.format(new Date()));
        depositEthTask.renewalRestOutput();
        if (depositEthTask.isGatherFlag()) {
            depositEthTask.gatherToMainDeposit();
            depositEthTask.setGatherFlag(false);
        }
        logger.debug("ethDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(initialDelay = 3 * 60 * 1000, fixedDelay = 30 * 60 * 1000)
    public void mainDepositTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("mainDepositTask begin at:" + dateFormat.format(new Date()));
        depositMainTask.renewalRestDeposit();
        logger.debug("mainDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void gatherDepositTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("gatherDepositTask begin at:" + dateFormat.format(new Date()));
        depositMainTask.gatherDepositTask();
        logger.debug("gatherDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 60 * 1000)
    public void getGatherInputData() {
        if (!onFlag) {
            return;
        }
        logger.debug("getGatherInputData begin at:" + dateFormat.format(new Date()));
        gatherInputTask.getGatherInputData();
        logger.debug("getGatherInputData finish at:" + dateFormat.format(new Date()));
    }

    public String adaptScheduledTask(boolean exchangeFlag, boolean balanceFlag) {
        exchangeTask.setOnFlag(exchangeFlag);
        this.setOnFlag(balanceFlag);
        return new ServerResponse().setState(ServerResponseCode.SUCCESS).toJsonString();
    }

//    public String gatherAllEla() {
//        Double value = 0.0;
//        value += inputWalletService.gatherAllRenewalWallet();
//        value += outputWalletsService.gatherAllExchangeWallet();
//        value += this.gatherAllDeposit();
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("value", value);
//        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
//    }

}

