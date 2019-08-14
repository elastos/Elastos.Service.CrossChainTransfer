package org.elastos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StartService implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(StartService.class);

    @Autowired
    private ChainService chainService;

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private RenewalWalletService renewalWalletService;

    @Autowired
    private ExchangeWalletsService exchangeWalletsService;

    @Autowired
    private WalletBalanceService walletBalanceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("------------In PlatformInitialization----------------");
        //Warning: change the sequence of these functions must be carefully.
        chainService.initService();
        walletBalanceService.initService();
        exchangeWalletsService.initService();
        renewalWalletService.initService();
        exchangeService.initService();
        logger.info("------------Out PlatformInitialization----------------");
    }
}
