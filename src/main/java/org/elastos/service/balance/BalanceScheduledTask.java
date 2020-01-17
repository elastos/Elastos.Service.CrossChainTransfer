package org.elastos.service.balance;

import org.elastos.constants.ServerResponseCode;
import org.elastos.service.*;
import org.elastos.util.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class BalanceScheduledTask {
    private Logger logger = LoggerFactory.getLogger(BalanceScheduledTask.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private boolean onFlag = true;

    @Autowired
    DepositMainProc depositMainProc;

    @Autowired
    DepositElaProc depositElaProc;

    @Autowired
    DepositDidProc depositDidProc;

    @Autowired
    DepositEthProc depositEthProc;

    @Autowired
    GatherInputTask gatherInputTask;

    @Autowired
    ExchangeTask exchangeTask;

    @Autowired
    InputWalletService inputWalletService;

    @Autowired
    OutputWalletsService outputWalletsService;

    @Autowired
    DepositWalletsService depositWalletsService;

    @Autowired
    ExchangeService exchangeService;

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
        depositElaProc.renewalOutput();
        if (depositElaProc.isGatherFlag()) {
            depositElaProc.gatherToMainDeposit();
            depositElaProc.setGatherFlag(false);
        }
        logger.debug("elaDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(fixedDelay = 3 * 60 * 1000)
    public void didDepositTask() {
        if ( !onFlag) {
            return;
        }
        logger.debug("didDepositTask begin at:" + dateFormat.format(new Date()));
        depositDidProc.renewalOutput();
        if (depositDidProc.isGatherFlag()) {
            depositDidProc.gatherToMainDeposit();
            depositDidProc.setGatherFlag(false);
        }
        logger.debug("didDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(fixedDelay = 60 * 1000)
    public void ethDepositTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("ethDepositTask begin at:" + dateFormat.format(new Date()));
        depositEthProc.renewalRestOutput();
        if (depositEthProc.isGatherFlag()) {
            depositEthProc.gatherToMainDeposit();
            depositEthProc.setGatherFlag(false);
        }
        logger.debug("ethDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(initialDelay = 3 * 60 * 1000, fixedDelay = 30 * 60 * 1000)
    public void mainDepositTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("mainDepositTask begin at:" + dateFormat.format(new Date()));
        depositMainProc.renewalRestDeposit();
        logger.debug("mainDepositTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void gatherDepositTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("gatherDepositTask begin at:" + dateFormat.format(new Date()));
        depositMainProc.gatherDepositTask();
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

    public String adaptScheduledTask(boolean exchangeServiceFlag, boolean exchangeFlag,
                                     boolean balanceFlag, boolean gahterFlag) {
        exchangeService.setOnFlag(exchangeServiceFlag);
        exchangeTask.setOnFlag(exchangeFlag);
        this.setOnFlag(balanceFlag);
        gatherInputTask.setOnFlag(gahterFlag);
        return new ServerResponse().setState(ServerResponseCode.SUCCESS).toJsonString();
    }

    public String gatherAllEla() {
        adaptScheduledTask(false, false, false, false);

        gatherInputTask.getGatherInputData();
        outputWalletsService.gatherAllOutputWallet();

        try {
            TimeUnit.MINUTES.sleep(5);
        } catch (InterruptedException e) {
        }

        depositWalletsService.gatherAllToMainDeposit();

        return new ServerResponse().setState(ServerResponseCode.SUCCESS).toJsonString();
    }

}

