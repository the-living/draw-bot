# Draw-bot Firmware

The _draw-bot_ is driven by an Arduino UNO with a [CNC Shield](http://blog.protoneer.co.nz/arduino-cnc-shield/) running [grbl-servo](https://github.com/cprezzi/grbl-servo) firmware. This firmware is a modified version of [GRBL v1.1](https://github.com/gnea/grbl/) that replaces spindle functionality with controls for a servo motor. Please refer to the [GRBL wiki](https://github.com/gnea/grbl/wiki) for more information on its capabilities and interfacing methods.

Loading the Arduino with the correct firmware and settings should only need to be done once -- GRBL stores machine settings on its onboard EEPROM memory.

### UPDATING THE FIRMWARE
In order to use the draw-bot controller application, the connected arduino must be running the correct version of GRBL. There are two methods to flash the firmware to the arduino.

#### Method 1: Compile & upload GRBL via Arduino IDE
- Make sure you have a copy of the [Arduino IDE](https://www.arduino.cc/en/Main/Software) installed on your computer. _Do not use the web-based version._
- Navigate to your Arduino Libraries directory -- this is typically located under `.\Documents\Arduino\libraries\`
- Download the folder `grbl` from this directory and place it in the `Arduino\libraries` directory. Close and restart the Arduino IDE if it is open.
- _Use this version of `grbl` as it includes pre-configured header files. You must also remove any existing copies of grbl in this folder temporarily._
- In the Arduino IDE, navigate to `File > Examples > grbl > grblUpload` to open the premade GRBL upload sketch.
- Make sure the board is connected via USB and the board and port setting are correct under `Tools`.
- Press `Upload` to compile and upload the firmware to the connected Arduino


#### Method 2: Upload HEX file via XLOADER
- A precompiled version of the firmware (`grblUpload.ino.ino.with_bootloader.standard.hex`) is located in this directory.
- Download the program [XLoader](http://russemotto.com/xloader/), and follow the instructions to load the `.hex` file to the connected Arduino.

### UPDATING THE SETTINGS
A newly flashed Arduino running GRBL will need its settings updated to work correctly with the draw-bot. You can use the `Serial Monitor` in the Arduino IDE (found under `Tools>Serial Monitor`) to send messages to GRBL.

Make sure the return character pull-down menu is set to `Newline` and the baudrate to `115200 baud`. You should see the message `Grbl 1.1f ['$' for help]` indicating a successfull connection to GRBL.

The current machine settings can be accessed with the `$$` command - see the GRBL wiki for an explanations of all of the settings. Individual settings can be changed by sending commands formatted as `$x=val` -- for example, sending `$110=5000.0` would set the max speed on the x-axis to 5000 mm/min.

Modify the settings so they match the following:
```
$0=10
$1=255
$2=0
$3=0
$4=0
$5=0
$6=0
$10=2
$11=0.010
$12=0.002
$13=0
$20=0
$21=0
$22=0
$23=0
$24=25.000
$25=500.000
$26=250
$27=1.000
$30=1000
$31=0
$32=0
$100=26.667
$101=26.667
$102=250.000
$110=10000.000
$111=10000.000
$112=500.000
$120=250.000
$121=250.000
$122=10.000
$130=200.000
$131=200.000
$132=200.000
```

GRBL also allows for up to two lines of startup code to run every time GRBL is started or reset. You can see the current startup codes by entering `$N` and set them by sending commands formatted as `$Nx=cmd`.

Update the startup commands to match the following:
```
$N0=M3S100G4P0.25
$N1=M5
```

#### _Congrats -- the robot should now be ready to draw!_
