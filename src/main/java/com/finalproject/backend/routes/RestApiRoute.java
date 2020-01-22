package com.finalproject.backend.routes;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;


import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

@Component
public class RestApiRoute extends RouteBuilder {

    private Processor test = (exchange -> {

        copyInputStreamToFile(exchange.getIn().getBody(InputStream.class), new File("test.md"));
    });

    @Override
    public void configure() throws Exception {
        rest("/say")
                .post("/hello").to("direct:hello")
                .get("/bye").consumes("application/json").to("direct:bye")
                .post("/bye").to("mock:update");

        from("direct:hello")
                .log("hello")
                .process(test);

        from("direct:bye")
                .transform().constant("Bye World");
    }
}
