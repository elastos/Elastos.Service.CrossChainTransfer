package org.elastos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InitService implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(InitService.class);

    @Autowired
    private ChainService chainService;

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private InputWalletService inputWalletService;

    @Autowired
    private OutputWalletsService outputWalletsService;

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("------------In PlatformInitialization----------------");
        //Warning: chainService should be initial first.
        //Warning: change the sequence of these functions must be carefully.
        chainService.initService();
        outputWalletsService.initService();
        inputWalletService.initService();
        exchangeService.initService();
        logger.info("------------Out PlatformInitialization----------------");
    }
}
