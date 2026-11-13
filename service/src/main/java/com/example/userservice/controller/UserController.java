package com.example.userservice.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.example.userservice.dto.UserCreateRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.dto.UserUpdateRequest;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
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
public class UserController {

  private final UserService userService;
  private final UserModelAssembler assembler;

  public UserController(UserService userService, UserModelAssembler assembler) {
    this.userService = userService;
    this.assembler = assembler;
  }

  @GetMapping
  public CollectionModel<EntityModel<UserResponse>> list() {
    List<EntityModel<UserResponse>> models =
        userService.findAll().stream().map(assembler::toModel).toList();
    return CollectionModel.of(models).add(
        linkTo(methodOn(UserController.class).list()).withSelfRel());
  }

  @GetMapping("/{id}")
  public EntityModel<UserResponse> getOne(@PathVariable Long id) {
    return assembler.toModel(userService.findById(id));
  }

  @PostMapping
  public ResponseEntity<EntityModel<UserResponse>>
  create(@Valid @RequestBody UserCreateRequest request) {
    EntityModel<UserResponse> model =
        assembler.toModel(userService.create(request));
    return ResponseEntity
        .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
        .body(model);
  }

  @PutMapping("/{id}")
  public EntityModel<UserResponse>
  update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
    return assembler.toModel(userService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }
}