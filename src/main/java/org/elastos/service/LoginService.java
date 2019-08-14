package org.elastos.service;

import org.apache.commons.lang3.StringUtils;
import org.elastos.conf.EMailConfiguration;
import org.elastos.constants.Auth;
import org.elastos.dao.AdminRepository;
import org.elastos.constants.RetCode;
import org.elastos.dto.Admin;
import org.elastos.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
public class LoginService {

    private static Logger logger = LoggerFactory.getLogger(LoginService.class);

    @Autowired
    AdminRepository adminRepository;

    @Autowired
    EMailConfiguration eMailConf;

    @Autowired
    TokenUtil tokenUtil;

    public String userReg(String email, String name, String password) {
        if(StringUtils.isAnyBlank(email, name, password)){
            logger.error("userReg parameter has null");
            System.out.println("userReg parameter has null");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("传入参数不能为空").toJsonString();
        }

        Optional<Admin> ret = adminRepository.findByEmail(email);
        if (ret.isPresent()) {
            logger.error("userReg email duplicated.");
            System.out.println("userReg email duplicated.");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("邮箱已占用").toJsonString();
        }

        Admin user = new Admin();
        user.setEmail(email);
        user.setName(name);
        String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
        SimpleHash hash = new SimpleHash("md5", password, salt, 2);
        String encodedPassword = hash.toHex();
        user.setPassword(encodedPassword);
        user.setSalt(salt);
        user = adminRepository.save(user);
        if (null == user.getId()) {
            logger.error("userReg user database save failed.");
            System.out.println("userReg user database save failed.");
            return new ServerResponse().setState(RetCode.ERROR_DATABASE).setMsg("服务器内部错误").toJsonString();
        }

        return new ServerResponse().setState(RetCode.SUCCESS).setData("注册成功").toJsonString();
    }

