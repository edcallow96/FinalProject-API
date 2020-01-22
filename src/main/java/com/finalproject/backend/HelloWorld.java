package com.finalproject.backend;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

public class HelloWorld implements RequestHandler<S3Event, String> {
    @Override
    public String handleRequest(S3Event event, Context ctx) {
        System.out.println(event);
        System.out.println(ctx);
        return null;
    }
}