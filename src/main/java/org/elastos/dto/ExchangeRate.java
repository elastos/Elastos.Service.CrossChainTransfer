package org.elastos.dto;

import javax.persistence.*;

@Entity
@Table(name = "exchange_rate",
        indexes = {@Index(name = "src_chain_id_index", columnList = "src_chain_id"),
                @Index(name = "dst_chain_id_index", columnList = "dst_chain_id")})
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "src_chain_id", nullable = false)
    private Long srcChainId;
    @Column(name = "src_chain_name", nullable = false, length = 100)
    private String srcChainName;
    @Column(name = "dst_chain_id", nullable = false)
    private Long dstChainId;
    @Column(name = "dst_chain_name", nullable = false, length = 100)
    private String dstChainName;
    @Column(name = "rate", nullable = false)
    private Double rate;
    @Column(name = "fee_rate", nullable = false)
    private Double fee_rate;
    private Double threshold_min;
    private Double threshold_max;
    @Column(name = "service_min_fee", nullable = false)
    private Double service_min_fee;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSrcChainId() {
        return srcChainId;
    }

    public void setSrcChainId(Long srcChainId) {
        this.srcChainId = srcChainId;
    }

    public String getSrcChainName() {
        return srcChainName;
    }

    public void setSrcChainName(String srcChainName) {
        this.srcChainName = srcChainName;
    }

    public Long getDstChainId() {
        return dstChainId;
    }

    public void setDstChainId(Long dstChainId) {
        this.dstChainId = dstChainId;
    }

    public String getDstChainName() {
        return dstChainName;
    }

    public void setDstChainName(String dstChainName) {
        this.dstChainName = dstChainName;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Double getFee_rate() {
        return fee_rate;
    }

    public void setFee_rate(Double fee_rate) {
        this.fee_rate = fee_rate;
    }

    public Double getThreshold_min() {
        return threshold_min;
    }

    public void setThreshold_min(Double threshold_min) {
        this.threshold_min = threshold_min;
    }

    public Double getThreshold_max() {
        return threshold_max;
    }

    public void setThreshold_max(Double threshold_max) {
        this.threshold_max = threshold_max;
    }

    public Double getService_min_fee() {
        return service_min_fee;
    }

    public void setService_min_fee(Double service_min_fee) {
        this.service_min_fee = service_min_fee;
    }
}
