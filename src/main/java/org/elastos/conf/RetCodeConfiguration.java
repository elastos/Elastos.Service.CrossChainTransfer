/**
 * Copyright (c) 2017-2018 The Elastos Developers
 * <p>
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.elastos.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("retcode")
public class RetCodeConfiguration {

    private int SUCC             ;
    private int BAD_REQUEST      ;
    private int NOT_FOUND        ;
    private int INTERNAL_ERROR   ;
    private int PROCESS_ERROR    ;

    public int PROCESS_ERROR() {
        return PROCESS_ERROR;
    }
    public void setPROCESS_ERROR(int PROCESS_ERROR) {
        this.PROCESS_ERROR = PROCESS_ERROR;
    }
    public int SUCC() {
        return SUCC;
    }

    public void setSUCC(int SUCC) {
        this.SUCC = SUCC;
    }

    public int BAD_REQUEST() {
        return BAD_REQUEST;
    }

    public void setBAD_REQUEST(int BAD_REQUEST) {
        this.BAD_REQUEST = BAD_REQUEST;
    }

    public int NOT_FOUND() {
        return NOT_FOUND;
    }

    public void setNOT_FOUND(int NOT_FOUND) {
        this.NOT_FOUND = NOT_FOUND;
    }

    public int INTERNAL_ERROR() {
        return INTERNAL_ERROR;
    }

    public void setINTERNAL_ERROR(int INTERNAL_ERROR) {
        this.INTERNAL_ERROR = INTERNAL_ERROR;
    }
}
