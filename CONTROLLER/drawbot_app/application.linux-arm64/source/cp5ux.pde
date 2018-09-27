////////////////////////////////////////////////////////////////////////////////
// CONTROLP5 CONTROLS
////////////////////////////////////////////////////////////////////////////////


// SET UP CP5 UX CONTROLS
void setupControls() {
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
                breakselect = int(startline.getValue());
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
void controlEvent( ControlEvent theEvent ) {
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
void checkStatus(){

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
void relabelButton(Bang button, String newlabel){
    button.getCaptionLabel().setText(newlabel);
}
void relabelToggle(Toggle button, String newlabel){
    button.getCaptionLabel().setText(newlabel);
}

// LOCK BUTTON
void lockButton(Bang button, boolean lock, color c, color t){
    button.setLock(lock)
    .setColorForeground(c)
    .getCaptionLabel().setColor(t);
}

void highlightButton(Bang button, color c){
    button.setColorActive(c);
}

// LOCK SLIDER
void lockSlider(Slider sl, boolean lock, color c, color t){
    sl.setLock(lock)
        .setColorForeground(c)
        .getValueLabel().setColor(t);
}
