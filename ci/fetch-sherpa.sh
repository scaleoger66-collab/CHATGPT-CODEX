#!/usr/bin/env bash
set -euo pipefail
mkdir -p voices/libs
file=voices/libs/sherpa-onnx-1.13.8.aar
checksum=633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96
if ! echo "$checksum  $file" | sha256sum --check --status 2>/dev/null; then
  curl --fail --location --retry 3 --output "$file.part" https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar
  echo "$checksum  $file.part" | sha256sum --check
  mv "$file.part" "$file"
fi
