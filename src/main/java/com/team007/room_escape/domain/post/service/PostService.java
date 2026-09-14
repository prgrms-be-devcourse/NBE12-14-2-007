package com.team007.room_escape.domain.post.service;

import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
}
