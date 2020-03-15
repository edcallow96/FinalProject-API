package com.finalproject.backend.suite;

import com.finalproject.backend.antivirus.AntiVirusScanningRouteRouteSuite;
import com.finalproject.backend.archive.HandleArchiveRouteSuite;
import com.finalproject.backend.fileidentification.FileIdentificationRouteSuite;
import com.finalproject.backend.handlers.FinalProjectLambdaFunctionSuite;
import com.finalproject.backend.handlers.LambdaEntryPointRouteSuite;
import com.finalproject.backend.notification.NotificationRouteSuite;
import com.finalproject.backend.threatremoval.ThreatRemovalRouteSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    FinalProjectLambdaFunctionSuite.class,
    LambdaEntryPointRouteSuite.class,
    AntiVirusScanningRouteRouteSuite.class,
    ThreatRemovalRouteSuite.class,
    FileIdentificationRouteSuite.class,
    HandleArchiveRouteSuite.class,
    NotificationRouteSuite.class
})
public class RouteTestSuite {
}