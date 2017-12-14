# USBConn
This mobile app for Android performs three functions. 

Reset (/app/src/main/java/com/example/bruckw/usbconn/MainActivity.java)

  -Instructs the pan tilt to realign itself to its default position

Scan (/app/src/main/java/com/example/bruckw/usbconn/Scan.java & MainActivity.java)

  -Incrementally sweeps across the azimuth and zenith angles to find where the light source is located

Auto (/app/src/main/java/com/example/bruckw/usbconn/Auto.java & MainActivity.java)

  -Completes the Scan function also but tracks the sun's movements and send position changes to the pan/tilt
