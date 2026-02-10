package com.back.boundedContext.member.app;

import com.back.boundedContext.member.domain.Member;
import com.back.boundedContext.member.out.MemberRepository;
import com.back.global.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberLoginUseCase {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Member login(String username, String password) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("401-1", "존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new DomainException("401-2", "비밀번호가 일치하지 않습니다.");
        }

        return member;
    }
}
