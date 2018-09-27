////////////////////////////////////////////////////////////////////////////////
// SERIAL COMMUNICATION
////////////////////////////////////////////////////////////////////////////////

// OPEN SERIAL PORT
void openSerial(){
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
void closeSerial(){
    portname = null;
    connected = false;
    port = null;
}

// SELECT SERIAL PORT TO OPEN
void selectSerial(){
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
void serialRun(){
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
                if(VERBOSE) print("[RX] "+temp+"\n");
                line++;
                completed++;
                timeout = 0;
            }
        }
        else {
            if( temp.matches(CMD_OK)){
                if(VERBOSE) print("[RX] "+temp+"\n");
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
void statusReport(){
    sendByte( report() );
}

// EXTRACT DIMENSIONS FROM MACHINE REPORT
void extractDim(){
    String[] temp_stat = status.substring(1,status.length()-1).split("\\|");
    //Extract machine status
    idle = temp_stat[0].contains("Idle");
    //Extract Work Position
    String[] temp_pos = temp_stat[1].substring(5).split(",");
    posx = float(temp_pos[0]);
    posy = float(temp_pos[1]);
    //Extract Servo Position
    int servoPos = int( temp_stat[3].substring(4).split(",")[1] );
    spraying = (servoPos > 0);
    //Format status message for UX
    status = join(subset(temp_stat,0,4), " | ");
}

// SERIAL SEND
void send( String cmd ){
    if(!connected) return;
    cmd = cmd.trim().replace(" ","");
    sent = cmd;
    port.write(cmd + "\n");

    if( VERBOSE ) print("SENT: " + cmd + '\n');
}

// SERIAL SEND BYTE
void sendByte( Byte b ){
    if(!connected) return;
    port.write( b );
    if(VERBOSE) sent = str(char(b));
}

// RESET SERIAL STREAM STATUS
void resetStatus(){
    line = 0;
    issued = 0;
    completed = 0;
    c_line = new IntList();
}

// SERIAL STREAM
void stream(){
    if(!connected || !streaming) return;

    if( startselect > 0 ) {
        line = startselect;
        PVector goPt = findLastPt();
        String goCmd = gLine(goPt.x, goPt.y, false);
        port.write( goCmd + "\n");
        lastSent = goCmd;
    }


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
