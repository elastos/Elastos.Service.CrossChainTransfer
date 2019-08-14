package org.elastos.dto;


import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "internal_tx_record",
        indexes = {@Index(name = "txid_index", columnList = "txid", unique = true),
                @Index(name = "src_addr_index", columnList = "src_addr"),
                @Index(name = "dst_addr_index", columnList = "dst_addr")})
@EntityListeners(AuditingEntityListener.class)
public class InternalTxRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "src_chain_id")
    private Long srcChainId;
    @Column(name = "src_addr", nullable = false)
    private String srcAddr;
    @Column(name = "dst_chain_id")
    private Long dstChainId;
    @Column(name = "dst_addr", nullable = false)
    private String dstAddr;
    @Column(name = "txid", nullable = false, length = 64)
    private String txid;
    @Column(name = "value", nullable = false)
    private Double value;
    @Column(name = "create_time", updatable = false, nullable = false)
    @CreatedDate
    private Date createTime;

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

    public String getSrcAddr() {
        return srcAddr;
    }

    public void setSrcAddr(String srcAddr) {
        this.srcAddr = srcAddr;
    }

    public Long getDstChainId() {
        return dstChainId;
    }

    public void setDstChainId(Long dstChainId) {
        this.dstChainId = dstChainId;
    }

    public String getDstAddr() {
        return dstAddr;
    }

    public void setDstAddr(String dstAddr) {
        this.dstAddr = dstAddr;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}


