# Skycat's Binary Search Tool
### Not a mod, don't put it in your mods folder!
This tool helps you binary search through your fabric mod list. This is useful when you think there is one mod that is 
causing problems, but you can't figure out which one from logs. This will NOT work when there's more than one mod 
causing the problem, when you're missing dependencies (including Minecraft or Java), or for non-fabric things.
## How to use
0. Make sure you have Java 17+ installed. You may need to restart your computer.
1. Download the application from the [Releases](https://github.com/skycatminepokie/FabricBinarySearchTool/releases/latest) 
page.
2. Double-click it to run it
3. Go to your `mods` folder and copy the path (on Windows with the default launcher it'll look like
`C:\Users\YourName\AppData\Roaming\.minecraft\mods`).
4. Paste that into the program in the "mods folder" text box. 
5. Make sure Minecraft is closed, then click start! The tool will disable about half your mods and keep required dependencies enabled too.
6. Check if the problem was there and close Minecraft (it's ok if it crashed).
7. If the **SAME** problem is still there, click "Failure." If it's gone, click "Success."
8. Go back to #7 until the program reports that it is done.
9. Once you're done, be sure to report the issue to the mod authors.
## What is a binary search?
In this context, a binary search is disabling half of your mods, and checking if the problem still occurs. If it does, 
then you know the problem is in that half of the mods. If it doesn't, the problem is in the other half. Then you do it 
again. Eventually, you're left with one mod, which is the problem.

The thing is, this can be hard. You have to make sure every mod has all its dependencies, and keep track of which ones 
have worked and which ones haven't. That's what this tool is for.
## What can it help with?
A binary search can find when one mod, and only one mod, is causing a problem. It can't find a conflict between two 
mods, tell you what dependencies you're missing, or fix the mod. It's a good tool as a last resort if the logs don't 
give a conclusive answer.
> Need help with reading logs? Try the [Fabric Discord server](https://discord.gg/v6v4pMv) in the #player-support 
> channel.

## Troubleshooting
### It doesn't open!
You probably don't have the right version of Java. You can verify this by opening up a terminal (Windows: Press 
the Windows key + R, type `cmd` and hit enter) and running `java -version`. If the version is lower than `1.17.xxx`,
you need to install Java 17 or later, then restart your computer.
### It still doesn't open!
Let's diagnose a litte further. Copy the path to where the jar is (Windows users, it'll probably be in 
`C:\Users\You\Downloads\BinarySearchTool-x.y.z-all.jar`. Open a terminal and run the Jar: `java -jar path/to/tool/tool.jar` (Windows 
users, it'll look like `java -jar C:\Users\You\Downloads\BinarySearchTool-x.y.z-all.jar`
### "...this version of the Java Runtime only recognizes class file versions up to..."
You need to install Java 17 or later, then restart your computer. If the installer asks, check the box that says
"set JAVA_HOME environment variable" or something like that.
### Some other problem
Try the [Fabric Discord server](https://discord.gg/v6v4pMv). Ping `@skycatminepokie` if the tool is 
broken or making no sense. If it's just your Minecraft, ask in the `#player-support` channel.

## What do I do if the tool breaks?
Report it on the [Issues](https://github.com/skycatminepokie/FabricBinarySearchTool/issues) tab. I'll take a look and 
see if it's in-scope to fix. If some of your mods are disabled, you can re-enable them by renaming the files to remove 
the `.disabled` part at the end. Windows will warn you about it - it's safe to ignore. You can also enable them via 
MultiMC (or another launcher probably). For immediate help, try the [Fabric Discord server](https://discord.gg/v6v4pMv). 
Ping `@skycatminepokie` if the tool is broken. If it's just your Minecraft, ask in the `#player-support` channel.
## Building from source
1. Clone the repo
2. Run `.\gradlew shadowJar`
3. Built product is `build/libs/BinarySearchTool-(version)-all.jar`
