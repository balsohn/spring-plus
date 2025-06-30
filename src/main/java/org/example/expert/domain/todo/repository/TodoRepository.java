package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long>, TodoRepositoryCustom {

    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u ORDER BY t.modifiedAt DESC")
    Page<Todo> findAllByOrderByModifiedAtDesc(Pageable pageable);

    // weather 만으로 검색
    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user WHERE t.weather = :weather ORDER BY t.modifiedAt DESC")
    List<Todo> findByWeather(@Param("weather") String weather);

    // 수정일 기간으로 검색
    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user WHERE t.modifiedAt BETWEEN :startDate AND :endDate ORDER BY t.modifiedAt DESC ")
    List<Todo> findByModifiedAtBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // weather + 기간 검색
    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user WHERE t.weather = :weather AND t.modifiedAt BETWEEN :startDate AND :endDate ORDER BY t.modifiedAt DESC")
    List<Todo> findByWeatherAndModifiedAtBetween(@Param("weather") String weather,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

}
