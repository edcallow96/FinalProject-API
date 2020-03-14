package com.finalproject.backend.suite;

import com.finalproject.backend.handlers.FinalProjectLambdaFunctionSuite;
import com.finalproject.backend.handlers.LambdaEntryPointRouteSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    FinalProjectLambdaFunctionSuite.class,
    LambdaEntryPointRouteSuite.class
})
public class RouteTestSuite {
}