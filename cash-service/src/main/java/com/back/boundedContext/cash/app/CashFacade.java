package com.back.boundedContext.cash.app;

import com.back.boundedContext.cash.domain.CashMember;
import com.back.boundedContext.cash.domain.Wallet;
import com.back.shared.cash.dto.CashMemberDto;
import com.back.shared.market.dto.OrderDto;
import com.back.shared.member.dto.MemberDto;
import com.back.shared.payout.dto.PayoutDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CashFacade {
    private final CashSupport cashSupport;
    private final CashSyncMemberUseCase cashSyncMemberUseCase;
    private final CashCreateWalletUseCase cashCreateWalletUseCase;
    private final CashCompleteOrderPaymentUseCase cashCompleteOrderPaymentUseCase;
    private final CashCompletePayoutUseCase cashCompletePayoutUseCase;

    @Transactional
    public CashMember syncMember(MemberDto member) {
        return cashSyncMemberUseCase.syncMember(member);
    }

    @Transactional
    public Wallet createWallet(CashMemberDto holder) {
        return cashCreateWalletUseCase.createWallet(holder);
    }

    @Transactional(readOnly = true)
    public Optional<CashMember> findMemberByUsername(String username) {
        return cashSupport.findMemberByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<Wallet> findWalletByHolder(CashMember holder) {
        return cashSupport.findWalletByHolder(holder);
    }

    @Transactional
    public void completeOrderPayment(OrderDto order, long pgPaymentAmount) {
        cashCompleteOrderPaymentUseCase.completeOrderPayment(order, pgPaymentAmount);
    }

    @Transactional(readOnly = true)
    public Optional<Wallet> findWalletByHolderId(int holderId) {
        return cashSupport.findWalletByHolderId(holderId);
    }

    @Transactional
    public void completePayout(PayoutDto payout) {
        cashCompletePayoutUseCase.completePayout(payout);
    }
}