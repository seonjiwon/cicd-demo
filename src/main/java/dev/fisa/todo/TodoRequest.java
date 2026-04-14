package dev.fisa.todo;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TodoRequest {
    private String title;
    private boolean completed;
}
