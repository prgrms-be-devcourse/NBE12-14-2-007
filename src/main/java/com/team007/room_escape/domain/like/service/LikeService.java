package com.team007.room_escape.domain.like.service;

import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {

	private final LikeRepository likeRepository;
}
