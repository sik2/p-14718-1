package com.back.shared.post.event;

import com.back.shared.post.dto.PostCommentDto;

public record PostCommentCreatedEvent(PostCommentDto postComment) {}
