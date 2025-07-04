package org.example.expert.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.user.dto.response.UserSearchResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@SpringBootTest
class UserDataProcessingTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSearchService userSearchService;

    @Test
    @Transactional
    @Rollback(value = false)
    void 기본_데이터_1000건_생성_및_검색_테스트() {
        log.info("==== 1000건 데이터 생성 및 검색 테스트 시작 ====");

        // 1000건 데이터 생성
        long startTime = System.currentTimeMillis();
        List<User> users = new ArrayList<>();

        for (int i = 1; i < 1000; i++) {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String nickname = "testuser_" + uniqueId;
            String email = "test" + i + "@example.com";

            User user = new User(email, "password123", UserRole.USER, nickname);
            users.add(user);
        }

        userRepository.saveAll(users);
        long endTime = System.currentTimeMillis();

        log.info("1000건 데이터 생성 완료. 소요시간: {}ms", endTime - startTime);

        // 생성된 데이터 확인
        long totalCount = userRepository.count();
        log.info("전체 사용자 수: {}", totalCount);

        // 검색 테스트
        if (!users.isEmpty()) {
            String testNickname = users.get(0).getNickname();

            // 정확한 검색
            Page<UserSearchResponse> exactResult = userSearchService.searchUsersByNickname(testNickname, 1, 10);
            log.info("정확한 검색 결과: {} 건", exactResult.getTotalElements());

            // LIKE 검색
            String partNickname = testNickname.substring(0, 8);
            Page<UserSearchResponse> likeResult = userSearchService.searchUsersByNicknameLike(partNickname, 1, 10);
            log.info("LIKE 검색 결과: {} 건", likeResult.getTotalElements());
        }

        log.info("==== 종료 ====");
    }

    @Test
    @Transactional
    @Rollback(value = false)
    void 배치_처리_10만건_생성_테스트() {
        log.info("==== 배치 처리 10만건 데이터 생성 테스트 시작 ====");

        int totalUsers= 100000;
        int batchSize = 1000;
        int batches = totalUsers / batchSize;

        long totalStartTime = System.currentTimeMillis();

        for (int batchIndex = 0; batchIndex < batches; batchIndex++) {
            long batchStartTime = System.currentTimeMillis();

            List<User> users = new ArrayList<>();

            for (int i = 0; i < batchSize; i++) {
                String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                String nickname = "batchuser_" + uniqueId;
                String email = "batch" + (batchIndex * batchSize + i) + "@example.com";

                User user = new User(email, "password123", UserRole.USER, nickname);
                users.add(user);
            }

            // 배치 저장
            userRepository.saveAll(users);

            long batchEndTime = System.currentTimeMillis();

            if ((batchIndex + 1) %  10 == 0) {
                log.info("배치 진행상황: {}/{} 완료, 현재 배치 소요시간: {}ms",
                        batchIndex + 1, batches, batchEndTime - batchStartTime);
            }

            // 메모리 정리
            if ((batchIndex + 1) % 50 == 0) {
                System.gc();
            }

            long totalEndTime = System.currentTimeMillis();

            // 결과 확인
            long finalCount = userRepository.count();
            log.info("10만건 배치 처리 완료");
            log.info("총 소요시간: {}ms ({}초)", totalEndTime - totalStartTime, (totalEndTime - totalStartTime) / 1000);
            log.info("전체 사용자 수: {}", finalCount);
            log.info("평균 배치 처리 시간: {}ms", (totalEndTime - totalStartTime) / batches);

            log.info("=== 10만건 종료 ===");
        }
    }

    @Test
    void 검색_성능_비교_테스트() {
        log.info("=== 검색 성능 비교 테스트 시작 ===");

        // 전체 데이터 수 확인
        long totalUsers = userRepository.count();
        log.info("현재 전체 사용자 수: {}", totalUsers);

        if (totalUsers < 1000) {
            log.warn("테스트용 데이터가 부족합니다.");
            return;
        }

        // 테스트용 닉네임 찾기
        List<User> sampleUsers = userRepository.findAll().stream().limit(5).toList();

        for (User user : sampleUsers) {
            String testNickname = user.getNickname();
            log.info("\n--- 테스트 닉네임: {}---", testNickname);

            // 정확한 매칭 검색
            long start1 = System.currentTimeMillis();
            Page<UserSearchResponse> exactResult = userSearchService.searchUsersByNickname(testNickname, 1, 10);
            long end1 = System.currentTimeMillis();

            // LIKE 검색
            String partNickname = testNickname.substring(0, Math.min(8, testNickname.length()));
            long start2 = System.currentTimeMillis();
            Page<UserSearchResponse> likeResult = userSearchService.searchUsersByNicknameLike(partNickname, 1, 10);
            long end2 = System.currentTimeMillis();

            // 두번째 정확한 검색
            long start3 = System.currentTimeMillis();
            Page<UserSearchResponse> exactResult2 = userSearchService.searchUsersByNickname(testNickname, 1, 10);
            long end3 = System.currentTimeMillis();

            log.info("정확한 검색 (첫번째): {}ms, 결과: {}건", end1 - start1, exactResult.getTotalElements());
            log.info("LIKE 검색: {}ms, 결과: {}건", end2 - start2, likeResult.getTotalElements());
            log.info("정확한 검색 (두번째): {}ms, 결과: {}건", end3 - start3, exactResult2.getTotalElements());

            // 성능 개선율 계산
            if (end2 - start2 > 0) {
                double improvement = ((double)(end2 - start2) - (end1 - start1)) / (end2 - start2) * 100;
                log.info("성능 개선율: {}%", String.format("%.1f", improvement));
            }

            log.info("=== 종료 ===");
        }
    }
}
