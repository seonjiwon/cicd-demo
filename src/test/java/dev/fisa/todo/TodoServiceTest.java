package dev.fisa.todo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @InjectMocks
    private TodoService todoService;

    @Mock
    private TodoRepository todoRepository;

    @Test
    void findAll_returnsList() {
        Todo todo = Todo.builder().title("테스트").completed(false).build();
        given(todoRepository.findAll()).willReturn(List.of(todo));

        List<Todo> result = todoService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트");
    }

    @Test
    void findById_found() {
        Todo todo = Todo.builder().title("테스트").completed(false).build();
        given(todoRepository.findById(1L)).willReturn(Optional.of(todo));

        Todo result = todoService.findById(1L);

        assertThat(result.getTitle()).isEqualTo("테스트");
    }

    @Test
    void findById_notFound_throwsException() {
        given(todoRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.findById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    void create_savesAndReturns() {
        TodoRequest request = new TodoRequest();
        Todo todo = Todo.builder().title("새 할일").completed(false).build();
        given(todoRepository.save(any(Todo.class))).willReturn(todo);

        Todo result = todoService.create(request);

        assertThat(result).isNotNull();
        then(todoRepository).should().save(any(Todo.class));
    }

    @Test
    void update_modifiesExisting() {
        Todo todo = Todo.builder().title("원래").completed(false).build();
        given(todoRepository.findById(1L)).willReturn(Optional.of(todo));

        TodoRequest request = new TodoRequest();
        // request의 title은 null, completed는 false (기본값)
        Todo result = todoService.update(1L, request);

        assertThat(result).isNotNull();
    }

    @Test
    void delete_callsRepository() {
        todoService.delete(1L);

        then(todoRepository).should().deleteById(1L);
    }
}
