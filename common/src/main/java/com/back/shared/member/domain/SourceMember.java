package com.back.shared.member.domain;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public abstract class SourceMember extends BaseMember {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private int id;
    @CreatedDate
    private LocalDateTime createDate;
    @LastModifiedDate
    private LocalDateTime modifyDate;

    public SourceMember(String username, String password, String nickname) {
        super(username, password, nickname, 0, UUID.randomUUID().toString());
    }
}
