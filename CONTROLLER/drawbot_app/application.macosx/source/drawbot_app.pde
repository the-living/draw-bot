 ////////////////////////////////////////////////////////////////////////////////
//                                                                            //
// draw-bot Drawing Robot Platform | The Living | 2018                        //                                                              //
// v5.0 2018.09.27                                                            //
//                                                                            //
////////////////////////////////////////////////////////////////////////////////

// EXTERNAL DEPENDENCIES
//------------------------------------------------------------------------------
import controlP5.*; //ControlP5 - UI Interface
import processing.serial.*; //Serial - Com protocol with Arduino
import javax.swing.JOptionPane; //Interface for COM port selection
import java.awt.event.*;

// GLOBAL VARIABLES
//------------------------------------------------------------------------------
// Debug
Boolean VERBOSE = false; //default: false -- if enabled, print all responses from GRBL
Boolean SIMPLE_MODE = false; //default: false (buffer-fill mode) / true (line-response mode)
int reportFreq = 10; //
String os;
String divider;

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
color black, white, grey, charcoal, green, red, blue;
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
float sprayoff = 0.0;
float sprayon = 28.0;
float spraymax = 100.0;
float pausepen = 0.28;
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
void setup() {
    os = System.getProperty("os.name");
    if(os.contains("Mac")){
        divider = "\\";
    } else {
        divider = "/";
    }
    println("OS: " + os);
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
void draw(){
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
void settings(){
    size(1400, 850);
    smooth();
}

void checkWindow(){
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
void initVariables(){
    // UX
    origin = new PVector(1000,400);
    scalar = 0.5;
    // GCODE
    gcode = new StringList();
    line = 0;
    issued = 0;
    completed = 0;
    startselect = 0;
    breakselect = 0;
    previewstate = 0;
    // MACHINE
    posx = 0.0;
    posy = 0.0;
    status = "[...]";
    spray_speed = 10000.0;
    canvas_width = 1270.0;
    canvas_height = 1270.0;
    canvas_margin = 0.0;
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

void initPatterns(){
    CMD_VERSION = versionPattern.replaceAll(".","[$0]").replace("[*]",".*");
    CMD_STARTUP = startupPattern.replaceAll(".","[$0]").replace("[*]",".*");
    CMD_STATUS = statusPattern.replaceAll(".","[$0]").replace("[*]",".*");
    CMD_OK = okPattern.replaceAll(".","[$0]").replace("[*]",".*");
    CMD_ERROR = errorPattern.replaceAll(".","[$0]").replace("[*]",".*");

}

// PARSE NUMBER FROM GCODE STRING
// Used to extract numerical values from GCode
float parseNumber(String s, String c, float f){
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
    return float( s.substring(start+1,end+1) );
}

// SUMS IntList
// Sums contents of an IntList (used in available buffer tracking)
int sumList(IntList w){
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
void initColors(){
    black = color(0);
    white = color(255);
    grey = color(220);
    charcoal = color(100);
    red = color(237, 28, 36);
    green = color(57, 181, 74);
    blue = color(80, 150, 225);
}

// INIT FONTS
void initFonts(){
    font24 = loadFont("Roboto-Regular-24.vlw");
    font18 = loadFont("Roboto-Regular-18.vlw");
    font16i = loadFont("Roboto-Italic-16.vlw");
    font14 = loadFont("Roboto-Regular-14.vlw");
    font12 = loadFont("Roboto-Regular-12.vlw");
}


// UPDATE DIMENSIONS
void updateDim(){
  String w_ = setwidth.getText();
  String h_ = setheight.getText();
  // String m_ = cP5.get(Textfield.class, "margin").getText();

  canvas_width = (w_ != "" && float(w_)>0) ? float(w_) : canvas_width;
  canvas_height = (h_ != "" && float(h_)>0) ? float(h_) : canvas_height;
  canvas_margin = 0.0;

  updateCanvasScale();
}

// UPDATE PEN POSITION
void updatePenText(){
    String s_ = cP5.get(Textfield.class, "penpos").getText();
    sprayon = (s_ != "" && int(s_)>=0 && int(s_)<=100) ? float(s_) : sprayon;
    cP5.get(Slider.class, "penSlider").setValue(sprayon);
    cP5.get(Textfield.class, "penpos").setText(nfs(sprayon,0,1));
    pausepen = sprayon * 0.01;
    reportPen();
    testPen();
}

void updatePenSlider(){
    Float s_ = cP5.get(Slider.class, "penSlider").getValue();
    cP5.get(Textfield.class, "penpos").setText(nfs(s_,0,1));
    sprayon = s_;
    pausepen = sprayon * 0.01;
    reportPen();
    testPen();
}

void reportPen(){
    print("Pen position: ");
    print(nf(sprayon,0,1));
    print(" | Pause time: ");
    println( nf(pausepen,0,3));
}

void testPen(){
    send(gSpray(true));
    send(gDwell(pausepen));
    send(gSpray(false));
}

// UPDATE SPEED
void updateSpeed(){
  String s_ = cP5.get(Textfield.class, "speed").getText();
  spray_speed = (s_ != "" && float(s_)>0 && float(s_)<=10000) ? float(s_) : spray_speed;
  send("G1F"+str(spray_speed)+"\n");
}

void updateLineSelector(){
    if (breaks.size() > 0){
        startline.setLock(false);
        step_f.setLock(false);
        step_b.setLock(false);
        startline.setMax(max(1,int(breaks.size())));
    }
}

void updatePosition(){
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

PVector findLastPt() {
    String cmd = gcode.get(startselect);
    int type = int(parseNumber(cmd,"G",-1));
    int o_ = 1;
    while(type < 0 || type > 3) {
        cmd = gcode.get(startselect - o_);
        type = int(parseNumber(cmd, "G", -1));
        o_++;
    }
    return new PVector(parseNumber(cmd, "X", 0),parseNumber(cmd, "Y", 0));
}
