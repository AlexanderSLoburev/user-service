package com.example.userservice.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.example.userservice.dto.UserResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class UserModelAssembler
    implements RepresentationModelAssembler<UserResponse,
                                            EntityModel<UserResponse>> {

  @Override
  public EntityModel<UserResponse> toModel(UserResponse response) {
    return EntityModel.of(
        response,
        linkTo(methodOn(UserController.class).getOne(response.id()))
            .withSelfRel(),
        linkTo(methodOn(UserController.class).list()).withRel("users"),
        linkTo(methodOn(UserController.class).update(response.id(), null))
            .withRel("update"),
        linkTo(methodOn(UserController.class).delete(response.id()))
            .withRel("delete"));
  }
}