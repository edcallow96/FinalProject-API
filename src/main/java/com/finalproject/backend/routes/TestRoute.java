package com.finalproject.backend.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TestRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:test").log("Camel route reached");
    }
}
