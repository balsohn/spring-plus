package org.example.expert.domain.user.repository;

import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // 닉네임으로 일치하는 사용자 검색
    @Query("SELECT u FROM User u WHERE u.nickname = :nickname ORDER BY u.id")
    Page<User> findByNicknameExact(@Param("nickname") String nickname, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.nickname LIKE %:nickname% ORDER BY u.id")
    Page<User> findByNicknameLike(@Param("nickname") String nickname, Pageable pageable);
}
