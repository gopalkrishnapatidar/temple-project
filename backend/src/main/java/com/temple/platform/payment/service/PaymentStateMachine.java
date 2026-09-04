package com.temple.platform.payment.service;

import com.temple.platform.payment.domain.PaymentStatus;
import com.temple.platform.payment.exception.InvalidPaymentStateTransitionException;

public final class PaymentStateMachine {

    private PaymentStateMachine() {
    }

    public static void requireTransition(PaymentStatus current, PaymentStatus target) {
        if (current == target) {
            return;
        }
        if (current == PaymentStatus.PENDING
                && (target == PaymentStatus.SUCCEEDED || target == PaymentStatus.FAILED)) {
            return;
        }
        throw new InvalidPaymentStateTransitionException();
    }

    public static boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED;
    }
}
