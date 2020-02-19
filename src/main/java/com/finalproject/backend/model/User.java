package com.finalproject.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class User {
  private String userId;

  private String emailAddress;
}
