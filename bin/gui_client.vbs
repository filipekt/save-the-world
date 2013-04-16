Set oShell = CreateObject ("Wscript.Shell") 
Dim strArgs
strArgs = "cmd /c gui_client.bat"
oShell.Run strArgs, 0, false