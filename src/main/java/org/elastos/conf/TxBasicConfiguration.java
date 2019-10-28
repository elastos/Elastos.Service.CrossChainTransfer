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
    private double ELA_FEE;
    private double ELA_CROSS_CHAIN_FEE;
    private double ELA_CROSS_CHAIN_SERVICE_MIN_FEE;
    private long RENEWAL_TIMEOUT;
    private Integer ELA_SAME_CHAIN_TRANSFER_WAIT;
    private Integer ELA_CROSS_CHAIN_TRANSFER_WAIT;
    private int WORKER_ADDRESS_SUM;
    private double WORKER_ADDRESS_RENEWAL_MIN_THRESHOLD;
    private double WORKER_ADDRESS_RENEWAL_VALUE;
    private double ETH_FEE=0.00021;
    private double ETH_CROSS_CHAIN_FEE=0.0001;
    private double ETH_CROSS_CHAIN_SERVICE_MIN_FEE=0.003;

    public double getELA_CROSS_CHAIN_FEE(){
        return ELA_CROSS_CHAIN_FEE;
    }

    public void setELA_CROSS_CHAIN_FEE(double ELA_CROSS_CHAIN_FEE) {
        this.ELA_CROSS_CHAIN_FEE = ELA_CROSS_CHAIN_FEE;
    }

    public long getONE_ELA() {
        return ONE_ELA;

    }

    public void setONE_ELA(long ONE_ELA) {
        this.ONE_ELA = ONE_ELA;
    }

    public double getELA_FEE() {
        return ELA_FEE;
    }

    public void setELA_FEE(double ELA_FEE) {
        this.ELA_FEE = ELA_FEE;
    }

    public double getELA_CROSS_CHAIN_SERVICE_MIN_FEE() {
        return ELA_CROSS_CHAIN_SERVICE_MIN_FEE;
    }

    public void setELA_CROSS_CHAIN_SERVICE_MIN_FEE(double ELA_CROSS_CHAIN_SERVICE_MIN_FEE) {
        this.ELA_CROSS_CHAIN_SERVICE_MIN_FEE = ELA_CROSS_CHAIN_SERVICE_MIN_FEE;
    }

    public long getRENEWAL_TIMEOUT() {
        return RENEWAL_TIMEOUT;
    }

    public void setRENEWAL_TIMEOUT(long RENEWAL_TIMEOUT) {
        this.RENEWAL_TIMEOUT = RENEWAL_TIMEOUT;
    }

    public int getWORKER_ADDRESS_SUM() {
        return WORKER_ADDRESS_SUM;
    }

    public void setWORKER_ADDRESS_SUM(int WORKER_ADDRESS_SUM) {
        this.WORKER_ADDRESS_SUM = WORKER_ADDRESS_SUM;
    }

    public double getWORKER_ADDRESS_RENEWAL_MIN_THRESHOLD() {
        return WORKER_ADDRESS_RENEWAL_MIN_THRESHOLD;
    }

    public void setWORKER_ADDRESS_RENEWAL_MIN_THRESHOLD(double WORKER_ADDRESS_RENEWAL_MIN_THRESHOLD) {
        this.WORKER_ADDRESS_RENEWAL_MIN_THRESHOLD = WORKER_ADDRESS_RENEWAL_MIN_THRESHOLD;
    }

    public double getWORKER_ADDRESS_RENEWAL_VALUE() {
        return WORKER_ADDRESS_RENEWAL_VALUE;
    }

    public void setWORKER_ADDRESS_RENEWAL_VALUE(double WORKER_ADDRESS_RENEWAL_VALUE) {
        this.WORKER_ADDRESS_RENEWAL_VALUE = WORKER_ADDRESS_RENEWAL_VALUE;
    }

    public Integer getELA_SAME_CHAIN_TRANSFER_WAIT() {
        return ELA_SAME_CHAIN_TRANSFER_WAIT;
    }

    public void setELA_SAME_CHAIN_TRANSFER_WAIT(Integer ELA_SAME_CHAIN_TRANSFER_WAIT) {
        this.ELA_SAME_CHAIN_TRANSFER_WAIT = ELA_SAME_CHAIN_TRANSFER_WAIT;
    }

    public Integer getELA_CROSS_CHAIN_TRANSFER_WAIT() {
        return ELA_CROSS_CHAIN_TRANSFER_WAIT;
    }

    public void setELA_CROSS_CHAIN_TRANSFER_WAIT(Integer ELA_CROSS_CHAIN_TRANSFER_WAIT) {
        this.ELA_CROSS_CHAIN_TRANSFER_WAIT = ELA_CROSS_CHAIN_TRANSFER_WAIT;
    }

    public double getETH_FEE() {
        return ETH_FEE;
    }

    public void setETH_FEE(double ETH_FEE) {
        this.ETH_FEE = ETH_FEE;
    }

    public double getETH_CROSS_CHAIN_FEE() {
        return ETH_CROSS_CHAIN_FEE;
    }

    public void setETH_CROSS_CHAIN_FEE(double ETH_CROSS_CHAIN_FEE) {
        this.ETH_CROSS_CHAIN_FEE = ETH_CROSS_CHAIN_FEE;
    }

    public double getETH_CROSS_CHAIN_SERVICE_MIN_FEE() {
        return ETH_CROSS_CHAIN_SERVICE_MIN_FEE;
    }

    public void setETH_CROSS_CHAIN_SERVICE_MIN_FEE(double ETH_CROSS_CHAIN_SERVICE_MIN_FEE) {
        this.ETH_CROSS_CHAIN_SERVICE_MIN_FEE = ETH_CROSS_CHAIN_SERVICE_MIN_FEE;
    }
}
