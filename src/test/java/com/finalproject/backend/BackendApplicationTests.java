package com.finalproject.backend;

import org.apache.camel.CamelContext;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@SpringBootTest
public class BackendApplicationTests {

  @Autowired
  private CamelContext camelContext;

  @Test
  public void contextLoads() {
    assertThat(camelContext, notNullValue());
  }

}
