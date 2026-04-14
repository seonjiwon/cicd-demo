package dev.fisa.todo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TodoTest {

    @Test
    void builder_createsTodo() {
        Todo todo = Todo.builder()
                .title("테스트")
                .completed(false)
                .build();

        assertThat(todo.getTitle()).isEqualTo("테스트");
        assertThat(todo.isCompleted()).isFalse();
    }

    @Test
    void update_changesFields() {
        Todo todo = Todo.builder()
                .title("원래")
                .completed(false)
                .build();

        todo.update("수정됨", true);

        assertThat(todo.getTitle()).isEqualTo("수정됨");
        assertThat(todo.isCompleted()).isTrue();
    }
}
