package com.finalproject.backend.suite;

import com.finalproject.backend.handlers.FinalProjectLambdaFunctionRouteSuite;
import com.finalproject.backend.handlers.LambdaEntryPointRouteSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    FinalProjectLambdaFunctionRouteSuite.class,
    LambdaEntryPointRouteSuite.class
})
public class RouteTestSuite {
}