/**
 * Copyright (c) 2017-2019 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elastos.annotation.Auth;
import org.elastos.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/1/ela_exchange/admin")
public class LogInController {
    private static Logger logger = LoggerFactory.getLogger(LogInController.class);

    @Autowired
    private LoginService loginService;

    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody
    public String userLogin(HttpServletRequest request, @RequestAttribute String reqBody) {
        JSONObject map = JSON.parseObject(reqBody);
        String email = map.getString("mail");
        String password = map.getString("password");
        return loginService.userLogin(request.getSession(), email, password);
    }

    @RequestMapping(value = "logout", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Auth
    public String userLogout(HttpServletRequest request, @RequestAttribute Long uid) {
        return loginService.userLogout(request.getSession(), uid);
    }


    @RequestMapping(value = "echo", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String echo(@RequestAttribute String reqBody) {
        logger.info("chain agent echo data:" + reqBody);
        return reqBody;
    }
}
