package com.finalproject.backend.suite;

import com.finalproject.backend.handlers.LambdaEntryPointRouteSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    LambdaEntryPointRouteSuite.class
})
// Test suite to help all tests that require Spring to be run through a single context
// Saves time in unit tests so that they don't each initialise their own context
// Each test suite needs same class annotations and properties to ensure same context is used
// Inclusion of Mock Beans will also cause a new context to be created, avoid where possible
public class RouteTestSuite {
}