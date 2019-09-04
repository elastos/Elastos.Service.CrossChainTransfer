package org.elastos.constants;

public class ExchangeState {
    /**
     *  交易类型 充值，消费，收入，提现
     */
    public static final String EX_STATE_RENEWAL_WAITING = "renewal_waiting";//Wait user renewal to transfer service
    public static final String EX_STATE_RENEWAL_TIMEOUT = "renewal_timeout";//User not renewal for a long time
    public static final String EX_STATE_TRANSFERRING = "transferring";//Exchanging
    public static final String EX_STATE_TRANSFER_FINISH = "transfer_finish";//ExchangeRecord is done successfully
    public static final String EX_STATE_TRANSFER_FAILED = "transfer_failed";//ExchangeRecord failed.
    public static final String EX_STATE_BACKING = "backing";//This transaction is out of range, we sent it back.
    public static final String EX_STATE_BACK_FINISH = "back_finish";//sent back success.
    public static final String EX_STATE_BACK_FAILED = "back_failed";//Sent back failed.
    public static final String EX_STATE_DIRECT_TRANSFERRING = "direct_transferring";//We sent it with cross transfer directly.
    public static final String EX_STATE_DIRECT_TRANSFERRING_WAIT_GATHER = "direct_transferring_wait_gather";//we sent it with cross transfer directly, then we wait for gather fee.
    public static final String EX_STATE_DIRECT_TRANSFER_FINISH = "direct_transfer_finish";//This transaction is out of range, we sent it with cross transfer.
    public static final String EX_STATE_DIRECT_TRANSFER_FAILED = "direct_transfer_failed";//This transaction is out of range, we sent it with cross transfer.
}
