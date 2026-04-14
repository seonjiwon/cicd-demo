package dev.fisa.todo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    public List<Todo> findAll() {
        return todoRepository.findAll();
    }

    public Todo findById(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Todo1 not found: " + id));
    }

    @Transactional
    public Todo create(TodoRequest request) {
        Todo todo = Todo.builder()
                .title(request.getTitle())
                .completed(request.isCompleted())
                .build();
        return todoRepository.save(todo);
    }

    @Transactional
    public Todo update(Long id, TodoRequest request) {
        Todo todo = findById(id);
        todo.update(request.getTitle(), request.isCompleted());
        return todo;
    }

    @Transactional
    public void delete(Long id) {
        todoRepository.deleteById(id);
    }
}
