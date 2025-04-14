package com.android.server.pm;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.component.ParsedUsesPermission;

import io.github.shomy.haruka.Haruka;

import android.util.Log;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexWrap;

@DexEdit()
final class ReconcilePackageUtils {
	@DexAdd
	final static String PLATFORM_PACKAGE_NAME = "android";
	@DexAdd
	final static String PLATFORM_PACKAGE_SPOOF_PERMISSION = "android.permission.SPOOF_PLATFORM_SIGNATURE";


	/* 
	 * This function is responsible for verifying packages signatures during boot.
	 * Since that when system packages fail verification, the device bootloops,
	 * we wrap the function to allow to spoofing packages that require the
	 * platform key to have the right signature before the actual verification happens.
	 * 
	 * Ideally, we would want to allow every system installed packages to spoof their 
	 * signature (this includes vendor apps that are being modified but aren't signed with platform key),
	 * but I have to find a way to get the expected package signature from SharedUserSettings at this
	 * step during boot.
	 * 
	 * */
	@DexWrap
	public static List<ReconciledPackage> reconcilePackages(
            List<InstallRequest> installRequests,
            Map<String, AndroidPackage> allPackages,
            Map<String, Settings.VersionInfo> versionInfos,
            SharedLibrariesImpl sharedLibraries,
            KeySetManagerService ksms, Settings settings)
            throws ReconcileFailure
    {
		final List<InstallRequest> newInstallRequests = new ArrayList<>(installRequests.size());
		
		ParsedPackage platformPackage = null;
		
		for (InstallRequest installRequest : installRequests) {
			if (installRequest.getParsedPackage().getPackageName() == PLATFORM_PACKAGE_NAME) {
				platformPackage = installRequest.getParsedPackage();
				break;
			}
		}
		
		if (platformPackage == null) {
			// We weren't able to get the platform key programmatically, so we avoid spoofing
			return reconcilePackages(installRequests, allPackages, versionInfos, sharedLibraries, ksms, settings);
		}

		for (InstallRequest installRequest : installRequests) {
			final ParsedPackage parsedPackage = installRequest.getParsedPackage();

			if (canSpoofPlatformSignature(parsedPackage)) {
				parsedPackage.setSigningDetails(platformPackage.getSigningDetails());

				final InstallRequest newInstallRequest = new InstallRequest(
						parsedPackage,
						installRequest.getParseFlags(),
						installRequest.getScanFlags(),
						installRequest.getUser(),
						installRequest.getScanResult()
						);
				
				newInstallRequests.add(newInstallRequest);
				Log.w(Haruka.TAG, "Spoofed platform signature for package " + parsedPackage.getPackageName());
				continue;
			}

			newInstallRequests.add(installRequest);
		}

		return reconcilePackages(newInstallRequests, allPackages, versionInfos, sharedLibraries, ksms, settings);
    }
	
	
	/*
	 * Verifies if a package can spoof its signature to the platform key.
	 * More checks need to be put in place to restrict the signature spoofing
	 * ability only to apps installed into /system, /system_ext and maybe /product and /vendor.
	 * */
	@DexAdd
	private static boolean canSpoofPlatformSignature(ParsedPackage pp) {
		boolean containsPermission = false;
		for (ParsedUsesPermission permission : pp.getUsesPermissions()) {
			if (permission.getName().equals(PLATFORM_PACKAGE_SPOOF_PERMISSION))
			{
				Log.w(Haruka.TAG, "Package " + pp.getPackageName() + " has permission " + permission.getName());
				containsPermission = true;
				break;
			}
		}
		

		// Only allow packages installed into the system partitions to spoof signature. We don't allow this for user-installed packages.
		// Ideally, we would want to have more checks in here, like 
		/* boolean isSystemApp = pp.isSystemExt() || pp.isSystem() || pp.isProduct(); */	
		boolean isSystemApp = pp.isCoreApp();

		return !pp.isSignedWithPlatformKey() && containsPermission && isSystemApp;
	}
}
