package com.bravostudiodev.grid;

import org.openqa.grid.internal.utils.DefaultCapabilityMatcher;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author IgorV
 *         Date: 13.2.2017
 *
 *         Default capability matcher considers only 4 capabilities:
 *         PLATFORM, BROWSER_NAME, VERSION, applicationName.
 *         There are capabilities like FirefoxDriver.PROFILE which are used to pass profile settings to the node, these need to be ignored.
 *         Custom capability matcher makes extension capability checks in addition to the basic ones.
 */
@SuppressWarnings("unused")
public class CustomCapabilityMatcher extends DefaultCapabilityMatcher {

    private static final Logger LOGGER = Logger.getLogger(CustomCapabilityMatcher.class.getName());

    @Override
    public boolean matches(Map<String, Object> nodeCapability, Map<String, Object> requestedCapability) {
        boolean basicChecks = super.matches(nodeCapability, requestedCapability);

        boolean customChecks = extensionCapabilityCheck(nodeCapability, requestedCapability);

        return basicChecks && customChecks;
    }

    private boolean extensionCapabilityCheck(Map<String, Object> nodeCapability, Map<String, Object> requestedCapability) {
        for (String capability : requestedCapability.keySet()) {
            if (!capability.startsWith(Capabilities.EXTENSION_PREFIX))
                continue;
            if (!requestedCapability.get(capability).equals(nodeCapability.get(capability))) {
                return false;
            }
        }
        return true;
    }
}
