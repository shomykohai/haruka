package com.android.server.pm;

import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

import com.android.server.pm.parsing.pkg.AndroidPackageInternal;
import com.android.server.pm.pkg.PackageStateInternal;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.SigningDetails;
import android.content.pm.SigningInfo;
import io.github.shomy.haruka.HarukaPackageWrapper;
import io.github.shomy.haruka.HarukaSignatureSpoofingCore;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexWrap;

@DexEdit(contentOnly = true)
public class ComputerEngine {
    @DexAdd
    public static final String FAKE_PACKAGE_SIGNATURE_PERM = "android.permission.FAKE_PACKAGE_SIGNATURE";

    /*
     * This method is involved in generating the package signatures, so by wrapping
     * it we can
     * then implement our own logic in it.
     * 
     * The workflow is simple: We let the original function generate the needed
     * stuff, and we later
     * generate a fake signature if the package requests it, to override the
     * original one
     */
    @DexWrap
    public final PackageInfo generatePackageInfo(PackageStateInternal ps, long flags, int userId) {
        PackageInfo pi = generatePackageInfo(ps, flags | PackageManager.GET_PERMISSIONS, userId);

        // This is a necessary base case, and also saves us some resources
        if (pi == null || ps == null)
            return pi;

        AndroidPackageInternal pp = ps.getPkg();

        if (pp == null) {
            return pi;
        }

        HarukaPackageWrapper hp = new HarukaPackageWrapper(
                pp.getPackageName(),
                pi.requestedPermissions,
                pi.requestedPermissionsFlags,
                flags,
                pp.getMetaData());

        if (HarukaSignatureSpoofingCore.hasSignatureSpoofing(hp)
                && HarukaSignatureSpoofingCore.isSpoofingPermissionGranted(hp)) {
            @SuppressWarnings("unchecked")
            Map<String, Class<?>> classMap = new HashMap<>();
            classMap.put("SigningDetails", SigningDetails.class);
            classMap.put("SigningInfo", SigningInfo.class);
            classMap.put("Signature", Signature.class);
            pi = HarukaSignatureSpoofingCore.spoofSignature(pi, flags, hp, classMap);
        }

        /*
         * NOTE: Why this code?
         * This step is really important!
         * Above, we used generatePackageInfo to force get the permissions from the
         * package,
         * since we check them in `hasSignatureSpoofing`.
         * If we don't force them back, there might be an unexpected issue, such as an
         * app
         * verifying back the flags.
         * 
         * This is taken from haystack, but it took me a while to figure out why it was
         * there.
         */
        if ((flags & PackageManager.GET_PERMISSIONS) == 0) {
            pi.permissions = null;
            pi.requestedPermissions = null;
            pi.requestedPermissionsFlags = null;
        }

        return pi;
    }

}
