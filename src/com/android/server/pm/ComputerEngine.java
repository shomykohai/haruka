package com.android.server.pm;

import java.security.cert.CertificateException;

import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.SigningInfo;
import android.util.Log;
import io.github.shomy.haruka.Haruka;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexWrap;


@DexEdit(contentOnly = true)
public class ComputerEngine {
	@DexAdd
	public static final String FAKE_PACKAGE_SIGNATURE_PERM = "android.permission.FAKE_PACKAGE_SIGNATURE";

	
	
	/*
	 * This method is involved in generating the package signatures, so by wrapping it we can
	 * then implement our own logic in it.
	 * 
	 * The workflow is simple: We let the original function generate the needed stuff, and we later
	 * generate a fake signature if the package requests it, to override the original one
	 * */
	@DexWrap
	public final PackageInfo generatePackageInfo(PackageStateInternal ps, long flags, int userId) {
		PackageInfo pi = generatePackageInfo(ps, flags, userId);
		AndroidPackage pp = ps.getPkg();

		if (pp != null && hasSignatureSpoofing(pp, pi)) {
			pi = spoofSignature(pi, pp, flags);
		}
		
		return pi;
	}
	
	
	/*
	 * This method just checks if a given package has signature spoofing
	 * */
	@DexAdd
	private boolean hasSignatureSpoofing(AndroidPackage ap, PackageInfo pi) {
		// This is important, or android will crash during boot
		if (pi.requestedPermissions == null || pi.requestedPermissionsFlags == null) {
			return false;
		}
		
		// There is surely a better way to check if the permission is present before hand
		boolean hasPermission = false;
		for (int i = 0; i < pi.requestedPermissions.length; i++) {
			if (pi.requestedPermissions[i].equals(FAKE_PACKAGE_SIGNATURE_PERM)) {
				hasPermission = true;
				break;
			}
		}
		
		// We don't log anything for packages without permissions.
		// The for loop above and this check if basically to avoid spamming the logs for every package
		if (!hasPermission) return false;
		
		// Just to make people life easier while debugging, we log eventual missing stuff
		if (ap.getMetaData() == null) {
			Log.w(Haruka.TAG, "Cannot get metadata for package " + ap.getPackageName() + " (metadata == null)");
			return false;
		}
		
		boolean isMetaDataInvalid = ( ap.getMetaData().get("fake-signature") instanceof String ) == false;
		if (isMetaDataInvalid || ap.getMetaData().getString("fake-signature") == null) {
			Log.w(Haruka.TAG, "Fake signature metadata for package " + ap.getPackageName() + " is invalid (not a string, or null).");
			return false;
		}

		return true;
	}
	
	
	/*
	 * Checks whether the user has granted signature spoofing permission. 
	 * I suggest using NanoDroid org.spoofing.apk for having a nice UI for it.
	 * 
	 * This method is mostly based on haystack, as this is the most effective method
	 * to check for user granted permission.
	 * */
	@DexAdd
	private boolean isSpoofingPermissionGranted(PackageInfo pi) {
		if (pi.requestedPermissions.length != pi.requestedPermissionsFlags.length) {
			// This shouldn't happen. We avoid spoofing signature to avoid crashing the system.
			Log.w(Haruka.TAG, "requestedPermissions and requestedPermissionsFlags have different length.");
			Log.w(Haruka.TAG, "Check your app (" + pi.packageName + ") for eventual errors.");
			return false;
		}
		
		for (int i = 0; i < pi.requestedPermissions.length; i++) {
			if (!pi.requestedPermissions[i].equals(FAKE_PACKAGE_SIGNATURE_PERM)) continue;
			
			// Check if the user has granted Signature Spoofing permission.
			// If the permission was granted, this shouldn't be equal to 0.
			boolean hasGranted = ( pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED ) != 0;
			
			if (!hasGranted) Log.w(Haruka.TAG, "The user didn't grant the FAKE_PACKAGE_SIGNATURE permission to " + pi.packageName);
			
			return hasGranted;
		}

		return false;
	}
	
	
	/*
	 * Has the name implies, it just spoofes the package signature by the one declared in the `fake-signature` metadata.
	 * 
	 * This patch supports new changes needed by microG.
	 * */
	@DexAdd
	private PackageInfo spoofSignature(PackageInfo pi, AndroidPackage pp, long flags) {
		if ( (flags & PackageManager.GET_SIGNATURES) == 0) {
			Log.w(Haruka.TAG, "Cannot get signatures for package " + pi.packageName);
			return pi;
		}
		
		if (isSpoofingPermissionGranted(pi) == false) {
			// We don't log here since `isSpoofingPermissionGranted` already does it
			return pi;
		}
		
		
		// We can safely assume this is valid, as it is being checked before this method is called
		String fakeSignature = pp.getMetaData().getString("fake-signature");
		
		
		/*
		 * Here we spoof both the classic signatures and the new SigningDetails fields, to allow compatibility
		 * between old and new apps (microg as an example for the latter, and Lanchon's Signature Spoofing checker for the first)
		 * */
		pi.signatures = new Signature[] { new Signature(fakeSignature) };
		
		try {
			pi.signingInfo = new SigningInfo(
				new SigningDetails(
						pi.signatures,
						SigningDetails.SignatureSchemeVersion.SIGNING_BLOCK_V3,
						SigningDetails.toSigningKeys(pi.signatures),
						null
						)
				);
		}
		catch (CertificateException ce) {
			Log.e(Haruka.TAG, "There was an error while generating SigningInfo for package " + pi.packageName + ", only spoofed legacy signatures.", ce);
		}
		
		
		Log.i(Haruka.TAG, "Spoofed signature for package " + pi.packageName);
		
		return pi;
	}
}
