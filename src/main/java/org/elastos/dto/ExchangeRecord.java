package org.elastos.dto;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "exchange_record",
        indexes = {@Index(name = "did_index", columnList = "did"),
                @Index(name = "src_address_index", columnList = "src_address"),
                @Index(name = "dst_address_index", columnList = "dst_address"),
                @Index(name = "state_index", columnList = "state")})
@EntityListeners(AuditingEntityListener.class)
public class ExchangeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "did")
    private String did;
    @Column(name = "src_chain_id", nullable = false)
    private Long srcChainId;
    @Column(name = "src_wallet_id", nullable = false)
    private Long srcWalletId;
    @Column(name = "src_address_id", nullable = false)
    private Integer srcAddressId;
    @Column(name = "src_address", nullable = false, length = 256)
    private String srcAddress;
    @Column(name = "`src_value`")
    private Double srcValue;
    @Column(name = "dst_chain_id", nullable = false)
    private Long dstChainId;
    @Column(name = "dst_address", nullable = false, length = 256)
    private String dstAddress;
    @Column(name = "dst_txid", length = 256)
    private String dstTxid;
    @Column(name = "`dst_value`")
    private Double dstValue;
    @Column(name = "back_address", nullable = false, length = 256)
    private String backAddress;
    @Column(name = "back_txid", length = 256)
    private String backTxid;
    @Column(name = "`rate`", nullable = false)
    private Double rate;
    @Column(name = "`fee_rate`", nullable = false)
    private Double fee_rate;
    @Column(name = "`fee`")
    private Double fee;
    @Column(name = "`state`", nullable = false, length = 40)
    private String state;
    @Column(name = "create_time", updatable = false, nullable = false)
    @CreatedDate
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public Long getSrcChainId() {
        return srcChainId;
    }

    public void setSrcChainId(Long srcChainId) {
        this.srcChainId = srcChainId;
    }

    public Long getSrcWalletId() {
        return srcWalletId;
    }

    public void setSrcWalletId(Long srcWalletId) {
        this.srcWalletId = srcWalletId;
    }

    public Integer getSrcAddressId() {
        return srcAddressId;
    }

    public void setSrcAddressId(Integer srcAddressId) {
        this.srcAddressId = srcAddressId;
    }

    public String getSrcAddress() {
        return srcAddress;
    }

    public void setSrcAddress(String srcAddress) {
        this.srcAddress = srcAddress;
    }

    public Double getSrcValue() {
        return srcValue;
    }

    public void setSrcValue(Double srcValue) {
        this.srcValue = srcValue;
    }

    public Long getDstChainId() {
        return dstChainId;
    }

    public void setDstChainId(Long dstChainId) {
        this.dstChainId = dstChainId;
    }

    public String getDstAddress() {
        return dstAddress;
    }

    public void setDstAddress(String dstAddress) {
        this.dstAddress = dstAddress;
    }

    public String getDstTxid() {
        return dstTxid;
    }

    public void setDstTxid(String dstTxid) {
        this.dstTxid = dstTxid;
    }

    public Double getDstValue() {
        return dstValue;
    }

    public void setDstValue(Double dstValue) {
        this.dstValue = dstValue;
    }

    public String getBackAddress() {
        return backAddress;
    }

    public void setBackAddress(String backAddress) {
        this.backAddress = backAddress;
    }

    public String getBackTxid() {
        return backTxid;
    }

    public void setBackTxid(String backTxid) {
        this.backTxid = backTxid;
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

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}


