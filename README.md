# GRPC GUI

A GUI for invoking GRPC services. Built with electron

## To Use

To clone and run this repository you'll need [Git](https://git-scm.com) and [Node.js](https://nodejs.org/en/download/) (which comes with [npm](http://npmjs.com)) installed on your computer.

If you're on windows, you probably need to do the following first
- Run
```bash
npm install --global --production windows-build-tools
npm install --global node-gyp
setx PYTHON "%USERPROFILE%\.windows-build-tools\python27\python.exe"
```
- Install [Windows 8.1 SDK](https://developer.microsoft.com/en-us/windows/downloads/windows-8-1-sdk)
- Install VS 2015 Universal Windows Platform Tools (check the box in the Visual Studio 2015 installer)


Now, this should work regardless of platform:
```bash
# Clone this repository
git clone https://github.com/markosindustries/grpc-gui
# Go into the repository
cd grpc-gui
# Install dependencies and compile native modules like GRPC against electron headers
build
# Run the app
npm start
```
