#define thisAppName "MORTAR"

[Setup]
WizardStyle=modern
AppName={#thisAppName}
AppVersion={#thisAppVersion}
AppCopyright=Copyright (C) 2024  Felix Baensch, Jonas Schaub; licensed under MIT license
AppId={{{#thisAppId}}
DefaultDirName={commonpf}\{#thisAppName}\{#thisAppName}v{#thisAppVersion}
AppPublisher={#thisAppName}
VersionInfoProductName={#thisAppName}
MinVersion=10.0.19045
OutputDir=.\out
OutputBaseFilename={#thisAppName}_v{#thisAppVersion}_WINx64_setup
DisableReadyPage=True
DisableWelcomePage=False
DisableDirPage=no
DisableProgramGroupPage=True
UninstallDisplayName={#thisAppName}v{#thisAppVersion}
UninstallDisplayIcon={app}\icon\Mortar_Logo_Icon1.ico
ArchitecturesInstallIn64BitMode=x64os
LicenseFile=LICENSE.txt
DisableStartupPrompt=True
UsePreviousAppDir=False
DirExistsWarning=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Dirs]
Name: "{app}"

[Files]
; Place any regular files here
Source: ".\in\bin\*"; DestDir: "{app}\bin"; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: ".\in\jdk-21.0.1_12_jre\*"; DestDir: "{app}\jdk-21.0.1_12_jre"; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: ".\in\lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: ".\in\tutorial\*"; DestDir: "{app}\tutorial"; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: ".\in\icon\*"; DestDir: "{app}\icon"; Flags: ignoreversion recursesubdirs createallsubdirs;

[Icons]
; Mortar
Name: "{commondesktop}\{#thisAppName} {#thisAppVersion}"; Filename: "{app}\bin\{#thisAppName}.bat"; IconFilename: "{app}\icon\Mortar_Logo_Icon1.ico"; Tasks: desktopicon_mortar
Name: "{commonstartmenu}\{#thisAppName} {#thisAppVersion}"; Filename: "{app}\bin\{#thisAppName}.bat"; IconFilename: "{app}\icon\Mortar_Logo_Icon1.ico"; Tasks: startmenu_mortar
; Mortar 20GB
Name: "{commondesktop}\{#thisAppName} {#thisAppVersion} 20GB"; Filename: "{app}\bin\{#thisAppName}_20GB.bat"; IconFilename: "{app}\icon\Mortar_Logo_Icon1.ico"; Tasks: desktopicon_mortar20
Name: "{commonstartmenu}\{#thisAppName} {#thisAppVersion} 20GB"; Filename: "{app}\bin\{#thisAppName}_20GB.bat"; IconFilename: "{app}\icon\Mortar_Logo_Icon1.ico"; Tasks: startmenu_mortar20

[Tasks]
; Mortar
Name: "desktopicon_mortar"; Description: "Create a &Desktop icon for {#thisAppName}"; GroupDescription: "Shortcuts:"; Flags: unchecked
Name: "startmenu_mortar";  Description: "Create a &Start Menu icon for {#thisAppName}"; GroupDescription: "Shortcuts:"
; Mortar 20GB
Name: "desktopicon_mortar20"; Description: "Create a &Desktop icon for {#thisAppName} 20GB"; GroupDescription: "Shortcuts:"; Flags: unchecked
Name: "startmenu_mortar20";  Description: "Create a &Start Menu icon for {#thisAppName} 20GB"; GroupDescription: "Shortcuts:"

[Run]
Filename: "{app}\bin\{#thisAppName}.bat"; \
Description: "Launch {#thisAppName}"; \
Flags: nowait postinstall skipifsilent
; This is an unconventional to alter the name of the uninstaller.
; Running the uninstall.exe will work as expected.
; CAUTION: The integration with the Windows Control Panel (Control Panel\Programs\Programs and Features) will NOT work!
; If the common way of uninstalling is through the Windows GUI than I would not recommend this approach.
; Maybe adding a note that the uninstaller is called unins000.exe in the README.md/documentation would be a good idea.
; However, if the default way of uninstalling is through the uninstall.exe than this might be a good solution.
;
; Filename: "{cmd}"; Parameters: "/C rename ""{app}\unins000.exe"" ""uninstall.exe"""; Flags: runhidden
; Filename: "{cmd}"; Parameters: "/C rename ""{app}\unins000.dat"" ""uninstall.dat"""; Flags: runhidden

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
