package org.example.expert.domain.todo.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final WeatherClient weatherClient;

    @Transactional
    public TodoSaveResponse saveTodo(AuthUser authUser, TodoSaveRequest todoSaveRequest) {
        User user = User.fromAuthUser(authUser);

        String weather = weatherClient.getTodayWeather();

        Todo newTodo = new Todo(
                todoSaveRequest.getTitle(),
                todoSaveRequest.getContents(),
                weather,
                user
        );
        Todo savedTodo = todoRepository.save(newTodo);

        return new TodoSaveResponse(
                savedTodo.getId(),
                savedTodo.getTitle(),
                savedTodo.getContents(),
                weather,
                new UserResponse(user.getId(), user.getEmail())
        );
    }

    public Page<TodoResponse> getTodos(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Todo> todos = todoRepository.findAllByOrderByModifiedAtDesc(pageable);

        return todos.map(todo -> new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(todo.getUser().getId(), todo.getUser().getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        ));
    }

    public TodoResponse getTodo(long todoId) {
        Todo todo = todoRepository.findByIdWithUser(todoId)
                .orElseThrow(() -> new InvalidRequestException("Todo not found"));

        User user = todo.getUser();

        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(user.getId(), user.getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        );
    }

    public List<TodoResponse> searchTodos(String weather, LocalDateTime startDate, LocalDateTime endDate) {
        List<Todo> todos;

        // 조건에 따라 다른 쿼리 호출
        if (weather != null && startDate != null && endDate != null) {
            // weather + 기간 검색
             todos = todoRepository.findByWeatherAndModifiedAtBetween(weather, startDate, endDate);
        } else if (weather != null) {
            // weather 만 검색
            todos = todoRepository.findByWeather(weather);
        } else if (startDate != null && endDate != null) {
            // 기간만 검색
            todos = todoRepository.findByModifiedAtBetween(startDate, endDate);
        } else {
            // 조건 없으면 전체 조회
            todos = todoRepository.findAllByOrderByModifiedAtDesc(PageRequest.of(0, 100)).getContent();
        }

        // Todo -> TodoResponse 변환
        return todos.stream()
                .map(todo -> new TodoResponse(
                        todo.getId(),
                        todo.getTitle(),
                        todo.getContents(),
                        todo.getWeather(),
                        new UserResponse(todo.getUser().getId(), todo.getUser().getEmail()),
                        todo.getCreatedAt(),
                        todo.getModifiedAt()
                ))
                .collect(Collectors.toList());
    }

    public Page<TodoSearchResponse> searchTodosWithProjections(TodoSearchRequest searchRequest, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        return todoRepository.searchTodos(searchRequest, pageable);
    }
}
