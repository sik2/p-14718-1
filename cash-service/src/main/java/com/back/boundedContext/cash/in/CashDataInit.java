package com.back.boundedContext.cash.in;


import com.back.boundedContext.cash.app.CashFacade;
import com.back.boundedContext.cash.domain.CashLog;
import com.back.boundedContext.cash.domain.CashMember;
import com.back.boundedContext.cash.domain.Wallet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Slf4j
@Profile("!prod")
public class CashDataInit {
    private static final int WAIT_SECONDS = 30;
    private static final int RETRY_INTERVAL_MS = 1000;

    private final CashDataInit self;
    private final CashFacade cashFacade;

    public CashDataInit(
            @Lazy CashDataInit self,
            CashFacade cashFacade
    ) {
        this.self = self;
        this.cashFacade = cashFacade;
    }

    @Bean
    public ApplicationRunner cashDataInitApplicationRunner() {
        return args -> {
            if (waitForMemberSync()) {
                self.makeBaseWallets();
                self.makeBaseCredits();
            }
        };
    }

    private boolean waitForMemberSync() {
        log.info("Waiting {}s for member sync...", WAIT_SECONDS);

        int maxRetries = WAIT_SECONDS * 1000 / RETRY_INTERVAL_MS;
        for (int i = 0; i < maxRetries; i++) {
            if (cashFacade.findMemberByUsername("user1").isPresent()) {
                log.info("Member sync completed. Proceeding with data init.");
                return true;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("Member sync timeout after {}s. Skipping data init.", WAIT_SECONDS);
        return false;
    }

    @Transactional
    public void makeBaseWallets() {
        var members = java.util.List.of("system", "holding", "admin", "user1", "user2", "user3");

        for (String username : members) {
            var memberOpt = cashFacade.findMemberByUsername(username);
            if (memberOpt.isEmpty()) continue;

            CashMember member = memberOpt.get();
            if (cashFacade.findWalletByHolder(member).isPresent()) continue;

            cashFacade.createWallet(member.toDto());
        }
    }

    @Transactional
    public void makeBaseCredits() {
        var user1MemberOpt = cashFacade.findMemberByUsername("user1");
        if (user1MemberOpt.isEmpty()) return;

        CashMember user1Member = user1MemberOpt.get();
        var user1WalletOpt = cashFacade.findWalletByHolder(user1Member);
        if (user1WalletOpt.isEmpty()) return;

        Wallet user1Wallet = user1WalletOpt.get();
        if (user1Wallet.hasBalance()) return;

        user1Wallet.credit(150_000, CashLog.EventType.충전__무통장입금);
        user1Wallet.credit(100_000, CashLog.EventType.충전__무통장입금);
        user1Wallet.credit(50_000, CashLog.EventType.충전__무통장입금);

        var user2MemberOpt = cashFacade.findMemberByUsername("user2");
        if (user2MemberOpt.isPresent()) {
            var user2WalletOpt = cashFacade.findWalletByHolder(user2MemberOpt.get());
            user2WalletOpt.ifPresent(wallet -> wallet.credit(150_000, CashLog.EventType.충전__무통장입금));
        }
    }
}
