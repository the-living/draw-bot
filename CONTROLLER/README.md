# Portrait-bot Controller Software
_Designed & built by The Living, an Autodesk Studio. 2017._

![User Interface](DOCUMENTATION/UI_01.png)

1. #### [User Interface](#user-interface)
2. #### [Typical Workflow](#typical-workflow)
3. #### [Troubleshooting](#troubleshooting)
4. #### [Manual GCODE Commands](#manual-gcode-commands)

***
The application can either be run from [Processing](https://processing.org/download/) using the `drawbot_app.pde` sketch or by using a precompiled standalone executable file located in `drawbot_app\application.[OS]`.

_Running the PDE sketch in Processing requires the [ControlP5](http://www.sojamo.de/libraries/controlP5/) library (install manually or through the built-in Processing library management interface)._

***
# [User Interface](#user-interface)

![UI-annotated](DOCUMENTATION/UI_02.png)
  ### PREVIEW Area
- __(1) Canvas Preview Window__ Blue: move / Red: draw
- __(2) Current Position__ Displays pen's current (x, y) position, in mm. Blue: pen up / Red: pen down
- __(3) Machine Status__ Displays current machine status, including state, machine position (_MPos_), buffer capacity (_Bf_), and feedrate / servo position (_FS_).
- __(4) Run Preview__ Starts simulation of GCODE. Press button again to stop and reset preview.
- __(5) Regenerate__ Regenerate GCODE preview.

### MANUAL CONTROL Area

- __(6) Start/Reset__ (While not running) Starts streaming commands to the robot. (While paused) Resets the machine, clearing all queued commands and moving back to the home position.
_After issuing `PAUSE` command, wait to press `RESET` until machine has stopped moving. Failure to do so may require resetting the robot by pressing `CONNECT`._
- __(7) Pause/Resume__ (While running) Pauses the machine, and raises the pen. (While paused) Lowers the pen, and resumes the drawing process.
- __(8) Pen Controls__ Manually raise/lower the pen. Pen down position can be adjusted via the slider or text input as needed to ensure contact with drawing surface (hit `ENTER` after updating text input).
- __(9) Machine Jogging__ Move nozzle up/down/left/right in 1, 10, or 100mm increments
- __(10) Go Home__ Moves nozzle back to home (0,0) position
- __(11) Set Origin__ Sets current nozzle position as the origin (0,0).
- __(12) Canvas Settings__ Current settings for canvas (width, height) and max painting speed (in mm/min). Hit `ENTER` after entering a new value to update settings.

### FILE LOADING Area
- __(13) Load File__ Opens a dialogue to select file(s) to run
- __(14) Load Settings__ Toggle input file type (GCODE txt / JSON) and loading mode (SINGLE FILE / DIRECTORY).
- __(15) Current File__ Displays currently loaded file or directory.
- __(16) GCODE Start Position__ Select GCODE position to start drawing, using the slider or the arrow buttons to step forward/backward one step. Machine will move pen to new starting position.

### CONSOLE Area
- __(17) Manual Command Entry__ Allows GCODE commands to be entered and issued manually. Hit ENTER to submit command. Robot will execute manual commands immediately.
- __(18) TX/RX Commands__ Displays last transmitted command (TX) and last received message (RX).
- __(19) Drawing Status__ Displays GCODE lines issued to serial buffer, and GCODE lines processed by the robot.
- __(20) Connection Status__ Will indicate connection to robot controller over serial COM port.
- __(21) Connect/Reconnect__ Attempts to (re)connect to robot controller over COM port. A drop-down list will appear if multiple ports are available.

---
# [Typical Workflow](#typical-workflow)
1. If not connected, plug in USB connection to Arduino controller and press `CONNECT` to establish serial connection.
3. Use the `JOGGING` buttons to position the pen at the __top left__ corner of the canvas. Click `SET ORIGIN` to set this as your work coordinate origin (0,0).
4. Select correct load settings (file type and loading mode), then click `LOAD` to select and process drawing file(s). A preview will appear in the Canvas Preview area.
5. Click `START` to begin machine operation. Click `PAUSE` to temporarily halt the machine operation, and `RESUME`. Click `RESET` if you need to cancel the drawing and return to home position.
6. Repeat from (4) to load a new drawing.

---
# [Troubleshooting](#troubleshooting)
__Canvas size is incorrect or pen doesn't touch drawing surface__
- Update canvas width, height settings or pen position settings. After hitting `ENTER` (or releasing the mouse on the slider), you will see the canvas preview update.

__Pen isn't at home Position__
- Use `JOG` buttons to move pen to top-left corner, and click `SET ORIGIN` to reset home position.

__Drawing was interrupted, or machine was bumped during operation__
- If drawing is still running, press `PAUSE`, wait for the machine to stop moving, then press `RESET`. Re-home the machine (see above). Using the `GCODE Start Position` controls, adjust until drawing position matches where you'd like to restart. Press `START` to resume the drawing.
---
# [Manual GCODE Commands](#manual-gcode-commands)

See [GRBL 1.1 Wiki](https://github.com/gnea/grbl/wiki/Grbl-v1.1-Commands) for more information about interfacing with robot controller.

`G0 / G1` __MOVE Command__
- G0: Move at max speed
- G1: Move at specified feed rate (F)
- X: Destination X coordinate (in mm)
- Y: Destination Y coordinate (in mm)
- F: Feed rate (in mm/min)
- Example: `G1X55.5Y125.25F2500.0` or `G0X1220.0Y1220.0`

`G2 / G3` __ARC Command__
- G2: Move in a clockwise arc (with blasting)
- G3: Move in a counter-clockwise arc (with blasting)
- I: Arc center X coordinate, given relative to current position (in mm)
- J: Arc center Y coordinate, given relative to current position (in mm)
- X: Destination X coordinate, absolute position (in mm)
- Y: Destination Y coordinate, absolute position (in mm)
- F: Feed rate (in mm/min)
- Example: `G02I10.0J0.0X20.0Y0.0F5000.0`

`G4 / G5` __DWELL Command__
- G4: Pause (without blasting)
- G5: Dwell (with blasting)
- P: Duration (in seconds)
- Example: `G4P0.25`

`M3 / M5` __SERVO Control__
- M5: Move servo to zero position
- M3: Move servo to specified position (S)
- S: Servo position, ranging from 0-1000
- _Our servo model (Hitec HS-645MG) has a 180&deg; range of motion_
- Example: `M3S200` or `M5`
