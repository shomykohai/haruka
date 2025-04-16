if [ -z $1 ]; then
    echo "Please pass the services.jar to patch"
    exit 1
fi

rm -rf patch
mkdir -p patch
dexpatcher --multi-dex-threaded --api-level 34 --verbose --debug --output patch $1 build/haruka.dex
mkdir -p patch/services
cp $1 patch/services.jar
cd patch/services
jar xf ../services.jar
rm classes*.dex
cp ../classes*.dex .
jar cvf ../../build/services.jar *
cd ../../
rm -rf patch