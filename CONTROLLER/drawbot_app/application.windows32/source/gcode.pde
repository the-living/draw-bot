////////////////////////////////////////////////////////////////////////////////
// GCODE
////////////////////////////////////////////////////////////////////////////////

// MANUAL ENTRY
String manualEntry() {
    return cP5.get(Textfield.class, "cmdEntry").getText().toUpperCase();
}

// G0/G1 - LINE COMMAND
String gLine(float x, float y, boolean f){
    String cmd = (f) ? "G1" : "G0";
    cmd += "X"+str(x) + "Y"+str(y);
    return cmd;
}

// G2/G3 - ARC COMMANDS
String gArc(float cx, float cy, float x, float y, boolean dir){
    //clockwise = 2 ... counterclockwise = 3
    if( dir ) return "G2I"+str(cx) + "J"+str(cy) + "X"+str(x) + "Y"+str(y) + "F"+str(int(spray_speed));
    else return "G3I" + str(cx) + "J" + str(cy) + "X" + str(x) + "Y" + str(y) + "F"+str(int(spray_speed));
}

// G4 - PAUSE COMMAND
String gDwell( float time ){
    return "G4P" + nf(time,0,3);
}

// M3 - SPRAY COMMAND
String gSpray( boolean s ){
    return "M" + ((s) ? "3S" + str(int(sprayon*10)) : "3S0");
    //return "M" + ((s) ? "3S" + str(int(sprayon*10)) : "5");
}

// Report
Byte report(){
    return byte(0x3f);
}

// JOGGING
String jog(float x, float y){
    String cmd = "G91";
    cmd += gLine(x,y,false);
    return cmd + "\nG90";
}

// SET ORIGIN
String origin(){
    posx = 0.0;
    posy = 0.0;
    return "G10 P1 L20 X0 Y0";
}
// GO HOME
String home(){
    return gLine(0,0,false);
}

Byte gPause(){
    return byte(0x21);
}

Byte gDoor(){
    return byte(0x84);
}

Byte gReset(){
    return byte(0x18);
}

Byte gResume(){
    return byte(0x7e);
}
