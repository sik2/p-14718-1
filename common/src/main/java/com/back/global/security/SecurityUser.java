package com.back.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class SecurityUser extends User {
    @Getter
    private int id;
    @Getter
    private String nickname;

    public SecurityUser(
            int id,
            String username,
            String password,
            String nickname,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password, authorities);
        this.id = id;
        this.nickname = nickname;
    }
}
