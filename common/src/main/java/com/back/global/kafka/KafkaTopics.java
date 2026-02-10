package com.back.global.kafka;

public class KafkaTopics {
    // Member events
    public static final String MEMBER_JOINED = "member.joined";
    public static final String MEMBER_MODIFIED = "member.modified";

    // Post events
    public static final String POST_CREATED = "post.created";
    public static final String POST_COMMENT_CREATED = "post.comment.created";

    // Market events
    public static final String MARKET_ORDER_PAYMENT_REQUESTED = "market.order.payment.requested";
    public static final String MARKET_ORDER_PAYMENT_COMPLETED = "market.order.payment.completed";

    // Cash events
    public static final String CASH_ORDER_PAYMENT_SUCCEEDED = "cash.order.payment.succeeded";
    public static final String CASH_ORDER_PAYMENT_FAILED = "cash.order.payment.failed";

    // Payout events
    public static final String PAYOUT_COMPLETED = "payout.completed";
}
