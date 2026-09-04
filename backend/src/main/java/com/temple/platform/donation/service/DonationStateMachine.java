package com.temple.platform.donation.service;

import com.temple.platform.donation.domain.DonationStatus;
import com.temple.platform.donation.exception.InvalidDonationStateTransitionException;

public final class DonationStateMachine {

    private DonationStateMachine() {
    }

    public static void requireTransition(DonationStatus current, DonationStatus target) {
        if (current == target) {
            return;
        }
        if (current == DonationStatus.PENDING
                && (target == DonationStatus.COMPLETED || target == DonationStatus.FAILED)) {
            return;
        }
        throw new InvalidDonationStateTransitionException();
    }
}
