package org.example.expert.domain.todo.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.entity.QComment;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TodoRepositoryImpl implements TodoRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        QTodo qTodo = QTodo.todo;
        QUser qUser = QUser.user;

        Todo result = jpaQueryFactory
                .selectFrom(qTodo)
                .leftJoin(qTodo.user, qUser).fetchJoin()
                .where(qTodo.id.eq(todoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(TodoSearchRequest searchRequest, Pageable pageable) {
        QTodo qTodo = QTodo.todo;
        QManager qManager = QManager.manager;
        QComment qComment = QComment.comment;

        // 동적 조건 생성
        BooleanBuilder builder = new BooleanBuilder();

        // 제목 부분 검색
        if (StringUtils.hasText(searchRequest.getTitle())) {
            builder.and(qTodo.title.containsIgnoreCase(searchRequest.getTitle()));
        }

        // 생성일 범위 검색
        if (searchRequest.getStartDate() != null && searchRequest.getEndDate() != null) {
            builder.and(qTodo.createdAt.between(searchRequest.getStartDate(), searchRequest.getEndDate()));
        } else if (searchRequest.getStartDate() != null) {
            builder.and(qTodo.createdAt.goe(searchRequest.getStartDate()));
        } else if (searchRequest.getEndDate() != null) {
            builder.and(qTodo.createdAt.loe(searchRequest.getEndDate()));
        }

        // 담당자 닉네임 부분 검색
        if (StringUtils.hasText(searchRequest.getManagerNickname())) {
            builder.and(qManager.user.nickname.containsIgnoreCase(searchRequest.getManagerNickname()));
        }

        // 메인 쿼리 - Projections 사용
        JPAQuery<TodoSearchResponse> query = jpaQueryFactory
                .select(Projections.constructor(TodoSearchResponse.class,
                        qTodo.title,
                        qManager.countDistinct().as("managerCount"),
                        qComment.countDistinct().as("commentCount")
                ))
                .from(qTodo);

        // 담당자 닉네임 검색이 있으면
        if (StringUtils.hasText(searchRequest.getManagerNickname())) {
            query.join(qTodo.managers, qManager);
        } else {
            query.leftJoin(qTodo.managers, qManager);
        }

        query.leftJoin(qTodo.comments, qComment)
                .where(builder)
                .groupBy(qTodo.id, qTodo.title)
                .orderBy(qTodo.createdAt.desc());

        // 카운트 쿼리
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qTodo.countDistinct())
                .from(qTodo);

        // 담당자 닉네임 검색이 있으면
        if (StringUtils.hasText(searchRequest.getManagerNickname())) {
            countQuery.join(qTodo.managers, qManager);
        } else {
            countQuery.leftJoin(qTodo.managers, qManager);
        }

        countQuery.where(builder);

        // 페이징 적용
        List<TodoSearchResponse> results = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = countQuery.fetchOne();
        if (total == null) total = 0L;

        return new PageImpl<>(results, pageable, total);

    }

}
