package com.back.boundedContext.member.in;

import com.back.boundedContext.member.app.MemberFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Slf4j
@Profile("!prod")
public class MemberDataInit {
    private static final int WAIT_FOR_OTHER_MODULES_SECONDS = 10;

    private final MemberDataInit self;
    private final MemberFacade memberFacade;

    @Value("${custom.system.apiKey}")
    private String systemApiKey;

    public MemberDataInit(
            @Lazy MemberDataInit self,
            MemberFacade memberFacade
    ) {
        this.self = self;
        this.memberFacade = memberFacade;
    }

    @Bean
    public ApplicationRunner memberDataInitApplicationRunner() {
        return args -> {
            waitForOtherModules();
            self.makeBaseMembers();
        };
    }

    private void waitForOtherModules() {
        log.info("Waiting {}s for other modules to start...", WAIT_FOR_OTHER_MODULES_SECONDS);
        try {
            Thread.sleep(WAIT_FOR_OTHER_MODULES_SECONDS * 1000L);
            log.info("Wait complete. Proceeding with member data init.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wait interrupted.");
        }
    }

    @Transactional
    public void makeBaseMembers() {
        if (memberFacade.count() > 0) {
            log.info("Members already exist. Skipping creation.");
            return;
        }

        var systemMember = memberFacade.join("system", "1234", "시스템").getData();
        systemMember.changeApiKey(systemApiKey);

        memberFacade.join("holding", "1234", "홀딩");
        memberFacade.join("admin", "1234", "관리자");
        memberFacade.join("user1", "1234", "유저1");
        memberFacade.join("user2", "1234", "유저2");
        memberFacade.join("user3", "1234", "유저3");
    }
}
