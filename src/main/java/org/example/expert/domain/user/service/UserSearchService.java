package org.example.expert.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.user.dto.response.UserSearchResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSearchService {

    private final UserRepository userRepository;

    public Page<UserSearchResponse> searchUsersByNickname(String nickname, int page, int size) {
        long startTime = System.currentTimeMillis();

        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<User> users = userRepository.findByNicknameExact(nickname, pageable);

        long endTime = System.currentTimeMillis();
        log.info("닉네임 검색 소요시간: {}ms, 검색어: {}, 결과 수: {}",
                endTime - startTime, nickname, users.getTotalElements());

        return users.map(user -> new UserSearchResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        ));
    }

    public Page<UserSearchResponse> searchUsersByNicknameLike(String nickname, int page, int size) {
        long startTime = System.currentTimeMillis();

        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<User> users = userRepository.findByNicknameLike(nickname, pageable);

        long endTime = System.currentTimeMillis();
        log.info("닉네임 LIKE 검색 소요시간: {}ms, 검색어: {}, 결과 수:{}",
                endTime - startTime, nickname, users.getTotalElements());

        return users.map(user -> new UserSearchResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        ));
    }
}
