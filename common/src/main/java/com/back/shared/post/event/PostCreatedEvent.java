package com.back.shared.post.event;

import com.back.shared.post.dto.PostDto;

public record PostCreatedEvent(PostDto post) {}
