package com.back.global.security;

import com.back.global.exception.DomainException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.shared.member.dto.MemberDto;
import com.back.standard.ut.Util;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final Rq rq;
    private final AuthTokenValidator authTokenValidator;

    @Value("${custom.system.apiKey}")
    private String systemApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            work(request, response, filterChain);
        } catch (DomainException e) {
            RsData<Void> rsData = new RsData<>(e.getResultCode(), e.getMsg());
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(Integer.parseInt(e.getResultCode().split("-")[0]));
            response.getWriter().write(Util.json.toString(rsData));
        }
    }

    private void work(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (List.of(
                "/api/v1/member/members/login",
                "/api/v1/member/members/logout",
                "/api/v1/member/members/join"
        ).contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey;
        String accessToken;

        String headerAuthorization = rq.getHeader("Authorization", "");

        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer "))
                throw new DomainException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.");

            String[] headerAuthorizationBits = headerAuthorization.split(" ", 3);

            apiKey = headerAuthorizationBits[1];
            accessToken = headerAuthorizationBits.length == 3 ? headerAuthorizationBits[2] : "";
        } else {
            apiKey = rq.getCookieValue("apiKey", "");
            accessToken = rq.getCookieValue("accessToken", "");
        }

        boolean isApiKeyExists = !apiKey.isBlank();
        boolean isAccessTokenExists = !accessToken.isBlank();

        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response);
            return;
        }

        // 시스템 apiKey인 경우 시스템 사용자로 인증 설정 후 통과
        if (isApiKeyExists && apiKey.equals(systemApiKey)) {
            UserDetails systemUser = new SecurityUser(
                    1,
                    "system",
                    "",
                    "시스템",
                    Collections.emptyList()
            );
            Authentication systemAuth = new UsernamePasswordAuthenticationToken(
                    systemUser,
                    systemUser.getPassword(),
                    systemUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(systemAuth);
            filterChain.doFilter(request, response);
            return;
        }

        MemberDto member = null;
        boolean isAccessTokenValid = false;

        if (isAccessTokenExists) {
            member = authTokenValidator.validateToken(accessToken);
            if (member != null) {
                isAccessTokenValid = true;
            }
        }

        if (member == null && isApiKeyExists) {
            member = authTokenValidator.findByApiKey(apiKey);
            if (member == null) {
                throw new DomainException("401-3", "API 키가 유효하지 않습니다.");
            }
        }

        if (member == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAccessTokenExists && !isAccessTokenValid) {
            String newAccessToken = authTokenValidator.generateAccessToken(member.id());
            if (newAccessToken != null) {
                rq.setCookie("accessToken", newAccessToken);
                rq.setHeader("Authorization", newAccessToken);
            }
        }

        UserDetails user = new SecurityUser(
                member.id(),
                member.username(),
                "",
                member.nickname(),
                Collections.emptyList()
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                user.getPassword(),
                user.getAuthorities()
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
