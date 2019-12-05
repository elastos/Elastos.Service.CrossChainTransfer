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
    private double ETH_FEE=0.00021;
    private double ETH_TRANSFER_CROSS_CHAIN_GAS_SAVE =0.0304;
    private double ETH_TRANSFER_GAS_SAVE =0.0003;

    private int OUTPUT_ADDRESS_SUM;
    private double OUTPUT_ADDRESS_SUPPLY_THRESHOLD;
    private Integer OUTPUT_ADDRESS_CAPABILITY;
    private double DEPOSIT_ADDRESS_SUPPLY_THRESHOLD;
    private double DEPOSIT_ADDRESS_CAPABILITY;

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

    public int getOUTPUT_ADDRESS_SUM() {
        return OUTPUT_ADDRESS_SUM;
    }

    public void setOUTPUT_ADDRESS_SUM(int OUTPUT_ADDRESS_SUM) {
        this.OUTPUT_ADDRESS_SUM = OUTPUT_ADDRESS_SUM;
    }

    public double getOUTPUT_ADDRESS_SUPPLY_THRESHOLD() {
        return OUTPUT_ADDRESS_SUPPLY_THRESHOLD;
    }

    public void setOUTPUT_ADDRESS_SUPPLY_THRESHOLD(double OUTPUT_ADDRESS_SUPPLY_THRESHOLD) {
        this.OUTPUT_ADDRESS_SUPPLY_THRESHOLD = OUTPUT_ADDRESS_SUPPLY_THRESHOLD;
    }

    public Integer getOUTPUT_ADDRESS_CAPABILITY() {
        return OUTPUT_ADDRESS_CAPABILITY;
    }

    public void setOUTPUT_ADDRESS_CAPABILITY(Integer OUTPUT_ADDRESS_CAPABILITY) {
        this.OUTPUT_ADDRESS_CAPABILITY = OUTPUT_ADDRESS_CAPABILITY;
    }

    public double getDEPOSIT_ADDRESS_SUPPLY_THRESHOLD() {
        return DEPOSIT_ADDRESS_SUPPLY_THRESHOLD;
    }

    public void setDEPOSIT_ADDRESS_SUPPLY_THRESHOLD(double DEPOSIT_ADDRESS_SUPPLY_THRESHOLD) {
        this.DEPOSIT_ADDRESS_SUPPLY_THRESHOLD = DEPOSIT_ADDRESS_SUPPLY_THRESHOLD;
    }

    public double getDEPOSIT_ADDRESS_CAPABILITY() {
        return DEPOSIT_ADDRESS_CAPABILITY;
    }

    public void setDEPOSIT_ADDRESS_CAPABILITY(double DEPOSIT_ADDRESS_CAPABILITY) {
        this.DEPOSIT_ADDRESS_CAPABILITY = DEPOSIT_ADDRESS_CAPABILITY;
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

    public double getETH_TRANSFER_CROSS_CHAIN_GAS_SAVE() {
        return ETH_TRANSFER_CROSS_CHAIN_GAS_SAVE;
    }

    public void setETH_TRANSFER_CROSS_CHAIN_GAS_SAVE(double ETH_TRANSFER_CROSS_CHAIN_GAS_SAVE) {
        this.ETH_TRANSFER_CROSS_CHAIN_GAS_SAVE = ETH_TRANSFER_CROSS_CHAIN_GAS_SAVE;
    }

    public double getETH_TRANSFER_GAS_SAVE() {
        return ETH_TRANSFER_GAS_SAVE;
    }

    public void setETH_TRANSFER_GAS_SAVE(double ETH_TRANSFER_GAS_SAVE) {
        this.ETH_TRANSFER_GAS_SAVE = ETH_TRANSFER_GAS_SAVE;
    }
}
