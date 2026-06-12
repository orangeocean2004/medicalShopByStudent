package com.medshop.shop.service;

import com.medshop.shop.entity.Payment;
import com.medshop.shop.mapper.PaymentMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 支付业务层（M1）。
 *
 * <p>与下单解耦：下单只生成「待支付」记录，真正的支付由独立的支付渠道回调驱动
 * （本课设以 mock 形式体现该解耦，不接入真实支付）。这是《代码设计》中
 * "createOrder 不直接发起支付"修正点的落地。
 */
@Service
public class PaymentService {

    private final PaymentMapper paymentMapper;

    public PaymentService(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    /** 为订单生成待支付记录。 */
    public Payment createPayment(Long orderId, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setChannel(1);     // 默认微信，实际由用户支付时选择
        payment.setStatus(0);      // 待支付
        paymentMapper.insert(payment);
        return payment;
    }
}
