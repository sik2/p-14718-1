package com.back.shared.post.dto;

import com.back.standard.modelType.HasModelTypeCode;

import java.time.LocalDateTime;

public record PostDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        int authorId,
        String authorName,
        String title,
        String content
) implements HasModelTypeCode {
    @Override
    public String getModelTypeCode() {
        return "Post";
    }
}
