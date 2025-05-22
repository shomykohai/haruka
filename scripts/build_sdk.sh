#!/bin/bash

SDK=""
JAR_FILES=()

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --sdk)
      if [[ -z "$2" || "$2" == --* ]]; then
        echo "Error: --sdk requires a value."
        exit 1
      fi
      SDK="$2"
      shift 2
      ;;
    *.jar)
      JAR_FILES+=("$1")
      shift
      ;;
    *)
      echo "Error: Unknown or invalid argument '$1'."
      exit 1
      ;;
  esac
done

if [[ -z "$SDK" ]]; then
  echo "Error: --sdk argument is required."
  exit 1
fi

if [[ "${#JAR_FILES[@]}" -lt 2 ]]; then
  echo "Error: You must pass services.jar and framework.jar"
  echo "Usage: $0 services.jar framework.jar"
  exit 1
fi


rm -rf tmp
mkdir tmp

echo $JAR_FILEs

count=1
for jar in "${JAR_FILES[@]}"; do
  if [[ ! -f "$jar" ]]; then
    echo "Error: File '$jar' does not exist."
    exit 2
  fi

  folder_name="$count"
  echo "Unzipping $(basename "$jar") into folder '$folder_name'..."
  mkdir -p "tmp/$folder_name"
  unzip -q "$jar" -d "tmp/$folder_name"
  ((count++))
done


for dir in tmp/*; do
  if [[ -d "$dir" ]]; then
    echo "Processing directory: $dir"
    
    # Step 1: Run d2j-dex2jar on each classes*.dex file
    for dex in "$dir"/classes*.dex; do
      if [[ -f "$dex" ]]; then
        echo "  Converting $(basename "$dex") to JAR..."
        d2j-dex2jar -f -o "${dex%.dex}.jar" "$dex"
      fi
    done

    # Step 2: Extract all resulting .jar files
    extract_dir="$dir/extracted"
    mkdir -p "$extract_dir"

    for jarfile in "$dir"/classes*.jar; do
      if [[ -f "$jarfile" ]]; then
        echo "  Extracting $(basename "$jarfile")..."
        unzip -oq "$jarfile" -d "$extract_dir"
      fi
    done
  fi
done

# Step 3: Merge all extracted contents into a single directory
final_merge_dir="tmp/merged"
mkdir -p "$final_merge_dir"

for extract_dir in tmp/*/extracted; do
  if [[ -d "$extract_dir" ]]; then
    echo "Merging from $extract_dir into final directory..."
    cp -r "$extract_dir/"* "$final_merge_dir/" 2>/dev/null || true
  fi
done

# Step 4: Create final merged JAR
echo "Creating final merged JAR file..."
jar cvf android$SDK.jar -C "$final_merge_dir" .

rm -rf tmp