package org.elastos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class ScheduledTaskService {

    private Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
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

    public void setOnFlag(boolean onFlag) {
        this.onFlag = onFlag;
    }

    //start after 1min, every 10sec
    @Scheduled(initialDelay = 60*1000, fixedDelay = 10*1000)
    public void exchangeTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("exchangeTask begin at:"+ dateFormat.format(new Date()));
        exchangeService.checkRunningTxTask();
        logger.debug("exchangeTask finish at:"+ dateFormat.format(new Date()));
    }

    //start after 1min, every 2min
    @Scheduled(initialDelay = 60*1000, fixedDelay = 2*60*1000)
    public void exchangeWalletStateTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("exchangeWalletStateTask begin at:"+ dateFormat.format(new Date()));
        exchangeWalletsService.walletsCheckTask();
        logger.debug("exchangeWalletStateTask finish at:"+ dateFormat.format(new Date()));
    }
}
