package com.team007.room_escape.domain.comment.service;

import com.team007.room_escape.domain.comment.infra.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
}
