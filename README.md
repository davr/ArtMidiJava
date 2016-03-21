ArtMidiJava
===========

This uses [ArtClientLib](https://raw.githubusercontent.com/rjwut/ArtClientLib/master/readme.md) to connect to an Artemis server and manipulate engineering controls based on the input from a midi mixer device. This method has proved to be much more reliable than the previous attempt where I manipulated the mouse cursor to actually click on the controls on-screen.

**Download Pre-compiled JAR**
Direct link to download here: [ArtMidiJava.jar](https://github.com/davr/ArtMidiJava/raw/master/ArtMidiJava/ArtMidiJava.jar). 

**Running Instructions:**

1. Start the Artemis server
2. Start the proxy, point it at the location of the Artemis server
3. Start the Artemis game client, connect to localhost (and custom port if specified)

**Commandline Usage:**

java -jar ArtMidiJava.jar remoteserver[:port] [localport]

remoteserver - the address of the currently running artemis game server (and optionally port, defaults to standard 2010)
localport - the local port to listen on for connections from the artemis game client (defaults to standard 2010)

**In-Game Usage:**

1. Volume sliders correspond to the energy sliders for respective ship systems
2. The knobs at the top correspond to coolant levels. Because there is no way to prevent you turning all the knobs up at the same time, there is an algorithm in play that will allocate coolant based on the relative settings. So if you turn two knobs all the way up, the two systems will each get 4 units of coolant. If you turn all the knobs all the way up, each system will get 1 unit of coolant. It's a little different gameplay, but in my opinion it makes more sense than having it so that sometimes turning a knob up does nothing until you turn other knobs down.
3. The S/M/R buttons don't do anything when pressed, but they light up as heat levels increase for the associated system. More heat = more lights turned on, and at critical levels they start blinking.
