package com.example.userservice.dto;

import com.example.userservice.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "user", collectionRelation = "users")
@Schema(description = "Данные пользователя, возвращаемые API (с HATEOAS-ссылками)")
public record UserResponse(
        @Schema(description = "Идентификатор пользователя", example = "1")
        Long id,

        @Schema(description = "Имя пользователя", example = "Иван")
        String name,

        @Schema(description = "Email пользователя", example = "ivan@example.com")
        String email,

        @Schema(description = "Возраст пользователя", example = "30")
        Integer age,

        @Schema(description = "Дата и время создания пользователя (ISO-8601)",
                example = "2026-01-01T12:00:00")
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getAge(), user.getCreatedAt());
    }
}