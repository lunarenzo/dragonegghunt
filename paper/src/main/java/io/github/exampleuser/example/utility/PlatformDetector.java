package io.github.exampleuser.example.utility;

/**
 * Utility to detect the current platform at runtime.
 */
public final class PlatformDetector {
    public static final boolean IS_FOLIA;

    static {
        IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
