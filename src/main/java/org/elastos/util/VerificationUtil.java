package org.elastos.util;

import org.elastos.constants.Auth;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.Date;

/**
 * Created by wanghan on 2019/1/15.
 */
@Component
public class VerificationUtil {
    private final static String CODE = "verify_code";
    private final static String CODE_TIME = "verify_code_time";
    private final static String EMAIL = "verify_email";
    private final static long EXPIRED_TIME = 15*60*1000;//15min


    public static String create(HttpSession session, String email) {
        String code = RandomString.createN(Auth.VERIFICATION_CODE_LENTH);
        session.setAttribute(CODE, code);
        session.setAttribute(CODE_TIME,new Date().getTime());
        session.setAttribute(EMAIL, email);
        return code;
    }

    public static Boolean isCodeCorrect(HttpSession session, String InCode) {
        String code = (String)session.getAttribute(CODE);
        if((null == code) || (!code.equals(InCode))){
            return false;
        } else {
            return true;
        }
    }

    public static Boolean isNotExpired(HttpSession session) {
        Long then = (Long)session.getAttribute(CODE_TIME);
        Long now = new Date().getTime();
        if ((null == then) || (now - then) > EXPIRED_TIME) {
            return false;
        } else {
            return true;
        }
    }

    public static String getEmail(HttpSession session) {
        String email = (String)session.getAttribute(EMAIL);
        return email;
    }

    public static void release(HttpSession session) {
        session.removeAttribute(CODE);
        session.removeAttribute(CODE_TIME);
    }

}
