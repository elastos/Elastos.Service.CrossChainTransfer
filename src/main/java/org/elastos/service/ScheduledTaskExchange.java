package org.elastos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class ScheduledTaskExchange {

    private Logger logger = LoggerFactory.getLogger(ScheduledTaskExchange.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private boolean onFlag = true;

    @Autowired
    ExchangeService exchangeService;

    @Autowired
    ExchangeWalletsService exchangeWalletsService;

    @Autowired
    RenewalWalletService renewalWalletService;

    public void setOnFlag(boolean onFlag) {
        this.onFlag = onFlag;
    }

    public boolean isOnFlag() {
        return onFlag;
    }

    //start after 1min, every 10sec
    @Scheduled(fixedDelay = 10*1000)
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
        //todo we check renewal save in db value here
    }
}