    public String userLogin(HttpSession session, String email, String password) {
        if(StringUtils.isAnyBlank(email, password)){
            logger.error("userLogin parameter has null");
            System.out.println("userLogin parameter has null");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("传入参数不能为空").toJsonString();
        }

        Optional<Admin> ret = adminRepository.findByEmail(email);
        if (!ret.isPresent()) {
            logger.error("userLogin email not found.");
            System.out.println("userLogin email not found.");
            return new ServerResponse().setState(RetCode.ERROR_DATA_NOT_FOUND).setMsg("邮箱未注册").toJsonString();
        }

        Admin user = ret.get();
        String salt = user.getSalt();
        SimpleHash hash = new SimpleHash("md5", password, salt, 2);
        String encodedPassword = hash.toHex();
        if(!encodedPassword.equals(user.getPassword())){
            logger.error("userLogin password wrong.");
            System.out.println("userLogin password wrong.");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("密码错误").toJsonString();
        }

        session.setAttribute(Auth.USER_ID, user.getId());

        String token = tokenUtil.createToken(user.getId());

        Map<String, String> data = new HashMap<>();
        data.put("token", token);

        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

    public String userLogout(HttpSession session, Long uid) {
        session.removeAttribute(Auth.USER_ID);
        String token = tokenUtil.userToToken(uid);
        tokenUtil.deleteToken(token, uid);
        return new ServerResponse().setState(RetCode.SUCCESS).toJsonString();
    }

    public String authByToken(Long uid) {
        Map<String, Long> data = new HashMap<>();
        data.put("uid", uid);
        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

    public String getUserInfo (Long uid) {
        Optional<Admin> ret = adminRepository.findById(uid);
        if(!ret.isPresent()){
            logger.error("getUserInfo uid not found.");
            System.out.println("getUserInfo uid not found.");
            return new ServerResponse().setState(RetCode.ERROR_DATA_NOT_FOUND).setMsg("用户未注册").toJsonString();
        }

        Admin user = ret.get();
        Map<String, String> data = new HashMap<>();
        data.put("name", user.getName());
        data.put("mail", user.getEmail());

        return new ServerResponse().setState(RetCode.SUCCESS).setData(data).toJsonString();
    }

    public String updatePassword (String oldPassword, String newPassword, Long uid) {
        if(StringUtils.isAnyBlank(oldPassword, newPassword)){
            logger.error("updatePassword parameter has null");
            System.out.println("updatePassword parameter has null");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("传入参数不能为空").toJsonString();
        }
        Optional<Admin> ret = adminRepository.findById(uid);
        if(!ret.isPresent()){
            logger.error("updatePassword uid not found.");
            System.out.println("updatePassword uid not found.");
            return new ServerResponse().setState(RetCode.ERROR_DATA_NOT_FOUND).setMsg("用户未注册").toJsonString();
        }

        Admin user = ret.get();
        String salt = user.getSalt();
        SimpleHash hash = new SimpleHash("md5", oldPassword, salt, 2);
        String encodedPassword = hash.toHex();
        if(!encodedPassword.equals(user.getPassword())){
            logger.error("updatePassword password wrong.");
            System.out.println("updatePassword password wrong.");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("密码错误").toJsonString();
        }

        SimpleHash newHash = new SimpleHash("md5", newPassword, salt, 2);
        String newEncodedPassword = newHash.toHex();
        user.setPassword(newEncodedPassword);

        adminRepository.save(user);
        return new ServerResponse().setState(RetCode.SUCCESS).toJsonString();
    }

    public String retrievePassword (HttpSession session, String email) {
        if(StringUtils.isBlank(email)){
            logger.error("retrievePassword parameter has null");
            System.out.println("retrievePassword parameter has null");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("传入参数不能为空").toJsonString();
        }

        Optional<Admin> ret = adminRepository.findByEmail(email);
        if (!ret.isPresent()) {
            logger.error("retrievePassword email not found.");
            System.out.println("retrievePassword email not found.");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("邮箱未注册").toJsonString();
        }

        String code = VerificationUtil.create(session, email);

//        if(!SendEmail.send(eMailConf.getFrom(), email,
//                            eMailConf.getHost(), eMailConf.getSmtpPassword(),
//                            code)){
        try {
            if(!SendEmail.sendByAmazon(eMailConf.getFrom(), email, "The code is:"+code,
                    eMailConf.getSmtpUserName(), eMailConf.getSmtpPassword(),
                    eMailConf.getHost(), eMailConf.getPort())){
                logger.error("retrievePassword send mail fail.");
                System.out.println("retrievePassword send mail fail.");
                return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("发送邮件失败，请稍候再试").toJsonString();
            }
        } catch (Exception e){
            logger.error("retrievePassword send mail exception.");
            System.out.println("retrievePassword send mail exception.");
            return new ServerResponse().setState(RetCode.ERROR_INTERNAL).setMsg("发送邮件失败，请稍候再试").toJsonString();
        }

        return new ServerResponse().setState(RetCode.SUCCESS).setData("code:"+code).toJsonString();
    }

    public String updateRetrievePassword (HttpSession session, String code, String password) {
        if(StringUtils.isAnyBlank(code, password)){
            logger.error("updateRetrievePassword parameter has null");
            System.out.println("updateRetrievePassword parameter has null");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("传入参数不能为空").toJsonString();
        }

        if(!VerificationUtil.isCodeCorrect(session, code)){
            logger.error("updateRetrievePassword code wrong.");
            System.out.println("updateRetrievePassword code wrong.");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("验证码错误").toJsonString();
        }

        if(!VerificationUtil.isNotExpired(session)){
            logger.error("updateRetrievePassword expired.");
            System.out.println("updateRetrievePassword expired.");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("验证码已过期,请重新发送").toJsonString();
        }

        String email = VerificationUtil.getEmail(session);
        if (null == email) {
            logger.error("updateRetrievePassword email empty.");
            System.out.println("updateRetrievePassword email empty.");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("邮箱地址错误").toJsonString();
        }

        Optional<Admin> ret = adminRepository.findByEmail(email);
        if (!ret.isPresent()) {
            logger.error("updateRetrievePassword email not found.");
            System.out.println("updateRetrievePassword email not found.");
            return new ServerResponse().setState(RetCode.ERROR_PARAMETER).setMsg("邮箱未注册").toJsonString();
        }

        Admin user = ret.get();
        String salt = user.getSalt();
        SimpleHash hash = new SimpleHash("md5", password, salt, 2);
        String encodedPassword = hash.toHex();
        user.setPassword(encodedPassword);
        adminRepository.save(user);

        VerificationUtil.release(session);
        return new ServerResponse().setState(RetCode.SUCCESS).toJsonString();
    }

}
