## 遥か (Haruka)

Custom patches for various Android 14 stuff, mostly for `penangf`, with dexpatcher.

Current features:
* Platform Signature spoofing **--** Allows system packages with the `SPOOF_PLATFORM_SIGNATURE` permission to spoof their signature to the platform key (used for patched system apps)
* (Updated) Signature Spoofing for apps **--** Allows all apps that have the `FAKE_PACKAGE_SIGNATURE` permission to spoof their signature to the one definied in their metadata. Updated to support latest changes for SigningInfo needed by microG and new apps. 

## Usage

Get the files you need from your ROM (for now only patches for services.jar are available), and [dexpatcher 1.8.0-beta1](https://github.com/DexPatcher/dexpatcher-tool/releases/tag/v1.8.0-beta1) and put all in the same directory.

Then run 
```
mkdir generated_dex
# This will only apply the signature spoof patch. For full haruka patches, use haruka_0.2.dex
java -jar dexpatcher-1.8.0-beta1.jar --multi-dex-threaded --api-level 34 --verbose --debug --output generated_dex services.jar haruka_0.2_only_sig_spoof.dex
```

After dexpatcher generated the new patched dexes, open services.jar with an archive program (such as 7zip or WinRar), remove all old `.dex` files from it and put the newly generated one inside.
Or use the `patch_services.sh` script like this
```sh
# If not present, create a directory called build and put haruka dex file in there as "haruka.dex"
./patch_services.sh path/to/stock_services.jar
```
And you'll find the patched services.jar inside the build directory.

Now, choose which steps you need to do based on what you'll need the patch for:
* [Building a ROM I will later flash](#building-a-rom-i-will-later-flash)
* [Patching the currently installed rom](#patching-the-currently-installed-rom)


### [Building a ROM I will later flash]

* Copy services.jar to /system/framework (replace if the old one is still present) 
* Run the following commands
```sh
chown 0:0 services.jar
chmod 0644 services.jar
setfattr -n security.selinux -v u:object_r:system_file:s0 services.jar
```
* Copy the patched services.jar to a working android device in the shared storage (e.g. Documents) and run the following command from ADB (Root is not needed)
```sh
# This is needed to generate optimized dexes files, or the device will bootloop
dex2oat64 --dex-file=services.jar --instruction-set=arm64 --oat-file=services.odex
```
* Pull the generated files (make sure they're not 0 bytes in size) into your PC
* Copy them to /system/framework/oat/arm64/
* Run the following commands
```sh
chown 0:0 services.vdex
chmod 0644 services.vdex
setfattr -n security.selinux -v u:object_r:system_file:s0 services.vdex

chown 0:0 services.odex
chmod 0644 services.odex
setfattr -n security.selinux -v u:object_r:system_file:s0 services.odex
```
* Enjoy!

### [Patching the currently installed rom]
* First of all, make sure your device is rooted
* With adb, push services.jar to shared storage (e.g. Documents)
* Run 
```sh
dex2oat64 --dex-file=services.jar --instruction-set=arm64 --oat-file=services.odex
```
* Replace services.jar in /system/framework with the patched one, and the newly generated optimized dexes file to /system/framework/oat/arm64
* Run the following commands on all copied files:
```sh
# Replace X with the extension of the file
chown 0:0 services.X
chmod 0644 services.X
chcon "u:object_r:system_file:s0" services.X
```
* Reboot to recovery and clear cache (Optional, but recommended)
* Reboot to system
* Enjoy!

## License

This project is licensed under the Apache 2.0 license. See [LICENSE](LICENSE).
