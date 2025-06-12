SKIPUNZIP=1
DALVIKVM="dalvikvm"
DEX2OAT="dex2oat64"
ARCH="arm64"
DEXPATCHER="$MODPATH/patcher/dexpatcher.dex"
ZIPBINARY="$MODPATH/patcher/zip.arm"

spawn_dalvikvm() {
    if [ ! "$BOOTMODE" ]; then
        ui_print "We are in recovery. Still not supported"
        abort
    fi

    "$DALVIKVM" -Xnoimage-dex2oat "${@}"
}


check_for_fake_package_perm() {
    if [ ! $BOOTMODE ]; then
        ui_print "We are in recovery. Still not supported"
        abort
    fi

    mkdir -p $TMPDIR/unpacked
    unzip -o $MODPATH/haruka/services.jar "classes*.dex" -d $TMPDIR/unpacked/ >> /data/adb/haruka.log 2>&1
    ls $TMPDIR/unpacked >> /data/adb/haruka.log 2>&1
    if strings $TMPDIR/unpacked/classes*.dex | grep -qF "android.permission.FAKE_PACKAGE_SIGNATURE"; then
        abort "The rom you're using already has signature spoofing. Aborting"
    fi
}

setup_env() {
    if [ ! $BOOTMODE ]; then
        ui_print "We are in recovery. Still not supported"
        abort
    fi

    rm /data/adb/haruka.log

    mkdir -p $MODPATH/haruka
    mkdir -p $MODPATH/haruka/patch
}

call_dexpatcher() {
    # $1 = file to patch
    # $2 = output folder
    # $3 = patch to apply
    local input_file="$1"
    local output_folder="$2"
    shift 2

    spawn_dalvikvm -classpath "$DEXPATCHER" lanchon.dexpatcher.Main \
        --debug --verbose --multi-dex-threaded --api-level "$API" \
        --output "$output_folder" "$input_file" "$@"
}


haruka_set_permissions() {
    chown 0:0 ${1}
    chmod 0644 ${1}
    chcon "u:object_r:system_file:s0" ${1}
}



######################################################################
#                                                                    #
# Actual section that gets executed. This is where the patch occurs  #
#                                                                    #
######################################################################
if [ $API -lt 33 ]; then abort "Haruka can only patch devices with A13 or higher" 
fi


if [ $BOOTMODE == true ]; then
    ui_print "Running in Magisk/KSU"
    
    if [ ! $IS64BIT ]; then
        DEX2OAT="dex2oat"
        ARCH="arm"
    fi

    ui_print "Current arch is ${ARCH}"
    setup_env

    ui_print "Copying necessary files"

    cp /system/framework/services.jar $MODPATH/haruka/services.jar
    # cp /storage/emulated/0/Music/services.jar $MODPATH/haruka/services.jar

    ui_print "Running checks before continuing"
    check_for_fake_package_perm

    ui_print "1st check passed. Continuing"
    unzip -o "$ZIPFILE" "module.prop" "patcher/*" -d "$MODPATH" >> /data/adb/haruka.log 2>&1

    ui_print "Checking for SDK compatibility"
    ui_print "System SDK is $API"

    core_patch="$MODPATH/patcher/haruka_core.dex" 
    current_version_patch="$MODPATH/patcher/haruka_${API}.dex"
    if [ ! -f "$current_version_patch" ]; then
        abort "No patch yet available for SDK $API"
    fi

    ui_print "2nd check passed. Continuing"

    patches="$core_patch $current_version_patch"
    mkdir -p $MODPATH/haruka/patch
    ui_print "Injecting haruka"
    call_dexpatcher $MODPATH/haruka/services.jar $MODPATH/haruka/patch $patches >> /data/adb/haruka.log 2>&1
    cp -r $MODPATH/haruka/patch $MODPATH/

    # Don't use quotes here, or the zip command will fail for some reason
    "${ZIPBINARY}" -d "$MODPATH/haruka/services.jar" classes*.dex >> /data/adb/haruka.log 2>&1
    "${ZIPBINARY}" -j "$MODPATH/haruka/services.jar" $MODPATH/haruka/patch/classes*.dex >> /data/adb/haruka.log 2>&1

    ui_print "Injection completed"

    ui_print "Optimizing dex files"
    START_TIME=$(date '+%m-%d %H:%M:%S.%3N')
    "$DEX2OAT" --dex-file=$MODPATH/haruka/services.jar --instruction-set=$ARCH --oat-file="$MODPATH/haruka/patch/services.odex" --dump-timings --dump-stats --dump-pass-timings --abort-on-hard-verifier-error --abort-on-soft-verifier-error || abort "Failed to optimize dex files"
    logcat -d -v time | awk -v start="$START_TIME" '$0 > start' | grep -i $DEX2OAT >> /data/adb/haruka.log 2>&1

    ui_print "Done optimizing dex files"

    ui_print "Installing modded services.jar"
    mkdir -p $MODPATH/system/framework
    mkdir -p $MODPATH/system/framework/oat/$ARCH/

    cp $MODPATH/haruka/services.jar $MODPATH/system/framework/services.jar
    cp $MODPATH/haruka/patch/services.odex $MODPATH/system/framework/oat/$ARCH/services.odex
    cp $MODPATH/haruka/patch/services.vdex $MODPATH/system/framework/oat/$ARCH/services.vdex
    cp "$MODPATH/patcher/haruka-framework.apk" $MODPATH/system/framework/

    ui_print "Setting permissions"
    haruka_set_permissions $MODPATH/system/framework/services.jar
    haruka_set_permissions $MODPATH/system/framework/oat
    haruka_set_permissions $MODPATH/system/framework/oat/$ARCH
    haruka_set_permissions $MODPATH/system/framework/oat/$ARCH/services.odex
    haruka_set_permissions $MODPATH/system/framework/oat/$ARCH/services.vdex
    haruka_set_permissions $MODPATH/system/framework/haruka-framework.apk


    ui_print "Cleaning up"

    rm -rf $MODPATH/haruka
    rm -rf $MODPATH/patcher
    rm -rf $MODPATH/patch # Just in case
    rm /data/adb/haruka.log # Remove the log file if we succeded 

    ui_print "All done!"

else
    ui_print "Running in Recovery. Not yet supported"
    abort
    # setup_env
    unzip -o "$ZIPFILE" "module.prop" -d "$TMPDIR/haruka"
    ui_print $(tree $TMPDIR)
fi
