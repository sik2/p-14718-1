package com.back.global.jpa.entity;

import com.back.global.global.GlobalConfig;
import com.back.standard.modelType.HasModelTypeCode;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public abstract class BaseEntity implements HasModelTypeCode {
    public abstract int getId();

    public abstract LocalDateTime getCreateDate();

    public abstract LocalDateTime getModifyDate();

    @Override
    public String getModelTypeCode() {
        return this.getClass().getSimpleName();
    }

    protected void publishEvent(Object event) {
        GlobalConfig.getEventPublisher().publish(event);
    }
}