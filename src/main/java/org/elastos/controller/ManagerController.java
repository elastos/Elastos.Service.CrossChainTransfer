/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.controller;

import org.elastos.annotation.Auth;
import org.elastos.service.ExchangeService;
import org.elastos.service.ExchangeWalletsService;
import org.elastos.service.WalletBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/1/ela_exchange/wallets")
public class ManagerController {
    private static Logger logger = LoggerFactory.getLogger(ManagerController.class);

    @Autowired
    private ExchangeService exchangeService;

    @GetMapping(value = "txdetail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Auth
    public String getTxDetail(@PageableDefault(sort = {"createTime", "id"}, direction = Sort.Direction.DESC)
                                      Pageable pageable, @RequestAttribute Long uid) {
        return exchangeService.getTxDetail(uid, pageable);
    }

    @GetMapping(value = "rest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Auth
    public String getAccountRest(@RequestAttribute Long uid) {
        return exchangeService.getAccountRest(uid);
    }

    @GetMapping(value = "deposit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Auth
    public String getDeposit(@RequestAttribute Long uid) {
        return exchangeService.getDepositAddress(uid);
    }

    @PostMapping(value = "retrans", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Auth
    public String reTrans(@RequestAttribute Long uid) {
        return exchangeService.reTransFailedExchange();
    }

}
