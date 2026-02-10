package com.back.boundedContext.member.in;

import com.back.boundedContext.member.app.MemberFacade;
import com.back.boundedContext.member.domain.Member;
import com.back.global.exception.DomainException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.shared.member.dto.MemberDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/member/members")
@RequiredArgsConstructor
public class ApiV1MemberController {
    private final MemberFacade memberFacade;
    private final Rq rq;

    @GetMapping("/randomSecureTip")
    public String getRandomSecureTip() {
        return memberFacade.getRandomSecureTip();
    }

    public record JoinReqBody(
            @NotBlank @Size(min = 2, max = 30) String username,
            @NotBlank @Size(min = 2, max = 30) String password,
            @NotBlank @Size(min = 2, max = 30) String nickname
    ) {}

    @PostMapping("/join")
    @Transactional
    public RsData<MemberDto> join(@Valid @RequestBody JoinReqBody reqBody) {
        RsData<Member> rs = memberFacade.join(reqBody.username(), reqBody.password(), reqBody.nickname());
        return new RsData<>(rs.getResultCode(), rs.getMsg(), rs.getData().toDto());
    }

    public record LoginReqBody(
            @NotBlank @Size(min = 2, max = 30) String username,
            @NotBlank @Size(min = 2, max = 30) String password
    ) {}

    public record LoginResBody(
            MemberDto item,
            String apiKey,
            String accessToken
    ) {}

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public RsData<LoginResBody> login(@Valid @RequestBody LoginReqBody reqBody) {
        Member member = memberFacade.login(reqBody.username(), reqBody.password());
        String accessToken = memberFacade.genAccessToken(member);

        rq.setCookie("apiKey", member.getApiKey());
        rq.setCookie("accessToken", accessToken);

        return new RsData<>(
                "200-1",
                "%s님 환영합니다.".formatted(member.getNickname()),
                new LoginResBody(member.toDto(), member.getApiKey(), accessToken)
        );
    }

    @DeleteMapping("/logout")
    public RsData<Void> logout() {
        rq.deleteCookie("apiKey");
        rq.deleteCookie("accessToken");

        return new RsData<>("200-1", "로그아웃 되었습니다.");
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public MemberDto me() {
        return memberFacade.findById(rq.getActor().getId())
                .map(Member::toDto)
                .orElseThrow(() -> new DomainException("404-1", "회원을 찾을 수 없습니다."));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public MemberDto getMemberById(@PathVariable int id) {
        return memberFacade.findById(id)
                .map(Member::toDto)
                .orElse(null);
    }

    @GetMapping("/by-apikey/{apiKey}")
    @Transactional(readOnly = true)
    public MemberDto getMemberByApiKey(@PathVariable String apiKey) {
        return memberFacade.findByApiKey(apiKey)
                .map(Member::toDto)
                .orElse(null);
    }

    public record ValidateTokenReqBody(String accessToken) {}

    @PostMapping("/validate-token")
    @Transactional(readOnly = true)
    public MemberDto validateToken(@RequestBody ValidateTokenReqBody reqBody) {
        var payload = memberFacade.payload(reqBody.accessToken());
        if (payload == null) return null;

        int id = (int) payload.get("id");
        return memberFacade.findById(id)
                .map(Member::toDto)
                .orElse(null);
    }

    @PostMapping("/{id}/access-token")
    @Transactional(readOnly = true)
    public RsData<String> generateAccessToken(@PathVariable int id) {
        return memberFacade.findById(id)
                .map(member -> new RsData<>("200-1", "토큰 생성 성공", memberFacade.genAccessToken(member)))
                .orElse(new RsData<>("404-1", "회원을 찾을 수 없습니다.", null));
    }
}
