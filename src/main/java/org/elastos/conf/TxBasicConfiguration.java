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
@ConfigurationProperties("txbasic")
public class TxBasicConfiguration {
    private long ONE_ELA;
    private double FEE;
    private double CROSS_CHAIN_FEE;
    private long TIMEOUT;
    private int WORKER_ADDRESS_SUM;
    private double WORKER_ADDRESS_MIN_THRESHOLD;
    private double WORKER_ADDRESS_RENEWAL_VALUE;
    private Integer SAME_CHAIN_TRANSFER_WAIT;
    private Integer CROSS_CHAIN_TRANSFER_WAIT;
    public double getCROSS_CHAIN_FEE(){
        return CROSS_CHAIN_FEE;
    }

    public void setCROSS_CHAIN_FEE(double CROSS_CHAIN_FEE) {
        this.CROSS_CHAIN_FEE = CROSS_CHAIN_FEE;
    }

    public long getONE_ELA() {
        return ONE_ELA;

    }

    public void setONE_ELA(long ONE_ELA) {
        this.ONE_ELA = ONE_ELA;
    }

    public double getFEE() {
        return FEE;
    }

    public void setFEE(double FEE) {
        this.FEE = FEE;
    }

    public long getTIMEOUT() {
        return TIMEOUT;
    }

    public void setTIMEOUT(long TIMEOUT) {
        this.TIMEOUT = TIMEOUT;
    }

    public int getWORKER_ADDRESS_SUM() {
        return WORKER_ADDRESS_SUM;
    }

    public void setWORKER_ADDRESS_SUM(int WORKER_ADDRESS_SUM) {
        this.WORKER_ADDRESS_SUM = WORKER_ADDRESS_SUM;
    }

    public double getWORKER_ADDRESS_MIN_THRESHOLD() {
        return WORKER_ADDRESS_MIN_THRESHOLD;
    }

    public void setWORKER_ADDRESS_MIN_THRESHOLD(double WORKER_ADDRESS_MIN_THRESHOLD) {
        this.WORKER_ADDRESS_MIN_THRESHOLD = WORKER_ADDRESS_MIN_THRESHOLD;
    }

    public double getWORKER_ADDRESS_RENEWAL_VALUE() {
        return WORKER_ADDRESS_RENEWAL_VALUE;
    }

    public void setWORKER_ADDRESS_RENEWAL_VALUE(double WORKER_ADDRESS_RENEWAL_VALUE) {
        this.WORKER_ADDRESS_RENEWAL_VALUE = WORKER_ADDRESS_RENEWAL_VALUE;
    }

    public Integer getSAME_CHAIN_TRANSFER_WAIT() {
        return SAME_CHAIN_TRANSFER_WAIT;
    }

    public void setSAME_CHAIN_TRANSFER_WAIT(Integer SAME_CHAIN_TRANSFER_WAIT) {
        this.SAME_CHAIN_TRANSFER_WAIT = SAME_CHAIN_TRANSFER_WAIT;
    }

    public Integer getCROSS_CHAIN_TRANSFER_WAIT() {
        return CROSS_CHAIN_TRANSFER_WAIT;
    }

    public void setCROSS_CHAIN_TRANSFER_WAIT(Integer CROSS_CHAIN_TRANSFER_WAIT) {
        this.CROSS_CHAIN_TRANSFER_WAIT = CROSS_CHAIN_TRANSFER_WAIT;
    }
}
