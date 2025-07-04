package org.example.expert.domain.user.service;

import com.amazonaws.services.s3.transfer.internal.future.CompletedFuture;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Test
    @Transactional
    @Rollback(value = false)
    void 병렬_처리_100만건_생성_테스트() {
        log.info("=== 병렬 처리로 100만건 데이터 생성 시작 ===");

        int totalUsers = 1000000;  // 100만건
        int batchSize = 10000;     // 배치 크기 증가
        int batches = totalUsers / batchSize;
        int threadCount = 5;       // 스레드 수

        long totalStartTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < batches; i++) {
            final int batchIndex = i;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    long batchStartTime = System.currentTimeMillis();

                    List<User> users = new ArrayList<>();

                    for (int j = 0; j < batchSize; j++) {
                        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                        String nickname = "parallel_" + uniqueId;
                        String email = "parallel" + (batchIndex * batchSize + j) + "@example.com";

                        User user = new User(email, "password123", UserRole.USER, nickname);
                        users.add(user);
                    }

                    // 각 스레드에서 별도의 트랜잭션으로 저장
                    saveUsersInNewTransaction(users);

                    long batchEndTime = System.currentTimeMillis();

                    if ((batchIndex + 1) % 10 == 0) {
                        log.info("병렬 배치 진행: {}/{} 완료, 배치 소요시간: {}ms",
                                batchIndex + 1, batches, batchEndTime - batchStartTime);
                    }

                } catch (Exception e) {
                    log.error("배치 {} 처리 중 오류: {}", batchIndex, e.getMessage());
                }
            }, executor);

            futures.add(future);
        }

        // 모든 배치 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long totalEndTime = System.currentTimeMillis();

        // 결과 확인
        long finalCount = userRepository.count();
        log.info("100만건 병렬 처리 완료!");
        log.info("총 소요시간: {}ms ({}분 {}초)",
                totalEndTime - totalStartTime,
                (totalEndTime - totalStartTime) / 60000,
                ((totalEndTime - totalStartTime) % 60000) / 1000);
        log.info("전체 사용자 수: {}", finalCount);
        log.info("평균 배치 처리 시간: {}ms", (totalEndTime - totalStartTime) / batches);
        log.info("초당 처리량: 약 {} 건/초", totalUsers * 1000L / (totalEndTime - totalStartTime));

        executor.shutdown();
        log.info("=== 병렬 데이터 생성 완료 ===");
    }

    @Test
    void 캐시_성능_테스트() {
        log.info("==== 캐시 성능 테스트 시작 ====");

        // 전체 데이터 수 확인
        long totalUsers = userRepository.count();
        log.info("현재 전체 사용자 수: {}", totalUsers);

        if (totalUsers < 100000) {
            log.warn("테스트용 데이터가 부족합니다.");
            return;
        }

        // 테스트용 닉네임 선택
        List<User> sampleUsers = userRepository.findAll().stream().limit(3).toList();

        for (User user : sampleUsers) {
            String testNickname = user.getNickname();
            log.info("\n --- 캐시 테스트 닉네임: {} ---", testNickname);

            // 첫번째 검색 (캐시미스)
            long start1 = System.currentTimeMillis();
            Page<UserSearchResponse> result1 = userSearchService.searchUsersByNickname(testNickname, 1, 10);
            long end1 = System.currentTimeMillis();
            log.info("첫 번째 검색 (캐시 미스): {}ms, 결과: {} 건", end1 - start1, result1.getTotalElements());

            // 두번째 검색 (캐시 히트)
            long start2 = System.currentTimeMillis();
            Page<UserSearchResponse> result2 = userSearchService.searchUsersByNickname(testNickname, 1, 10);
            long end2 = System.currentTimeMillis();
            log.info("두 번째 검색 (캐시 히트): {}ms, 결과: {} 건", end2 - start2, result2.getTotalElements());

            // 세번째 검색 (캐시 히트)
            long start3 = System.currentTimeMillis();
            Page<UserSearchResponse> result3 = userSearchService.searchUsersByNickname(testNickname, 1, 10);
            long end3 = System.currentTimeMillis();
            log.info("세 번째 검색 (캐시 히트): {}ms, 결과: {} 건", end3 - start3, result3.getTotalElements());

            // 캐시 효과 계산
            if (end1 - start1 > 0) {
                double cacheImprovement = ((double)(end1 - start1) - (end2 - start2)) / (end1 - start1) * 100;
                log.info("캐시 성능 개선율: {}%", String.format("%.1f", cacheImprovement));
            }
        }

        log.info("=== 캐시 테스트 완료 ===");
    }

    @Test
    void 최종_성능_종합_비교_테스트() {
        log.info("==== 최종 성능 종합 비교 테스트 시작 ====");

        long totalUsers = userRepository.count();
        log.info("전체 사용자 수: {}", totalUsers);

        if (totalUsers < 100000) {
            log.warn("충분한 데이터가 없습니다.");
            return;
        }

        // 다양한 패턴으로 검색 성능 테스트
        List<User> testUsers = userRepository.findAll().stream().limit(5).toList();

        log.info("\n=== 성능 비교 결과 ===");
        log.info("검색방법\t\t\t평균시간(ms)\t최대시간(ms)\t최소시간(ms)");
        log.info("─".repeat(70));

        // 각 검색 방법별 성능 측정
        long[] exactTimes = new long[testUsers.size()];
        long[] likeTimes = new long[testUsers.size()];
        long[] cachedTimes = new long[testUsers.size()];

        for (int i = 0; i < testUsers.size(); i++) {
            User user = testUsers.get(i);
            String nickname = user.getNickname();
            String partialNickname = nickname.substring(0, Math.min(8, nickname.length()));

            // 정확한 검색 (첫 번째 - 캐시 미스)
            long start = System.currentTimeMillis();
            userSearchService.searchUsersByNickname(nickname, 1, 10);
            exactTimes[i] = System.currentTimeMillis() - start;

            // LIKE 검색
            start = System.currentTimeMillis();
            userSearchService.searchUsersByNicknameLike(partialNickname, 1, 10);
            likeTimes[i] = System.currentTimeMillis() - start;

            // 캐시된 검색 (두 번째 호출)
            start = System.currentTimeMillis();
            userSearchService.searchUsersByNickname(nickname, 1, 10);
            cachedTimes[i] = System.currentTimeMillis() - start;
        }

        // 통계 계산 및 출력
        printPerformanceStats("정확한 검색 (인덱스)", exactTimes);
        printPerformanceStats("LIKE 검색 (풀스캔)", likeTimes);
        printPerformanceStats("캐시된 검색", cachedTimes);

        log.info("─".repeat(70));
        log.info("=== 최종 성능 테스트 완료 ===");
    }

    @Test
    void 동시_접속자_성능_테스트() {
        log.info("=== 동시 접속자 성능 테스트 시작 ===");

        if (userRepository.count() < 100000) {
            log.warn("충분한 데이터가 없습니다.");
            return;
        }

        // 테스트용 닉네임 준비
        String testNickname = userRepository.findAll().stream()
                .findFirst()
                .map(User::getNickname)
                .orElse("testuser_12345");

        // 10명 동시 접속 시뮬레이션
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                long start = System.currentTimeMillis();
                userSearchService.searchUsersByNickname(testNickname, 1, 10);
                return System.currentTimeMillis() - start;
            }, executor));
        }

        List<Long> times = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long testEndTime = System.currentTimeMillis();

        double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0);

        log.info("동시 10명 접속 테스트 결과:");
        log.info("전체 소요시간: {}ms", testEndTime - testStartTime);
        log.info("평균 응답시간: {}ms", String.format("%.1f", avgTime));
        log.info("최대 응답시간: {}ms", maxTime);
        log.info("최소 응답시간: {}ms", minTime);

        executor.shutdown();
        log.info("=== 동시 접속자 테스트 완료 ===");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveUsersInNewTransaction(List<User> users) {
        userRepository.saveAll(users);
    }

    private void printPerformanceStats(String method, long[] times) {
        if (times.length == 0) return;

        long sum = 0, max = times[0], min = times[0];
        for (long time : times) {
            sum += time;
            max = Math.max(max, time);
            min = Math.min(min, time);
        }

        double avg = (double) sum / times.length;
        log.info("{}\t\t{}\t\t{}\t\t{}", method,
                String.format("%.1f", avg), max, min);
    }
}
