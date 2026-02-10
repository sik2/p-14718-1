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
@Profile("prod")
public class MemberDataInitProd {
    private final MemberDataInitProd self;
    private final MemberFacade memberFacade;

    @Value("${custom.system.apiKey}")
    private String systemApiKey;

    public MemberDataInitProd(
            @Lazy MemberDataInitProd self,
            MemberFacade memberFacade
    ) {
        this.self = self;
        this.memberFacade = memberFacade;
    }

    @Bean
    public ApplicationRunner memberDataInitProdApplicationRunner() {
        return args -> {
            self.makeBaseMembers();
        };
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

        log.info("Prod base members created: system, holding, admin");
    }
}
