package com.back.shared.member.domain;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@MappedSuperclass
@Getter
@Setter(value = PROTECTED)
@NoArgsConstructor
public abstract class BaseMember extends BaseEntity {
    @Column(unique = true)
    private String username;
    private String password;
    private String nickname;
    private int activityScore;
    @Column(unique = true)
    private String apiKey;

    public BaseMember(String username, String password, String nickname, int activityScore, String apiKey) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.activityScore = activityScore;
        this.apiKey = apiKey;
    }

    public boolean isSystem() {
        return "system".equals(username);
    }

    public boolean isAdmin() {
        if ("system".equals(username)) return true;
        if ("admin".equals(username)) return true;
        return false;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthoritiesAsStringList()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private List<String> getAuthoritiesAsStringList() {
        List<String> authorities = new ArrayList<>();
        if (isAdmin()) authorities.add("ROLE_ADMIN");
        return authorities;
    }
}
