mkdir FSim
mkdir "FSim/Library"
xcopy "Editor/Library" "FSim/Library"
mkdir "FSim/ProjectSettings"
xcopy "Editor/ProjectSettings" "FSim/ProjectSettings"
cd FSim
mklink /D Assets ..\Editor\Assets
cd ..
