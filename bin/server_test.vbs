Set oShell = CreateObject ("Wscript.Shell") 
Dim strArgs
strArgs = "cmd /c server_test.bat"
oShell.Run strArgs, 0, false