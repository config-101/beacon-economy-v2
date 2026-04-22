BUILDING ON WINDOWS

1. Unzip this folder.
2. Open the folder.
3. Double-click build.bat.

Or run this command inside the folder:

C:\Gradle\bin\gradle.bat build

The finished mod jar will be in:

build\libs

If the build fails, copy the full error text and send it back.

IMPORTANT:
This fixed version removes the settings.gradle repository mode error:
"Build was configured to prefer settings repositories over project repositories..."

FIXED IN THIS VERSION:
- Uses implementation for Fabric Loader and Fabric API, matching the 26.1.2 Fabric example project.
- Removes Mojang mappings for 26.1.2 non-obfuscated builds.
