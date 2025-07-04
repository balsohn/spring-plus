package org.example.expert.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.user.dto.response.UserSearchResponse;
import org.example.expert.domain.user.service.UserSearchService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserSearchController {

    private final UserSearchService userSearchService;

    @GetMapping("/users/search")
    public ResponseEntity<Page<UserSearchResponse>> searchUsersByNickname(
            @RequestParam String nickname,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userSearchService.searchUsersByNickname(nickname, page, size));
    }

    @GetMapping("/users/search/like")
    public ResponseEntity<Page<UserSearchResponse>> searchUsersByNicknameLike(
            @RequestParam String nickname,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userSearchService.searchUsersByNicknameLike(nickname, page, size));
    }
}
