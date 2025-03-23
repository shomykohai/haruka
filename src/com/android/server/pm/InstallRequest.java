package com.android.server.pm;


import android.content.pm.SharedLibraryInfo;
import android.os.UserHandle;
import com.android.server.pm.parsing.pkg.ParsedPackage;

import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;

@DexEdit
final class InstallRequest {
	@DexIgnore private ScanResult mScanResult;
	@PackageManagerService.ScanFlags
	@DexIgnore private int mScanFlags;
	@DexIgnore private int mParseFlags;
	@DexIgnore private int mUserId;
	@DexIgnore private ParsedPackage mParsedPackage;
	
	
	@DexIgnore
	InstallRequest(ParsedPackage parsedPackage, int parseFlags, int scanFlags, UserHandle userHandle, ScanResult scanResult) { throw null; }
	
	/* 
	 * Expose mScanResult since we need it to recreate the InstallRequest object.
	 * We could just set the parsedPackage and call it a day, but I want to be sure
	 * there's no sign of old signatures in the old object
	 * */
	@DexAdd
	public ScanResult getScanResult() {
		return mScanResult;
	}
	
	@DexIgnore
	public int getScanFlags() {
		return mScanFlags;
	}
	
	@DexIgnore
    public UserHandle getUser() {
        return new UserHandle(this.mUserId);
    }
	
	@DexIgnore
    public int getParseFlags() {
        return this.mParseFlags;
    }
	
	@DexIgnore
    public ParsedPackage getParsedPackage() {
        return this.mParsedPackage;
    }
	
	@DexIgnore
	public SharedLibraryInfo getStaticSharedLibraryInfo() { throw null; }

	@DexIgnore
	public PackageSetting getScannedPackageSetting() { throw null; }
	
	@DexIgnore
	public void onReconcileStarted() {}


}
