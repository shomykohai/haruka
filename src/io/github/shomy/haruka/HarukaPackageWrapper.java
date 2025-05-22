package io.github.shomy.haruka;

import java.util.Arrays;

import android.os.Bundle;

/*
 *
 * Helper class to wrap all the needed information of a package,
 * in the hope of making it SDK agnostic.
 * 
*/
public class HarukaPackageWrapper {
    private final String packageName;
    private final String[] requestedPermissions;
    private final int[] requestedPermissionsFlags;
    private final long flags;
    private final Bundle metaData;

    public HarukaPackageWrapper(String packageName, String[] requestedPermissions, int[] requestedPermissionsFlags,
            long flags, Bundle metaData) {
        this.packageName = packageName;
        this.requestedPermissions = requestedPermissions;
        this.requestedPermissionsFlags = requestedPermissionsFlags;
        this.metaData = metaData;
        this.flags = flags;
    }

    public String getPackageName() {
        return packageName;
    }

    public String[] getRequestedPermissions() {
        return requestedPermissions;
    }

    public int[] getRequestedPermissionsFlags() {
        return requestedPermissionsFlags;
    }

    public long getFlags() {
        return flags;
    }

    public Bundle getMetaData() {
        return metaData;
    }

    public boolean hasPermission(String permission) {
        if (requestedPermissions == null || requestedPermissionsFlags == null)
            return false;

        boolean hasPermission = Arrays.asList(this.requestedPermissions).contains(permission);
        return hasPermission;
    }

}