/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elastos.service.ExchangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/1/ela_exchange")
public class ExchangeController {
    private static Logger logger = LoggerFactory.getLogger(ExchangeController.class);

    @Autowired
    private ExchangeService exchangeService;

    @GetMapping(value = "chainlist", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getChainList() {
        return exchangeService.getExchangeChainList();
    }

    @GetMapping(value = "rateinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getRate(@RequestParam(required = false, name = "src") Long src,
                          @RequestParam(required = false, name = "dst") Long dst) {
        return exchangeService.getExchangeRate(src, dst);
    }

    @PostMapping(value = "generator", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String generate(@RequestAttribute String reqBody) {
        JSONObject map = JSON.parseObject(reqBody);
        Long srcChainId = map.getLong("src_chain_id");
        Long dstChainId = map.getLong("dst_chain_id");
        String dstChainAddr = map.getString("dst_addr");
        String backChainAddr = map.getString("back_addr");
        String userDid = map.getString("user_did");

        return exchangeService.startNewExchange(srcChainId, dstChainId, dstChainAddr, backChainAddr, userDid);
    }

    @GetMapping(value = "{did}/tx/{exchange_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getExchange(@PathVariable("did") String did, @PathVariable("exchange_id") Long exchangeId) {
        return exchangeService.getExchangeInfo(did, exchangeId);
    }

    @GetMapping(value = "{did}/txs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getExchanges(@PageableDefault(sort = {"createTime", "id"}, direction = Sort.Direction.DESC)
                                           Pageable pageable, @PathVariable("did") String did) {
        return exchangeService.getExchangeList(did, pageable);
    }

}
