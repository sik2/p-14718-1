package com.back.boundedContext.payout.app;

import com.back.boundedContext.payout.domain.Payout;
import com.back.boundedContext.payout.domain.PayoutMember;
import com.back.boundedContext.payout.out.PayoutMemberRepository;
import com.back.boundedContext.payout.out.PayoutRepository;
import com.back.shared.payout.dto.PayoutMemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayoutCreatePayoutUseCase {
    private final PayoutRepository payoutRepository;
    private final PayoutMemberRepository payoutMemberRepository;

    public Payout createPayout(int payeeId) {
        PayoutMember _payee = payoutMemberRepository.getReferenceById(payeeId);

        Payout payout = payoutRepository.save(
                new Payout(
                        _payee
                )
        );

        return payout;
    }
}
