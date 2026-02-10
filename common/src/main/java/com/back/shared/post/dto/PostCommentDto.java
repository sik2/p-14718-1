package com.back.shared.post.dto;

import java.time.LocalDateTime;

public record PostCommentDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        int postId,
        int authorId,
        String authorName,
        String content
) {}
