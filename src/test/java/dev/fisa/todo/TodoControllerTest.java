package dev.fisa.todo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TodoService todoService;

    @Test
    void findAll_returnsOk() throws Exception {
        Todo todo = Todo.builder().title("할일").completed(false).build();
        given(todoService.findAll()).willReturn(List.of(todo));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("할일"));
    }

    @Test
    void findById_returnsOk() throws Exception {
        Todo todo = Todo.builder().title("할일").completed(false).build();
        given(todoService.findById(1L)).willReturn(todo);

        mockMvc.perform(get("/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("할일"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        Todo todo = Todo.builder().title("새 할일").completed(false).build();
        given(todoService.create(any(TodoRequest.class))).willReturn(todo);

        String body = objectMapper.writeValueAsString(new TodoRequest());

        mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("새 할일"));
    }

    @Test
    void update_returnsOk() throws Exception {
        Todo todo = Todo.builder().title("수정됨").completed(true).build();
        given(todoService.update(eq(1L), any(TodoRequest.class))).willReturn(todo);

        String body = objectMapper.writeValueAsString(new TodoRequest());

        mockMvc.perform(put("/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정됨"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        willDoNothing().given(todoService).delete(1L);

        mockMvc.perform(delete("/1"))
                .andExpect(status().isNoContent());
    }
}
