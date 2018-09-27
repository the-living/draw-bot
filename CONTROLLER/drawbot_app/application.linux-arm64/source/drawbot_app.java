import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import controlP5.*; 
import processing.serial.*; 
import javax.swing.JOptionPane; 
import java.awt.event.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class drawbot_app extends PApplet {

 ////////////////////////////////////////////////////////////////////////////////
//                                                                            //
// draw-bot Drawing Robot Platform | The Living | 2018                        //                                                              //
// v5.0 2018.09.27                                                            //
//                                                                            //
////////////////////////////////////////////////////////////////////////////////

// EXTERNAL DEPENDENCIES
//------------------------------------------------------------------------------
 //ControlP5 - UI Interface
 //Serial - Com protocol with Arduino
 //Interface for COM port selection


// GLOBAL VARIABLES
//------------------------------------------------------------------------------
// Debug
Boolean VERBOSE = false; //default: false -- if enabled, print all responses from GRBL
Boolean SIMPLE_MODE = false; //default: false (buffer-fill mode) / true (line-response mode)
int reportFreq = 10; //

// IO
Boolean type_gcode = false;
Boolean load_dir = true;
String fp = "";

// UX
ControlP5 cP5;
CallbackListener cb;
Bang start, pause, load, setorigin, connect, penon, penoff, runpreview, regenpreview;
Bang gohome, x1, x10, x100, x1n, x10n, x100n, y1, y10, y100, y1n, y10n, y100n;
Slider pen, startline;
Bang step_b, step_f;
Textfield setwidth, setheight, setpen, setspeed;
Textfield cmdentry;
Toggle f_type, l_type;
int lastw, lasth;

PFont font24, font18, font16i, font14, font12;
int black, white, grey, charcoal, green, red, blue;
PVector origin;
float scalar;
PShape preview;

// GCODE
StringList gcode;
IntList breaks;
int line, startselect, breakselect;
int issued, completed;
int previewstate;

// MACHINE
float posx, posy, lastx, lasty, spray_speed;
float canvas_width, canvas_height, canvas_margin;
float scaleWidth, scaleHeight, scaleMargin;
float sprayoff = 0.0f;
float sprayon = 28.0f;
float spraymax = 100.0f;
boolean swapSpray = true;

// STATUS
String status;
Boolean streaming, spraying, paused, loaded, idle, previewing;
String versionPattern = "Grbl*";
String startupPattern = ">*:ok";
String statusPattern = "<*>";
String okPattern = "*ok*";
String errorPattern = "*error*";
String CMD_VERSION, CMD_STARTUP, CMD_STATUS, CMD_OK, CMD_ERROR;
Boolean match;

// STREAM MODE
IntList c_line;

// SERIAL
Serial port;
String portname;
String val, sent;
String lastSent;
Boolean connected;
int r = 0;
int timeout = 0;

// SETUP
//------------------------------------------------------------------------------
public void setup() {
    settings(); //INITIALIZE WINDOW SIZE
    lastw = width;
    lasth = height;
    surface.setResizable(true);

    initVariables(); //INITIALIZE SYSTEM VARIABLES
    initPatterns(); //INITIALIZE MESSAGE PATTERNS

    initFonts(); //INITIALIZE UX FONTS
    initColors(); //INITIALIZE UX COLORS

    initPreview( ); //INITIALIZE GCODE PREVIEW

    setupControls(); //GENERATE UX
    selectSerial(); //ATTEMPT TO CONNECT TO SERIAL

    updateCanvasScale();
}

// DRAW
//------------------------------------------------------------------------------
public void draw(){
    checkWindow();
    displayUI(); // DRAW UI
    renderPreview(); // DRAW GCODE PREVIEW
    displayStats(); // DISPLAY DRAWING STATUS
    checkStatus(); // UPDATE BUTTONS BY STATE

    if(previewing) {
        if (streaming) previewing = false;
        // println("Line " + ((gcode.size()-startselect)-previewstate));
        previewstate--;
        if(previewstate <= 0) {
            println("Preview completed.");
            previewing = false;
            // previewstate = 0;
        }
    }

    // REALTIME STATUS REPORTING
    if(connected && r>reportFreq){
        statusReport();
        r = 0;
    }
    r++;

    if (connected) serialRun(); // CHECK SERIAL FOR UPDATES
    renderNozzle(); // DRAW NOZZLE ON PREVIEW

    // TIMEOUT IF SYSTEM HANGS
    // FIRST SHUTS OFF SPRAY
    // THEN CANCELS STREAM AND GOES HOME
    if( idle && spraying && timeout > 120 ){
        send( gSpray(false) );
    }

    if( idle && streaming ){
        timeout++;
        if ( timeout > 1200 ){
          print("TIMED OUT, GOING HOME\n");
          streaming = false;
          line = 0;
          timeout = 0;
          send( home() );
        }
    }
}

// SETTINGS
//------------------------------------------------------------------------------
public void settings(){
    size(1400, 850);
    smooth();
}

public void checkWindow(){
    if( lastw != width || lasth != height || width < 1400 || height < 850) {
        if( width < 1400 || height < 850){
            surface.setSize(max(1400,width), max(850,height));
        }
        updateCanvasScale();
        connect.setPosition(475,height-30);
        runpreview.setPosition(origin.x-50,height-30);
        regenpreview.setPosition(625, height-30);

        lastw = width;
        lasth = height;
    }
    cP5.setGraphics(this, 0, 0);
}

// INIT VARIABLES
public void initVariables(){
    // UX
    origin = new PVector(1000,400);
    scalar = 0.5f;
    // GCODE
    gcode = new StringList();
    line = 0;
    issued = 0;
    completed = 0;
    startselect = 0;
    breakselect = 0;
    previewstate = 0;
    // MACHINE
    posx = 0.0f;
    posy = 0.0f;
    status = "[...]";
    spray_speed = 10000.0f;
    canvas_width = 1270.0f;
    canvas_height = 1270.0f;
    canvas_margin = 0.0f;
    //STATUS
    streaming = false;
    spraying = false;
    paused = false;
    loaded = false;
    match = false;
    idle = false;
    previewing = false;
    //STREAM MODE
    c_line = new IntList();
    // SERIAL
    port = null;
    portname = null;
    val = "...";
    sent = "...";
    connected = false;
    lastSent = "";
}

public void initPatterns(){
    CMD_VERSION = versionPattern.replaceAll(".","[$0]").replace("[*]",".*");
    CMD_STARTUP = startupPattern.replaceAll(".","[$0]").replace("[*]",".*");
    CMD_STATUS = statusPattern.replaceAll(".","[$0]").replace("[*]",".*");
    CMD_OK = okPattern.replaceAll(".","[$0]").replace("[*]",".*");
    CMD_ERROR = errorPattern.replaceAll(".","[$0]").replace("[*]",".*");

}

// PARSE NUMBER FROM GCODE STRING
// Used to extract numerical values from GCode
public float parseNumber(String s, String c, float f){
    String num = "-.0123456789";
    c = c.toUpperCase();
    s = s.toUpperCase();
    int start = s.indexOf(c);
    if( start < 0 ) return f;
    int end = start+1;
    for( int i = start+1; i < s.length(); i++){
    char k = s.charAt(i);
    if( Character.isLetter(k) || k == ' ') break;
        end = i;
    }
    return PApplet.parseFloat( s.substring(start+1,end+1) );
}

// SUMS IntList
// Sums contents of an IntList (used in available buffer tracking)
public int sumList(IntList w){
    int sum = 0;
    for(int i = 0; i<w.size();i++){
        sum += w.get(i);
    }
    return sum;
}


////////////////////////////////////////////////////////////////////////////////
// UX
////////////////////////////////////////////////////////////////////////////////

// INIT COLORS
public void initColors(){
    black = color(0);
    white = color(255);
    grey = color(220);
    charcoal = color(100);
    red = color(237, 28, 36);
    green = color(57, 181, 74);
    blue = color(80, 150, 225);
}

// INIT FONTS
public void initFonts(){
    font24 = loadFont("Roboto-Regular-24.vlw");
    font18 = loadFont("Roboto-Regular-18.vlw");
    font16i = loadFont("Roboto-Italic-16.vlw");
    font14 = loadFont("Roboto-Regular-14.vlw");
    font12 = loadFont("Roboto-Regular-12.vlw");
}


// UPDATE DIMENSIONS
public void updateDim(){
  String w_ = setwidth.getText();
  String h_ = setheight.getText();
  // String m_ = cP5.get(Textfield.class, "margin").getText();

  canvas_width = (w_ != "" && PApplet.parseFloat(w_)>0) ? PApplet.parseFloat(w_) : canvas_width;
  canvas_height = (h_ != "" && PApplet.parseFloat(h_)>0) ? PApplet.parseFloat(h_) : canvas_height;
  canvas_margin = 0.0f;

  updateCanvasScale();
}

// UPDATE PEN POSITION
public void updatePenText(){
    String s_ = cP5.get(Textfield.class, "penpos").getText();
    sprayon = (s_ != "" && PApplet.parseInt(s_)>=0 && PApplet.parseInt(s_)<=100) ? PApplet.parseFloat(s_) : sprayon;
    cP5.get(Slider.class, "penSlider").setValue(sprayon);
    cP5.get(Textfield.class, "penpos").setText(nfs(sprayon,0,1));
    testPen();
}

public void updatePenSlider(){
    Float s_ = cP5.get(Slider.class, "penSlider").getValue();
    cP5.get(Textfield.class, "penpos").setText(nfs(s_,0,1));
    sprayon = s_;
    testPen();
}

public void testPen(){
    send(gSpray(true));
    send(gDwell(0.25f));
    send(gSpray(false));
}

// UPDATE SPEED
public void updateSpeed(){
  String s_ = cP5.get(Textfield.class, "speed").getText();
  spray_speed = (s_ != "" && PApplet.parseFloat(s_)>0 && PApplet.parseFloat(s_)<=10000) ? PApplet.parseFloat(s_) : spray_speed;
  send("G1F"+str(spray_speed)+"\n");
}

public void updateLineSelector(){
    if (breaks.size() > 0){
        startline.setLock(false);
        step_f.setLock(false);
        step_b.setLock(false);
        startline.setMax(max(1,PApplet.parseInt(breaks.size())));
    }
}

public void updatePosition(){
  if( startselect > 0 ) {
        line = startselect;
        issued = startselect-1;
        completed = startselect-1;
        PVector goPt = findLastPt();
        String goCmd = gLine(goPt.x, goPt.y, false);
        port.write( goCmd + "\n");
        sent = goCmd;
        lastSent = goCmd;
    }
}

public PVector findLastPt() {
    String cmd = gcode.get(startselect);
    int type = PApplet.parseInt(parseNumber(cmd,"G",-1));
    int o_ = 1;
    while(type < 0 || type > 3) {
        cmd = gcode.get(startselect - o_);
        type = PApplet.parseInt(parseNumber(cmd, "G", -1));
        o_++;
    }
    return new PVector(parseNumber(cmd, "X", 0),parseNumber(cmd, "Y", 0));
}
////////////////////////////////////////////////////////////////////////////////
// CONTROLP5 CONTROLS
////////////////////////////////////////////////////////////////////////////////


// SET UP CP5 UX CONTROLS
public void setupControls() {
    cP5 = new ControlP5(this);

    // Global Settings
    cP5.setFont( font12 );
    cP5.setColorForeground( black );
    cP5.setColorBackground( white );
    cP5.setColorValueLabel( white );
    cP5.setColorCaptionLabel( white );
    cP5.setColorActive( blue );

    CallbackListener inputGeneric = new CallbackListener() {
        public void controlEvent(CallbackEvent theEvent) {
            switch(theEvent.getAction()) {
                case(ControlP5.ACTION_ENTER):
                    cursor(HAND);
                    break;
                case(ControlP5.ACTION_LEAVE):
                    cursor(ARROW);
                    break;
            }
        }
    };

    CallbackListener textGeneric = new CallbackListener() {
        public void controlEvent(CallbackEvent theEvent) {
            switch(theEvent.getAction()) {
                case(ControlP5.ACTION_ENTER):
                    cursor(TEXT);
                    break;
                case(ControlP5.ACTION_LEAVE):
                    cursor(ARROW);
                    break;
            }
        }
    };


    CallbackListener jogCB = new CallbackListener() {
        public void controlEvent(CallbackEvent theEvent) {
            if(theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                String n_ = theEvent.getController().getName();
                println("JOG "+ n_.toUpperCase());
                switch(n_) {
                    case "y+100":
                        if(!streaming) send( jog( 0, 100 ) );
                        break;
                    case "y+10":
                        if(!streaming) send( jog( 0, 10 ) );
                        break;
                    case "y+1":
                        if(!streaming) send( jog(0, 1) );
                        break;
                    case "y-1":
                        if(!streaming) send( jog(0,-1) );
                        break;
                    case "y-10":
                        if(!streaming) send( jog(0, -10) );
                        break;
                    case "y-100":
                        if(!streaming) send( jog(0, -100) );
                        break;
                    case "x+100":
                        if(!streaming) send( jog(100, 0) );
                        break;
                    case "x+10":
                        if(!streaming) send( jog(10, 0) );
                        break;
                    case "x+1":
                        if(!streaming) send( jog(1, 0) );
                        break;
                    case "x-1":
                        if(!streaming) send( jog(-1, 0) );
                        break;
                    case "x-10":
                        if(!streaming) send( jog(-10, 0) );
                        break;
                    case "x-100":
                        if(!streaming) send( jog(-100, 0) );
                        break;
                }
            }
        }
    };


    ////////////////
    // OPERATIONS //
    ////////////////

    // OPERATION - Serial Reconnect Button
    connect = cP5.addBang("connect")
        .setPosition(475,height-30)
        .setSize(100,25)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(white)
        .setColorActive(blue);
    connect.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(black)
        .setFont(font12)
        .setText("CONNECT");
    connect.addCallback(inputGeneric);
    connect.addCallback(new CallbackListener() {
        public void controlEvent(CallbackEvent theEvent) {
            if(theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                if(connected){
                    port.stop();
                    portname = null;
                }
                selectSerial();
            }
        }
    });

    // OPERATION - Preview Button
    runpreview = cP5.addBang("preview")
        .setPosition(origin.x-50,height-30)
        .setSize(100, 25)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black)
        .setColorActive(red);
    runpreview.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font14)
        .setText("RUN PREVIEW");
    runpreview.addCallback(inputGeneric);
    
    // OPERATION - Regenerate Preview
    regenpreview = cP5.addBang("regen")
        .setPosition(625, height-30)
        .setSize(100, 25)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black)
        .setColorActive(red);
    regenpreview.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font14)
        .setText("REGENERATE");
    regenpreview.addCallback(inputGeneric);

    // OPERATION - Start Button
    start = cP5.addBang("start")
        .setPosition(345,245)
        .setSize(245,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(green);
    start.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font24)
        .setText("RUN FILE");
    start.addCallback(inputGeneric);


    // OPERATION - Pause Button
    pause = cP5.addBang("pause")
        .setPosition(345,300)
        .setSize(245,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(red);
    pause.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font24)
        .setText("PAUSE");
    pause.addCallback(inputGeneric);


    /////////////
    // FILE IO //
    /////////////

    // LOADING - Load File(s) Button
    load = cP5.addBang("load")
        .setPosition(470,375)
        .setSize(120,70)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(blue);
    load.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(black)
        .setFont(font18)
        .setText("LOAD");
    load.addCallback(inputGeneric);
    load.addCallback( new CallbackListener() {
        public void controlEvent(CallbackEvent theEvent) {
            if(theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                if( load_dir ) loadFolder();
                else loadSingle();
            }
        }
    });

    // LOADING - Input file type toggle
    f_type = cP5.addToggle("file-type")
        .setPosition(25,390)
        .setSize(80,30)
        .setColorForeground(white)
        .setColorBackground(white)
        .setColorActive(white)
        .setState(type_gcode);
    f_type.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(black)
        .setFont(font18)
        .setText("JSON");
    f_type.addCallback(inputGeneric);

    // LOADING - Input loading type toggle
    l_type = cP5.addToggle("load-mode")
        .setPosition(110,390)
        .setSize(80,30)
        .setColorForeground(white)
        .setColorBackground(white)
        .setColorActive(white)
        .setState(load_dir);
    l_type.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(black)
        .setFont(font18)
        .setText("FILE");
    l_type.addCallback(inputGeneric);

    // LOADING - Start position slider
    startline = cP5.addSlider("startline")
        .setRange(0,100)
        .setPosition(25,450)
        .setSize(440, 25)
        .setTriggerEvent(Slider.RELEASE)
        .snapToTickMarks(false)
        .showTickMarks(false)
        .setDecimalPrecision(0)
        .setValue(0)
        // .setColorValueLabel(black)
        .setColorForeground(grey)
        .setLock(true);
    startline.getCaptionLabel()
        .setText("");
    startline.getValueLabel()
        .setColor(black)
        .setFont(font18);
    startline.addCallback(inputGeneric);
    startline.addCallback(new CallbackListener(){
        public void controlEvent(CallbackEvent theEvent) {
            if(theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                previewing = false;
                breakselect = PApplet.parseInt(startline.getValue());
                startline.setValue(breakselect);
                startselect = breaks.get(breakselect);
                updatePreview();
                updatePosition();
            }
        }
    });

    // LOADING - Start position step -
    step_b = cP5.addBang("startDown")
        .setPosition(470,450)
        .setSize(25,25)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(white)
        .setLock(true);
    step_b.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(black)
        .setFont(font18)
        .setText("<");
    step_b.addCallback(inputGeneric);
    step_b.addCallback(new CallbackListener(){
        public void controlEvent(CallbackEvent theEvent) {
            if(theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                previewing = false;
                breakselect = max(0, breakselect-1);
                startline.setValue(breakselect);
                startselect = breaks.get(breakselect);
                updatePreview();
                updatePosition();
            }
        }
    });

    // LOADING - Start position step +
    step_f = cP5.addBang("startUp")
        .setPosition(500,450)
        .setSize(25,25)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(white)
        .setLock(true);
    step_f.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(black)
        .setFont(font18)
        .setText(">");
    step_f.addCallback(inputGeneric);
    step_f.addCallback(new CallbackListener(){
        public void controlEvent(CallbackEvent theEvent) {
            if(theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                previewing = false;
                breakselect = min(breaks.size()-1, breakselect+1);
                startline.setValue(breakselect);
                startselect = breaks.get(breakselect);
                updatePreview();
                updatePosition();
            }
        }
    });

    //////////////////////////////
    // ROBOT / DRAWING SETTINGS //
    //////////////////////////////

    // SETTINGS - Pen Height Slider
    pen = cP5.addSlider("penSlider")
        .setRange(0,100)
        .setPosition(346,170)
        .setSize(245,25)
        .setTriggerEvent(Slider.RELEASE)
        .setDecimalPrecision(1)
        .setNumberOfTickMarks(11)
        .snapToTickMarks(false)
        .showTickMarks(true)
        .setColorTickMark(black)
        .setValue(sprayon);
    pen.getCaptionLabel()
        .setText("");
    pen.addCallback(inputGeneric);
    pen.addCallback(new CallbackListener() {
        public void controlEvent( CallbackEvent theEvent) {
            if(theEvent.getAction()==ControlP5.ACTION_BROADCAST) {
                Float s_ = cP5.get(Slider.class, "penSlider").getValue();
                cP5.get(Textfield.class, "penpos").setText(nfs(s_,0,1));
                sprayon = s_;
                testPen();
            }
        }
    });

    // SETTINGS - Pen Height Entry
    setpen = cP5.addTextfield("penpos")
        .setPosition( 345, 110 )
        .setSize( 120, 30 )
        .setFont( font18 )
        .setFocus( false )
        .setColor( black )
        .setAutoClear( false )
        .setValue( nfs(sprayon,0,1) )
        .setColorCursor( blue );
    setpen.getCaptionLabel()
        .setColor( black )
        .setFont( font14 )
        .alignX( ControlP5.LEFT )
        .setText( "PEN DOWN %" );
    setpen.addCallback(textGeneric);

    // SETTINGS - Canvas WIDTH
    setwidth = cP5.addTextfield("width")
        .setPosition( 345, 50 )
        .setSize( 120, 30 )
        .setFont( font18 )
        .setFocus( false )
        .setColor( black )
        .setAutoClear( false )
        .setInputFilter( ControlP5.INTEGER )
        .setValue( nf(canvas_width) )
        .setColorCursor( blue );
    setwidth.getCaptionLabel()
        .setColor( black )
        .setFont( font14 )
        .alignX( ControlP5.LEFT )
        .setText( "WIDTH (mm)" );
    setwidth.addCallback(textGeneric);

    // SETTINGS - Canvas HEIGHT
    setheight = cP5.addTextfield("height")
        .setPosition( 470, 50 )
        .setSize( 120, 30 )
        .setFont( font18 )
        .setFocus( false )
        .setColor( black )
        .setAutoClear( false )
        .setInputFilter( ControlP5.INTEGER )
        .setValue( nf(canvas_height) )
        .setColorCursor( blue );
    setheight.getCaptionLabel()
        .setColor( black )
        .setFont( font14 )
        .alignX( ControlP5.LEFT )
        .setText( "HEIGHT (mm)" );
    setheight.addCallback(textGeneric);

    // SETTINGS - Motion SPEED/FEEDRATE
    setspeed = cP5.addTextfield("speed")
        .setPosition( 470, 110 )
        .setSize( 120, 30 )
        .setFont( font18 )
        .setFocus( false )
        .setColor( black )
        .setAutoClear( false )
        .setInputFilter( ControlP5.INTEGER )
        .setValue( nf(spray_speed) )
        .setColorCursor( blue );
    setspeed.getCaptionLabel()
        .setColor( black )
        .setFont( font14 )
        .alignX( ControlP5.LEFT )
        .setText( "SPEED (mm/min)" );
    setspeed.addCallback(textGeneric);


    ///////////////////////////////
    // MANUAL CONTROLS / JOGGING //
    ///////////////////////////////

    // MANUAL CONTROLS - Y-100 button
    y100n = cP5.addBang("y-100")
        .setPosition(150,50)
        .setSize(50,32)
        .setColorForeground(black)
        .setTriggerEvent(Bang.RELEASE);
    y100n.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("-100");
    y100n.addCallback(inputGeneric);
    y100n.addCallback(jogCB);

    // MANUAL CONTROLS - Y-10 button
    y10n = cP5.addBang("y-10")
        .setPosition(150,87)
        .setSize(50,32)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    y10n.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("-10");
    y10n.addCallback(inputGeneric);
    y10n.addCallback(jogCB);

    // MANUAL CONTROLS - Y-1 button
    y1n = cP5.addBang("y-1")
        .setPosition(150,124)
        .setSize(50,32)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    y1n.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("-1");
    y1n.addCallback(inputGeneric);
    y1n.addCallback(jogCB);

    // MANUAL CONTROLS - Y+100 button
    y100 = cP5.addBang("y+100")
        .setPosition(150,318)
        .setSize(50,32)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    y100.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("+100");
    y100.addCallback(inputGeneric);
    y100.addCallback(jogCB);

    // MANUAL CONTROLS - Y+10 button
    y10 = cP5.addBang("y+10")
        .setPosition(150,281)
        .setSize(50,32)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    y10.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("+10");
    y10.addCallback(inputGeneric);
    y10.addCallback(jogCB);

    // MANUAL CONTROLS - Y+1 button
    y1 = cP5.addBang("y+1")
        .setPosition(150,244)
        .setSize(50,32)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    y1.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("+1");
    y1.addCallback(inputGeneric);
    y1.addCallback(jogCB);

    // MANUAL CONTROLS - X-100 button
    x100n = cP5.addBang("x-100")
        .setPosition(25,175)
        .setSize(32,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    x100n.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("-100");
    x100n.addCallback(inputGeneric);
    x100n.addCallback(jogCB);

    // MANUAL CONTROLS - X-10 button
    x10n = cP5.addBang("x-10")
        .setPosition(62,175)
        .setSize(32,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    x10n.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("-10");
    x10n.addCallback(inputGeneric);
    x10n.addCallback(jogCB);

    // MANUAL CONTROLS - X-1 button
    x1n = cP5.addBang("x-1")
        .setPosition(99,175)
        .setSize(32,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    x1n.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("-1");
    x1n.addCallback(inputGeneric);
    x1n.addCallback(jogCB);

    // MANUAL CONTROLS - X+100 button
    x100 = cP5.addBang("x+100")
        .setPosition(293,175)
        .setSize(32,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    x100.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("+100");
    x100.addCallback(inputGeneric);
    x100.addCallback(jogCB);

    // MANUAL CONTROLS - X+10 button
    x10 = cP5.addBang("x+10")
        .setPosition(256,175)
        .setSize(32,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    x10.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("+10");
    x10.addCallback(inputGeneric);
    x10.addCallback(jogCB);

    // MANUAL CONTROLS - X+1 button
    x1 = cP5.addBang("x+1")
        .setPosition(219,175)
        .setSize(32,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    x1.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("+1");
    x1.addCallback(inputGeneric);
    x1.addCallback(jogCB);

    // MANUAL CONTROLS - Go Home Button
    gohome = cP5.addBang("home")
        .setPosition(140,165)
        .setSize(70,70)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    gohome.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font12)
        .setText("GO HOME");
    gohome.addCallback(inputGeneric);

    // MANUAL CONTROLS - Pen Off Button
    penoff = cP5.addBang("penUp")
        .setPosition(345,210)
        .setSize(120,25)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    penoff.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font14)
        .setText("PEN UP");
    penoff.addCallback(inputGeneric);

    // MANUAL CONTROLS - Pen On Button
    penon = cP5.addBang("penDn")
        .setPosition(470,210)
        .setSize(120,25)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(black);
    penon.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(white)
        .setFont(font14)
        .setText("PEN DOWN");
    penon.addCallback(inputGeneric);

    // MANUAL CONTROLS - Set Origin Button
    setorigin = cP5.addBang("origin")
        .setPosition(255,300)
        .setSize(70,50)
        .setTriggerEvent(Bang.RELEASE)
        .setColorForeground(white)
        .setColorActive(blue);
    setorigin.getCaptionLabel()
        .align(ControlP5.CENTER, ControlP5.CENTER)
        .setColor(black)
        .setFont(font14)
        .setText("SET (0,0)");
    setorigin.addCallback(inputGeneric);



    // MANUAL CONTROLS - User Command Entry
    cmdentry = cP5.addTextfield("cmdEntry")
        .setPosition( 15, 510 )
        .setSize( 560, 50 )
        .setFont( font24 )
        .setFocus( true )
        .setColor( black )
        .setAutoClear( true )
        .setColorCursor( blue );
    cmdentry.getCaptionLabel()
        .setColor(white)
        .setFont(font14)
        .alignX(ControlP5.LEFT)
        .setText("MANUAL ENTRY");
    cmdentry.addCallback(textGeneric);

}

// UX CONTROL EVENTS
public void controlEvent( ControlEvent theEvent ) {
if ( theEvent.isController() ) {
    String eventName = theEvent.getName();
    switch( eventName ) {
        // case "connect":
        // if(connected){
        //     port.stop();
        //     portname = null;
        // }
        // selectSerial();
        // break;
        case "preview":
            if(!previewing){
                println("PREVIEWING GCODE...");
                previewstate = gcode.size() - startselect;
                previewing = true;
                break;
            } else {
                println("PREVIEW STOPPED.");
                previewing = false;
                break;
            }
        case "regen":
            updatePreview();
            break;
        case "home":
            if(!streaming) send( home() );
            break;
        case "penUp":
            if(!streaming) send( gSpray(false) );
            break;
        case "penDn":
            if(!streaming) send( gSpray(true) );
            break;
        // case "penSlider":
        // if(!streaming) updatePenSlider();
        // break;
        case "origin":
            if(!streaming) send( origin() );
            break;
        case "width":
        case "height":
        // case "margin":
          if(!streaming) updateDim();
          break;
        case "penpos":
            if(!streaming) updatePenText();
            break;
        case "speed":
            if(!streaming) updateSpeed();
            break;
        case "cmdEntry":
            if(!streaming) send( manualEntry() );
            break;
        // case "load":
        //     // if( load_dir ) loadFolder();
        //     // else loadSingle();
        //     // updateLineSelector();
        //     break;
        case "load-mode":
        if(!streaming) {
            load_dir = !load_dir;
            print("LOADING MODE: "+ ((load_dir)?"DIRECTORY":"SINGLE FILE") + "\n");
        }
        break;
        case "file-type":
        if(!streaming){
            type_gcode = !type_gcode;
            print("INPUT FILETYPE: "+ ((type_gcode)?"GCODE":"JSON") + "\n");
        }
        break;
        case "start":
        if(paused){
            streaming = false;
            resetStatus();
            sendByte( gReset() );
            delay(100);
            send( home() );
            paused = false;
            break;
        }
        if(!streaming){
            updateSpeed();
            streaming = true;
            stream();
        }
        break;
        case "pause":
        paused = !paused;
        if(paused){
            sendByte( gDoor() );
            } else {
                sendByte( gResume() );
                streaming = true;
                stream();
            }
            break;
            default:
            break;
        }
    }
}

// CHECK STATUS
public void checkStatus(){

    relabelToggle( f_type, ((type_gcode)?"GCODE":"JSON"));
    relabelToggle( l_type, ((load_dir)?"DIR":"FILE"));

    if( !connected ){
        lockButton( start, true, charcoal, grey );
        relabelButton( start, "START" );
        lockButton( pause, true, charcoal, grey );
        relabelButton( pause, "PAUSE" );
        relabelButton( connect, "CONNECT" );
        return;
    }

    if( loaded ){
        relabelButton( load, "RELOAD" );
        if (startline.getMax() < gcode.size()) {
            updateLineSelector();
        }
        lockSlider( startline, false, grey, black);
        lockButton( step_f, false, white, black);
        lockButton( step_b, false, white, black);
    }

    if( (streaming && !paused) ){
        lockButton( start, false, blue, white );
        relabelButton( start, "RUNNING" );
        lockButton( pause, false, red, white );
        relabelButton( pause, "PAUSE" );
        lockButton( load, true, charcoal, grey );
        lockButton( setorigin, true, charcoal, grey );
        lockButton( connect, true, charcoal, grey );
        lockButton( runpreview, true, black, black);
        lockSlider( startline, true, charcoal, white);
        lockButton( step_f, true, black, white);
        lockButton( step_b, true, black, white);
        return;
    }

    if( paused ){
        lockButton( start, false, red, white );
        relabelButton( start, "RESET" );
        lockButton( pause, false, green, white );
        relabelButton( pause, "RESUME" );
        lockButton( load, true, charcoal, grey );
        lockButton( setorigin, true, charcoal, grey );
        lockButton( connect, true, charcoal, grey );
        lockButton( runpreview, true, black, black);
        lockSlider( startline, true, charcoal, white);
        lockButton( step_f, true, black, white);
        lockButton( step_b, true, black, white);
        return;
    }

    lockButton( start, false, green, white );
    relabelButton( start, "START" );
    lockButton( pause, false, red, white );
    relabelButton( pause, "PAUSE" );
    lockButton( load, false, blue, black );
    lockButton( setorigin, false, black, white );
    lockButton( connect, false, white, black );
    lockButton( runpreview, false, black, white);
    
    // relabelButton( runpreview, "START PREVIEW" );

}

// RELABEL BUTTON
public void relabelButton(Bang button, String newlabel){
    button.getCaptionLabel().setText(newlabel);
}
public void relabelToggle(Toggle button, String newlabel){
    button.getCaptionLabel().setText(newlabel);
}

// LOCK BUTTON
public void lockButton(Bang button, boolean lock, int c, int t){
    button.setLock(lock)
    .setColorForeground(c)
    .getCaptionLabel().setColor(t);
}

public void highlightButton(Bang button, int c){
    button.setColorActive(c);
}

// LOCK SLIDER
public void lockSlider(Slider sl, boolean lock, int c, int t){
    sl.setLock(lock)
        .setColorForeground(c)
        .getValueLabel().setColor(t);
}
////////////////////////////////////////////////////////////////////////////////
// GCODE
////////////////////////////////////////////////////////////////////////////////

// MANUAL ENTRY
public String manualEntry() {
    return cP5.get(Textfield.class, "cmdEntry").getText().toUpperCase();
}

// G0/G1 - LINE COMMAND
public String gLine(float x, float y, boolean f){
    String cmd = (f) ? "G1" : "G0";
    cmd += "X"+str(x) + "Y"+str(y);
    return cmd;
}

// G2/G3 - ARC COMMANDS
public String gArc(float cx, float cy, float x, float y, boolean dir){
    //clockwise = 2 ... counterclockwise = 3
    if( dir ) return "G2I"+str(cx) + "J"+str(cy) + "X"+str(x) + "Y"+str(y) + "F"+str(PApplet.parseInt(spray_speed));
    else return "G3I" + str(cx) + "J" + str(cy) + "X" + str(x) + "Y" + str(y) + "F"+str(PApplet.parseInt(spray_speed));
}

// G4 - PAUSE COMMAND
public String gDwell( float time ){
    return "G4P" + str(time);
}

// M3 - SPRAY COMMAND
public String gSpray( boolean s ){
    return "M" + ((s) ? "3S" + str(PApplet.parseInt(sprayon*10)) : "5");
}

// Report
public Byte report(){
    return PApplet.parseByte(0x3f);
}

// JOGGING
public String jog(float x, float y){
    String cmd = "G91";
    cmd += gLine(x,y,false);
    return cmd + "\nG90";
}

// SET ORIGIN
public String origin(){
    posx = 0.0f;
    posy = 0.0f;
    return "G10 P1 L20 X0 Y0";
}
// GO HOME
public String home(){
    return gLine(0,0,false);
}

public Byte gPause(){
    return PApplet.parseByte(0x21);
}

public Byte gDoor(){
    return PApplet.parseByte(0x84);
}

public Byte gReset(){
    return PApplet.parseByte(0x18);
}

public Byte gResume(){
    return PApplet.parseByte(0x7e);
}
////////////////////////////////////////////////////////////////////////////////
// FILE I/O
////////////////////////////////////////////////////////////////////////////////

// LOAD FILES
public void loadSingle(){
  selectInput("Select a file to load:", "fileSelected");
}

public void loadFolder(){
  selectFolder("Select a folder of drawings to load:", "folderSelected");
}

public void folderSelected( File f ){
  if( f == null ){
    print("Window closed or user cancelled\n");
    return;
  }
  fp = f.getAbsolutePath();
  print("User selected " + fp + "\n");
  String[] files = listFiles(fp);
  if( files == null || !checkDir(files,((type_gcode)?"txt":"json"))){
    loaded = false;
    fp = "";
    print( ((files==null)?"ERROR--EMPTY OR INVALID DIRECTORY\n":"ERROR--NO JSON DRAWING FILES IN DIRECTORY\n"));
    return;
  }
  loaded = true;
  gcode = (type_gcode) ? processGCODEs( files ) : processJSONs( files );
  if( gcode.size() > 0 ){
    print("DRAWINGS LOADED\n");
    print("GCODE LINES GENERATED: " + gcode.size() + "\n");
    generatePreview(gcode,0,0);
    print("GCODE PREVIEW GENERATED\n");
    saveStrings( "data/gcode.txt", gcode.array() );
  }
  line = 0;
  updateLineSelector();
}

public void fileSelected( File f ){
  if( f == null ){
    print("Window closed or user cancelled\n");
    return;
  }
  fp = f.getAbsolutePath();
  print( "User selected "+fp+"\n");
  loaded = true;
  gcode = (type_gcode) ? processGCODE(fp) : processJSON(fp);
  if(gcode.size() > 0){
    print( "DRAWING LOADED\n");
    print( "GCODE LINES GENERATED: "+gcode.size()+"\n");
    generatePreview(gcode,0,0);
    if(VERBOSE) print("GCODE PREVIEW GENERATED");
    saveStrings( "data/gcode.txt", gcode.array() );
  }
  line = 0;
  updateLineSelector();
}

// LIST FILES IN DIRECTORY
public String[] listFiles( String dir ){
  File file = new File(dir);
  if( file.isDirectory() ){
    return file.list();
  }
  return null;
}

public Boolean fileCheck( String f, String ext ){
  return f.contains(ext);
}

// CHECK FILE EXTENSION
public Boolean checkDir( String[] files, String ext ){
  for( int i = 0; i<files.length; i++){
    if ( fileCheck(files[i], ext) ) return true;
  }
  return false;
}

// PROCESS FILES
public StringList processJSONs( String[] f ){
  StringList g = new StringList(); //clear gcode buffer
  PVector p;

  g.append( gSpray(false) );
  g.append( home() );

  for( int i = 0; i < f.length; i++){
    if( !fileCheck(f[i],"json") ) continue;

    JSONArray coords = loadJSONArray( fp + "\\" + f[i] );

    p = extractPos( coords.getFloat(0), -coords.getFloat(1) );
    g.append( gSpray(false) );
    g.append( gLine( p.x, p.y, false ) );
    g.append( gDwell(0.5f) );
    g.append( gSpray(true) );

    for( int k = 2; k < coords.size(); k+=2 ){
      p = extractPos( coords.getFloat(k),coords.getFloat(k+1) );
      g.append( gLine(p.x, p.y, true) );
    }
    g.append( gSpray(false) );
  }
  g.append( gSpray(false) );
  g.append( home() );

  print("GCODE LINES GENERATED: " + g.size() + "\n");
  return g;
}

public StringList processGCODEs( String[] f ){
  String[] load;
  StringList g = new StringList();
  breaks = new IntList();

  g.append( gSpray(false) );
  g.append( home() );

  for(int i = 0; i < f.length; i++){
    if( !fileCheck(f[i],"txt") ) continue;
    load = loadStrings(fp+"\\"+f[i]);

    for(int k = 0; k < load.length; k++){
      //ignore home commands at beginning & end of file
      if( k <= 3 && load[k].contains("G0X0Y0")) continue;
      if( load[k].length() < 1 ) continue;
      if( k >= load.length-3 && load[k].contains("G0X0Y0")) continue;
      if (load[k].contains("G0X")) breaks.append(g.size());
      g.append(load[k]);
    }
    g.append(gSpray(false));
  }
  g.append( gSpray(false));
  g.append( home() );

  return g;
}

public StringList processJSON( String f ){
  StringList g = new StringList();
  if( !fileCheck(f,"json") ){
    print("ERROR - NOT A JSON FILE\n");
    return g;
  }

  PVector p;
  JSONArray coords = loadJSONArray( f );
  p = extractPos(coords.getFloat(0), -coords.getFloat(1));
  g.append( gSpray(false) );
  g.append( home() );

  g.append( gLine( p.x, p.y, false ) );
  g.append( gDwell(0.5f) );
  g.append( gSpray(true) );

  for( int i = 2; i < coords.size(); i+=2 ){
    p = extractPos( coords.getFloat(i),coords.getFloat(i+1) );
    g.append( gLine(p.x, p.y, true) );
  }

  g.append( gSpray(false) );
  g.append( home() );

  return g;
}

public StringList processGCODE( String f ){
  StringList g = new StringList();
  breaks = new IntList();
  if( !fileCheck(f,"txt") ){
    print("ERROR - NOT A GCODE FILE\n");
    return g;
  }
  String[] load = loadStrings(f);
  g.append( gSpray(false) );
  g.append( home() );

  for (int i = 0; i < load.length; i++){
    if (load[i].contains("G0X")) breaks.append(g.size());
    g.append( load[i] );
  }
  g.append( gSpray(false) );
  g.append( home() );
  return g;
}

public PVector extractPos(float x, float y){
  float x_s = (canvas_width*0.5f)-canvas_margin;
  float y_s = (canvas_height*0.5f)-canvas_margin;
  float x_off = canvas_width*0.5f;
  float y_off = canvas_height*0.5f;

  return new PVector( x_off + x * x_s, y_off + y * y_s );
}
////////////////////////////////////////////////////////////////////////////////
// RENDERING
////////////////////////////////////////////////////////////////////////////////

// INITIALIZE GCODE PREVIEW
public void initPreview(){
    preview = new PShape();
}

// RENDER PREVIEW TO CANVAS
public void renderPreview(){
    if( preview == null ) return;
    if(  previewstate > 0 ) {
        if (!previewing) {
            previewstate = 0;
        }
        updatePreview();
    }
    preview.enableStyle();
    shape(preview, origin.x - (canvas_width*0.5f*scalar), origin.y-(canvas_height*0.5f*scalar));
}

public void updatePreview(){
    if (gcode.size() > 0) {
        generatePreview(gcode, startselect, previewstate);
    }
}

// GENERATE PREVIEW
public void generatePreview(StringList g, int p, int e){
  preview = new PShape();
  PVector last = new PVector(0,0);
  int type;
  int c;
  float o;
  float w;

    if (p > 0) {
        String cmd = g.get(p);
        type = PApplet.parseInt(parseNumber(cmd,"G",-1));
        int o_ = 1;
        while(type < 0 || type > 3) {
            cmd = g.get(p-o_);
            type = PApplet.parseInt(parseNumber(cmd, "G", -1));
            o_++;
        }
        renderLine(last, cmd, blue, 255, 2);
    }

    for(int i = p; i<g.size()-e; i++){
        String cmd = g.get(i);
        type = PApplet.parseInt(parseNumber(cmd,"G",-1));
        if( type < 0 ) continue;

        // COLOR, LINEWEIGHT, OPACITY SETTINGS
        c = (type==0||type==4) ? blue : red;
        w = (type==0||type==4) ? 2 : 2;
        o = (type==0||type==4) ? 85 : 170;

        switch(type){
            case 0:
            case 1:
                renderLine(last, cmd, c, o, w);
                break;
            case 2:
            case 3:
                renderArc(last, cmd, type, c, o, w);
                break;
            case 4:
                renderPoint(last, c, o, w);
                break;
            default:
                break;
        }
    }
}

// RENDER LINE
// Visualizes GCODE line command (G0/G1)
public void renderLine(PVector l, String cmd, int c, float o, float w ){
    PShape ln;
    float x = parseNumber(cmd,"X",l.x);
    float y = parseNumber(cmd,"Y",l.y);
    noFill();
    stroke(c,o);
    strokeWeight(w);
    ln = createShape( LINE, l.x*scalar, l.y*scalar, x*scalar, y*scalar );
    // line(origin.x+l.x*scalar, origin.y-l.y*scalar,origin.x+x*scalar, origin.y-y*scalar);
    preview.addChild( ln );
    l.x = x;
    l.y = y;
}

// RENDER ARC
// Visualizes GCODE arc command (G2/G3)
public void renderArc( PVector l, String cmd, int dir, int c, float o, float w ){
    PShape a;

    float cx = parseNumber(cmd, "I", 0.0f)+l.x;
    float cy = parseNumber(cmd, "J", 0.0f)+l.y;
    float x = parseNumber(cmd, "X", l.x);
    float y = parseNumber(cmd, "Y", l.y);
    float dx2 = l.x - cx;
    float dy2 = l.y - cy;
    float dx1 = x - cx;
    float dy1 = y - cy;

    float r = sqrt( pow(dx1,2) + pow(dy1,2) );

    float SA = TWO_PI - atan2(dy1, dx1);
    float EA = TWO_PI - atan2(dy2, dx2);

    if( dir == 3 && SA > EA){
        EA += TWO_PI;
    } else if( dir == 2 && EA > SA){
        SA += TWO_PI;
    }

    noFill();
    stroke(c,o);
    strokeWeight(w);

    if( dir == 2){
        a = createShape(ARC, cx*scalar, cy*scalar, r*2*scalar, r*2*scalar, EA, SA);
    } else {
        a = createShape(ARC, cx*scalar, cy*scalar, r*2*scalar, r*2*scalar, SA, EA);
    }
    preview.addChild(a);
    l.x = x;
    l.y = y;
}

// RENDER POINT
// Visualizes GCODE dwell command (G4)
public void renderPoint( PVector l, int c, float o, float w){
    PShape p;
    stroke(c,o);
    strokeWeight(w*3);
    p = createShape(POINT, l.x*scalar, l.y*scalar);
    preview.addChild(p);
}
////////////////////////////////////////////////////////////////////////////////
// SERIAL COMMUNICATION
////////////////////////////////////////////////////////////////////////////////

// OPEN SERIAL PORT
public void openSerial(){
    if( portname == null ){
        connected = false;
        return;
    }
    if( port != null ) port.stop();
    try{
        port = new Serial(this, portname, 115200);
        port.bufferUntil('\n');
        connected = true;
    } catch( Exception e){
        closeSerial();
        println("DISCONNECTED: NO SERIAL CONNECTION AVAILABLE");
    }
}

// RESET ALL SERIAL VARIABLES
public void closeSerial(){
    portname = null;
    connected = false;
    port = null;
}

// SELECT SERIAL PORT TO OPEN
public void selectSerial(){
    int s = Serial.list().length;
    if( s == 0 ){
        JOptionPane.showMessageDialog(null, "No Arduino Connected");
        return;
    }
    if( s > 1){
        String result = (String) JOptionPane.showInputDialog(
            null,
            "Select the serial port connected to Arduino",
            "Select serial port",
            JOptionPane.PLAIN_MESSAGE,
            null,
            Serial.list(),
            0
        );
        if( result != null ) portname = result;
    }
    else portname = Serial.list()[0];
    openSerial();
}

// UPDATE SERIAL CONNECTION & CHECK FOR DATA
public void serialRun(){
    if(port.available() > 0){
        String temp = port.readStringUntil('\n');
        if(temp == null) return;
        temp = temp.trim();

        if(temp.matches(CMD_VERSION)){
            if(VERBOSE) print("[STARTUP] "+temp+"\n");
            return;
        }
        if(temp.matches(CMD_STARTUP)){
            if(VERBOSE) print("[STARTUP] "+temp+"\n");
            return;
        }
        if(temp.matches(CMD_STATUS)){
            status = temp;
            extractDim();
            return;
        }
        if(temp.matches(CMD_ERROR)){
            print("[ERROR] " + temp +"\n");
            print("[SENT] " + sent + "\n");
            return;
        }

        if(SIMPLE_MODE){
            if( temp.matches(CMD_OK) ){
                val = temp;
                if(VERBOSE) print("[RX] "+temp+"\n");
                line++;
                completed++;
                timeout = 0;
            }
        }
        else {
            if( temp.matches(CMD_OK)){
                if(VERBOSE) print("[RX] "+temp+"\n");
                val = temp;
                if(c_line.size()>0){
                    c_line.remove(0);
                    completed++;
                }
                timeout = 0;
            }
        }
    }
    //SEND GCODE
    stream();
}

// REQUEST MACHINE POSITION REPORT
public void statusReport(){
    sendByte( report() );
}

// EXTRACT DIMENSIONS FROM MACHINE REPORT
public void extractDim(){
    String[] temp_stat = status.substring(1,status.length()-1).split("\\|");
    //Extract machine status
    idle = temp_stat[0].contains("Idle");
    
    for(int i = 1; i < temp_stat.length; i++){
      String tempVal = temp_stat[i];
      if(tempVal.contains("Pos:")){
        String[] temp_pos = tempVal.substring(5).split(",");
        posx = PApplet.parseFloat(temp_pos[0]);
        posy = PApplet.parseFloat(temp_pos[1]);
      } else if (tempVal.contains("FS:")){
        int servoPos = PApplet.parseInt( tempVal.substring(3).split(",")[1] );
        spraying = (servoPos > 0);
      }
    }
    //Format status message for UX
    status = join(subset(temp_stat,0,min(temp_stat.length,3)), " | ");
}

// SERIAL SEND
public void send( String cmd ){
    if(!connected) return;
    cmd = cmd.trim().replace(" ","");
    sent = cmd;
    port.write(cmd + "\n");

    if( VERBOSE ) print("SENT: " + cmd + '\n');
}

// SERIAL SEND BYTE
public void sendByte( Byte b ){
    if(!connected) return;
    port.write( b );
    if(VERBOSE) sent = str(PApplet.parseChar(b));
}

// RESET SERIAL STREAM STATUS
public void resetStatus(){
    line = 0;
    issued = (startselect > 0 ? startselect-1 : 0);
    completed = (startselect > 0 ? startselect-1 : 0);
    c_line = new IntList();
}

// SERIAL STREAM
public void stream(){
    if(!connected || !streaming) return;

    while(true){
        if( line >= gcode.size() || line < 0 ){
            if( line>0 ){
                print("COMPLETED STREAMING\n");
                //streaming = false;
                line = -1;
            } else if ( c_line.size() == 0 ) {
                print("DRAWING FINISHED\n");
                streaming = false;
                resetStatus();
            }
            return;
        }
        if( gcode.get(line).trim().length() == 0 ){
            line++;
            continue;
        }
        else break;
    }

    String cmd = gcode.get(line).trim().replace(" ","");
    if(swapSpray && cmd.contains("M3S")) cmd = gSpray(true);
    
    if(SIMPLE_MODE){
        if( !lastSent.contains(cmd) ){
            port.write( cmd + "\n" );
            issued++;
            lastSent = cmd;
            print("SENT "+line+": "+cmd+" : ");
            sent = cmd;
        }
    } else {
        if(VERBOSE) print( str(127 - sumList(c_line)) + " BYTES AVAILABLE\n" );
        if( sumList(c_line) + (cmd.length()+1) <= 127 ){
            c_line.append( cmd.length()+1 );
            port.write(cmd + "\n");
            issued++;
            lastSent = cmd;
            line++;
            print("SENT "+line+": "+cmd+"\n");
            sent = cmd;
        }
    }
}
////////////////////////////////////////////////////////////////////////////////
// UI/UX DISPLAY
////////////////////////////////////////////////////////////////////////////////

public void updateCanvasScale() {
    origin = new PVector(600 + (width-600)/2, (height/2)-25);
    float sw_ = (width - 650) / canvas_width;
    float sh_ = (height - 100) / canvas_height;
    scalar = min(sw_, sh_);

    scaleWidth = canvas_width * scalar;
    scaleHeight = canvas_height * scalar;
    scaleMargin = canvas_margin * scalar;

    updatePreview();
}

// DISPLAY UI
public void displayUI() {
    // UI CANVAS DIMENSIONS
    // scaleWidth = canvas_width*scalar;
    // scaleHeight = canvas_height*scalar;
    // scaleMargin = canvas_margin*scalar;

    // SETUP PREVIEW AREA
    // Canvas BG
    noStroke();
    fill(black);
    rect(600, 0, width-600, height);

    pushMatrix();
    rectMode(CENTER);

    // Canvas
    translate(origin.x,origin.y);
    fill(white);
    rect(0,0,scaleWidth,scaleHeight);
    fill(grey);
    rect(0,0,scaleWidth-scaleMargin*2,scaleHeight-scaleMargin*2);

    // Canvas Grid
    noFill();
    stroke(white);
    strokeWeight(1);
    for (float x = 0 ; x < scaleWidth*0.5f; x+=scalar*20) {
        line(x, -scaleHeight*0.5f, x, scaleHeight*0.5f);
        line(-x, -scaleHeight*0.5f, -x, scaleHeight*0.5f);
    }
    for (float y = 0; y < scaleHeight*0.5f; y+=scalar*20) {
        line(-scaleWidth*0.5f, y, scaleWidth*0.5f, y);
        line(-scaleWidth*0.5f, -y, scaleWidth*0.5f, -y);
    }

    // Canvas Frame
    strokeWeight(1);
    rect(0, 0, scaleWidth, scaleHeight);

    rectMode(CORNER);
    popMatrix();

    // MANUAL CONTROLS AREA
    // Controls BG
    noStroke();
    fill(grey);
    rect(0,0,600,900);
    noFill();
    stroke(charcoal);
    strokeWeight(1);
    rect(15,40,320,320);
    // Controls Label
    fill(black);
    textFont(font24,24);
    textAlign(LEFT);
    text("MANUAL CONTROLS", 15, 30);
    // File load Area
    fill(black);
    rect(0,375,590,120);

    // Console area
    fill(black);
    rect(0,500,590,height-500);

    // Loading labels
    fill(white);
    textFont(font14,14);
    textAlign(LEFT);
    text("FILE TYPE",25,435);
    text("LOAD MODE",110,435);

    // Position slider label
    fill(white);
    textFont(font12, 12);
    textAlign(LEFT);
    text("GCODE START POSITION", 25, 490);

}

// RENDER NOZZLE POSITION
public void renderNozzle(){
  pushMatrix();
  //Display Dimensions
  // float scaleWidth = canvas_width*scalar;
  // float scaleHeight = canvas_height*scalar;

  translate(origin.x-scaleWidth*0.5f,origin.y-scaleHeight*0.5f);

  // Nozzle Icon
  stroke( (spraying)?red:blue );
  fill(white,50);
  strokeWeight(3);
  ellipse(posx*scalar,(posy*scalar),10,10);
  noFill();
  strokeWeight(0.5f);
  ellipse(posx*scalar, (posy*scalar),20,20);

  // Nozzle Position Text
  String pos = "( "+nf(posx,0,2)+", "+nf(posy,0,2)+" )";
  rectMode(CENTER);
  noStroke();
  fill(255,100);
  rect(posx*scalar,posy*scalar+20, textWidth(pos)+10,20,10);
  rectMode(CORNER);

  //fill( (spraying) ? red : blue );
  fill(black);
  textFont(font14,14);
  textAlign(CENTER);
  text(pos,(posx*scalar),(posy*scalar) + 24.0f);

  popMatrix();

}

// DISPLAY STATS
public void displayStats(){
  // TX Command
  if(sent != null){
    noStroke();
    fill(green);
    textAlign(LEFT);
    textFont(font24, 24);
    text("TX: "+sent, 15, 610);
  }
  // RX Command
  if(val != null){
    noStroke();
    fill(red);
    textAlign(LEFT);
    textFont(font18, 18);
    text("RX: "+val, 15, 690);
  }

  //COMPLETION
  noStroke();
  fill(white);
  textAlign(LEFT);
  textFont(font18,18);
  text("LINES SENT: "+issued+" / "+gcode.size(), 15, height-50);
  text("COMPLETED: "+completed+" / "+gcode.size(), 15, height-30);

  // Serial Status
  String serial_status;
  textFont(font18,18);
  fill( ((connected) ? green : red) );
  serial_status = (connected) ? "CONNECTED ON " + portname : "NOT CONNECTED";
  text(serial_status, 15, height-10);

  // Machine status
  textFont(font18,18);
  fill( (status.contains("Idle")) ? white : (status.contains("Run")) ? green : red );
  textAlign(CENTER);
  text(status, origin.x, origin.y+(scaleHeight*0.5f)+25.0f);

  // File Selection
  if(fp.length()>0){
    String[] path = fp.split("\\\\");
    int depth = path.length;
    textFont(font18,18);
    fill( white );
    textAlign(LEFT);
    String fpDisplay = join(subset(path, depth-2), "/");
    if(textWidth(fpDisplay) > 250) {
      text(path[depth-2] + "/", 210, 405);
      text(path[depth-1], 210, 425);
    } else {
      text(join(subset(path,depth-2),"/"),210,415);
    }
  }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "drawbot_app" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
