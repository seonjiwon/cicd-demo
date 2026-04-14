package dev.fisa.todo;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private boolean completed;

    @Builder
    public Todo(String title, boolean completed) {
        this.title = title;
        this.completed = completed;
    }

    public void update(String title, boolean completed) {
        this.title = title;
        this.completed = completed;
    }
}
