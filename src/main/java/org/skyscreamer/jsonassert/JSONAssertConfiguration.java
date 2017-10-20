package org.skyscreamer.jsonassert;

public final class JSONAssertConfiguration {

    private static boolean diffViewerEnabled;

    private JSONAssertConfiguration() {
    }

    public static void setDiffViewerEnabled(boolean enabled) {
        JSONAssertConfiguration.diffViewerEnabled = enabled;
    }

    public static boolean isDiffViewerEnabled() {
        return diffViewerEnabled;
    }
}
