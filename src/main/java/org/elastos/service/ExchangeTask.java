package org.elastos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class ExchangeTask {

    private Logger logger = LoggerFactory.getLogger(ExchangeTask.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private boolean onFlag = true;

    @Autowired
    ExchangeService exchangeService;

    @Autowired
    OutputWalletsService outputWalletsService;

    @Autowired
    InputWalletService inputWalletService;

    public void setOnFlag(boolean onFlag) {
        this.onFlag = onFlag;
    }

    public boolean isOnFlag() {
        return onFlag;
    }

    @Scheduled(fixedDelay = 10*1000)
    public void exchangeTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("exchangeTask begin at:"+ dateFormat.format(new Date()));
        exchangeService.checkRunningTxTask();
        logger.debug("exchangeTask finish at:"+ dateFormat.format(new Date()));
    }

    @Scheduled(initialDelay = 30*1000, fixedDelay = 30*1000)
    public void outputCheckTask() {
        if (!onFlag) {
            return;
        }
        logger.debug("outputCheckTask begin at:"+ dateFormat.format(new Date()));
        outputWalletsService.checkAddressFree();
        logger.debug("outputCheckTask finish at:"+ dateFormat.format(new Date()));
    }
}
