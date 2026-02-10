package com.back.boundedContext.payout.out;

import com.back.boundedContext.payout.domain.PayoutMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayoutMemberRepository extends JpaRepository<PayoutMember, Integer> {
    Optional<PayoutMember> findByUsername(String username);
}
