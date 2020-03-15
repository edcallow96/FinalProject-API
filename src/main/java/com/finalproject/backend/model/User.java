package com.finalproject.backend.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

import java.io.Serializable;


@Getter
@Setter
@ToString
@DynamoDBTable(tableName = "UserTable")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

  @DynamoDBHashKey
  private String userId;

  @DynamoDBAttribute
  private String emailAddress;

  @DynamoDBAttribute
  private String firstName;

  @DynamoDBAttribute
  private String lastName;

}
