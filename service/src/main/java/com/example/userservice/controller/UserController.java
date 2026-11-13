package com.example.userservice.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.example.userservice.dto.UserCreateRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.dto.UserUpdateRequest;
import com.example.userservice.service.UserService;
import com.example.userservice.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "CRUD-операции над пользователями")
public class UserController {

    private final UserService userService;
    private final UserModelAssembler assembler;

    public UserController(UserService userService, UserModelAssembler assembler) {
        this.userService = userService;
        this.assembler = assembler;
    }

    @GetMapping
    @Operation(
            summary = "Список всех пользователей",
            description = "Возвращает всех пользователей, отсортированных по id по возрастанию. "
                    + "Каждый элемент содержит HATEOAS-ссылки self/update/delete/users.")
    @ApiResponse(
            responseCode = "200",
            description = "Список пользователей",
            content = @Content(schema = @Schema(implementation = CollectionModel.class)))
    public CollectionModel<EntityModel<UserResponse>> list() {
        List<EntityModel<UserResponse>> models =
                userService.findAll().stream().map(assembler::toModel).toList();
        return CollectionModel.of(models).add(
                linkTo(methodOn(UserController.class).list()).withSelfRel());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить пользователя по id",
            description = "Возвращает одного пользователя по идентификатору с HATEOAS-ссылками.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь найден",
                    content = @Content(schema = @Schema(implementation = EntityModel.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с таким id не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public EntityModel<UserResponse> getOne(
            @Parameter(description = "Идентификатор пользователя", example = "1")
            @PathVariable Long id) {
        return assembler.toModel(userService.findById(id));
    }

    @PostMapping
    @Operation(
            summary = "Создать пользователя",
            description = "Создаёт нового пользователя. Email должен быть уникальным. "
                    + "В ответе заголовок Location указывает на созданный ресурс.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь создан",
                    content = @Content(schema = @Schema(implementation = EntityModel.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидное тело запроса",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пользователь с таким email уже существует",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EntityModel<UserResponse>> create(
            @RequestBody @Valid UserCreateRequest request) {
        EntityModel<UserResponse> model =
                assembler.toModel(userService.create(request));
        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Обновить пользователя",
            description = "Обновляет имя, email и возраст пользователя. "
                    + "Email должен оставаться уникальным среди других пользователей.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь обновлён",
                    content = @Content(schema = @Schema(implementation = EntityModel.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидное тело запроса",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с таким id не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email уже занят другим пользователем",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public EntityModel<UserResponse> update(
            @Parameter(description = "Идентификатор пользователя", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid UserUpdateRequest request) {
        return assembler.toModel(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя по id. Публикует событие DELETED в Kafka.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пользователь удалён"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с таким id не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Идентификатор пользователя", example = "1")
            @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}