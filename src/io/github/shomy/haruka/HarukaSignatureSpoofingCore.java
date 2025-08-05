package io.github.shomy.haruka;

import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.Map;

import android.content.pm.PackageInfo;
import android.util.ArraySet;
import android.util.Log;
import lanchon.dexpatcher.annotation.DexAdd;

public class HarukaSignatureSpoofingCore {
    @DexAdd
    public static final String FAKE_PACKAGE_SIGNATURE_PERM = "android.permission.FAKE_PACKAGE_SIGNATURE";
    @DexAdd
    public static final String FAKE_PACKAGE_SIGNATURE = "fake-signature";
    @DexAdd
    public static final String NANODROID_SPOOFING = "org.spoofing";
    @DexAdd
    public static final String HARUKA_SPOOFING = "io.github.shomy.haruka.framework";

    /*
     * Checks whether the user has granted signature spoofing permission.
     * I suggest using NanoDroid org.spoofing.apk for having a nice UI for it.
     * 
     * This method is mostly based on haystack, as this is the most effective method
     * to check for user granted permission.
     */
    @DexAdd
    public static boolean isSpoofingPermissionGranted(HarukaPackageWrapper hp) {
        if (hp.getRequestedPermissions().length != hp.getRequestedPermissionsFlags().length) {
            // This shouldn't happen. We avoid spoofing signature to avoid crashing the
            // system.
            Log.w(Haruka.TAG, "requestedPermissions and requestedPermissionsFlags have different length.");
            Log.w(Haruka.TAG, "Check your app (" + hp.getPackageName() + ") for eventual errors.");
            return false;
        }

        for (int i = 0; i < hp.getRequestedPermissions().length; i++) {
            if (!hp.getRequestedPermissions()[i].equals(FAKE_PACKAGE_SIGNATURE_PERM))
                continue;

            // Check if the user has granted Signature Spoofing permission.
            // If the permission was granted, this shouldn't be equal to 0.
            // 0x2 corresponds to PackageInfo.REQUESTED_PERMISSION_GRANTED
            boolean hasGranted = (hp.getRequestedPermissionsFlags()[i] & 0x2) != 0;

            if (!hasGranted)
                Log.w(Haruka.TAG,
                        "The user didn't grant the FAKE_PACKAGE_SIGNATURE permission to " + hp.getPackageName());

            return hasGranted;
        }

        return false;
    }

    /*
     * This method just checks if a given package has signature spoofing
     */
    @DexAdd
    public static boolean hasSignatureSpoofing(HarukaPackageWrapper hp) {
        boolean hasPermission = hp.hasPermission(FAKE_PACKAGE_SIGNATURE_PERM);

        // We don't log anything for packages without permissions.
        // The for loop above and this check if basically to avoid spamming the logs for
        // every package
        if (!hasPermission) {
            return false;
        }
        // Just to make people life easier while debugging, we log eventual missing
        // stuff
        if (hp.getMetaData() == null) {
            // Small check to avoid spamming with NanoDroid org.spoofing apk
            if (!(hp.getPackageName().equals(NANODROID_SPOOFING)) && !(hp.getPackageName().equals(HARUKA_SPOOFING))) {
                Log.w(Haruka.TAG, "Cannot get metadata for package " + hp.getPackageName() + " (metadata == null)");
            }
            return false;
        }

        boolean isMetaDataInvalid = (hp.getMetaData().getString(FAKE_PACKAGE_SIGNATURE) instanceof String) == false;
        if (isMetaDataInvalid || hp.getMetaData().getString(FAKE_PACKAGE_SIGNATURE) == null) {
            Log.w(Haruka.TAG, "Fake signature metadata for package " + hp.getPackageName()
                    + " is invalid (not a string, or null).");
            return false;
        }

        return true;
    }

    /*
     * As the name implies, it just spoofes the package signature by the one
     * declared in the `fake-signature` metadata.
     * 
     * This patch supports new changes needed by microG, as well the Android 9+
     * GET_SIGNING_CERTIFICATES flag (which new apps use).
     */
    @DexAdd
    public static PackageInfo spoofSignature(PackageInfo pi, long flags, HarukaPackageWrapper hp,
            Map<String, Class<?>> classMap) {
        /*
         * 64 = PackageManager.GET_SIGNATURES (Old signatures)
         * 134217728 = PackageManager.GET_SIGNING_CERTIFICATES
         */
        if ((flags & 64) == 0 && (flags & 134217728) == 0) {
            // The app didn't request package signature, thus the fields have to be null
            return pi;
        }

        if (hp.getMetaData() == null) {
            return pi;
        }
        // We can safely assume this is valid, as it is being checked before this method
        // is called
        String fakeSignature = hp.getMetaData().getString(FAKE_PACKAGE_SIGNATURE);

        if (fakeSignature == null || !(fakeSignature instanceof String)) {
            Log.e(Haruka.TAG, "fake-signature metadata is null (" + hp.getPackageName() + ")");
            return pi;
        }

        /*
         * Here we spoof both the classic signatures and the new SigningDetails fields,
         * to allow compatibility
         * between old and new apps (microg as an example for the latter, and Lanchon's
         * Signature Spoofing checker for the first)
         * 
         * The code looks weird because of the use of reflection, as it will be shared
         * between different SDKs versions. It basically does this:
         * 
         * 
         * pi.signatures = new Signature[] { new Signature(fakeSignature) };
         * pi.signingInfo = new SigningInfo(
         * new SigningDetails(
         * pi.signatures,
         * SigningDetails.SignatureSchemeVersion.SIGNING_BLOCK_V3,
         * SigningDetails.toSigningKeys(pi.signatures),
         * null
         * ));
         */

        Class<?> signatureClass = classMap.get("Signature");
        Class<?> signingDetailsClass = classMap.get("SigningDetails");
        Class<?> signingInfoClass = classMap.get("SigningInfo");

        try {
            Object signatureInstance = Reflector.newInstance(signatureClass, fakeSignature);
            Object signaturesArray = Reflector.newArray(signatureClass, 1);
            Array.set(signaturesArray, 0, signatureInstance);

            Reflector.set(pi, "signatures", signaturesArray);

            Object signingDetailsInstance = Reflector.newInstance(signingDetailsClass,
                    signaturesArray,
                    (int) 3);

            Object signingInfoInstance = Reflector.newInstance(signingInfoClass, signingDetailsInstance);

            Reflector.set(pi, "signingInfo", signingInfoInstance);
        } catch (Exception e) {
            Log.e(Haruka.TAG, "There was an error while spoofing signature for package " + pi.packageName, e);

        }

        return pi;
    }
}