ArtMidiJava
===========

This uses [ArtClientLib](https://raw.githubusercontent.com/rjwut/ArtClientLib/master/readme.md) to connect to an Artemis server and manipulate engineering controls based on the input from a midi mixer device. This method has proved to be much more reliable than the previous attempt where I manipulated the mouse cursor to actually click on the controls on-screen.

Running Instructions:

1. Start the Artemis server
2. Start the proxy, point it at the location of the Artemis server
3. Start the Artemis game client, connect to localhost (and custom port if specified)

Commandline Usage:

java -jar ArtMidiJava.jar remoteserver[:port] [localport]

remoteserver - the address of the currently running artemis game server (and optionally port, defaults to standard 2010)
localport - the local port to listen on for connections from the artemis game client (defaults to standard 2010)

On the midi mixer, the sliders correspond to the in-game energy sliders, the knobs correspond to the in-game coolant levels. The S, M, R buttons do not do anything when pressed, but instead the LED lights correspond to heat levels. More lights lit up = more heat, and will start blinking when heat levels are critical.

