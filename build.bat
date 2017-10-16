REM The version of electron
set npm_config_target=1.7.8

REM The architecture of Electron, can be ia32 or x64.
set npm_config_arch=x64
set npm_config_target_arch=x64

REM Download headers for Electron.
set npm_config_disturl=https://atom.io/download/electron

REM Tell node-pre-gyp that we are building for Electron.
set npm_config_runtime=electron

REM Tell node-pre-gyp to build module from source code.
set npm_config_build_from_source=true

REM Install all dependencies
npm install
