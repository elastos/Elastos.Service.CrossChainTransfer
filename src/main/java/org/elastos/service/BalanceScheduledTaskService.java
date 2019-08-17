package org.elastos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class BalanceScheduledTaskService {

    private Logger logger = LoggerFactory.getLogger(BalanceScheduledTaskService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private boolean onFlag = true;

    @Autowired
    ExchangeService exchangeService;

    @Autowired
    ExchangeWalletsService exchangeWalletsService;

    @Autowired
    RenewalWalletService renewalWalletService;

    @Autowired
    WalletBalanceService walletBalanceService;

    @Scheduled(initialDelay = 5 * 60 * 1000, fixedDelay = 60 * 60 * 1000)
    public void exchangeBalanceTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("exchangeBalanceTask begin at:" + dateFormat.format(new Date()));
        walletBalanceService.EveryDepositToExchangeTask();
        logger.debug("exchangeBalanceTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(initialDelay = 60 * 60 * 1000, fixedDelay = 2 * 60 * 60 * 1000)
    public void depositBalanceTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("depositBalanceTask begin at:" + dateFormat.format(new Date()));
        walletBalanceService.MainDepositToSideChainDepositTask();
        logger.debug("depositBalanceTask finish at:" + dateFormat.format(new Date()));
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void renewalWalletBalanceTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("renewalWalletBalanceTask begin at:" + dateFormat.format(new Date()));
        walletBalanceService.UserInputToMainDepositTask();
        logger.debug("renewalWalletBalanceTask finish at:" + dateFormat.format(new Date()));
    }

    public void setOnFlag(boolean onFlag) {
        this.onFlag = onFlag;
    }
}
