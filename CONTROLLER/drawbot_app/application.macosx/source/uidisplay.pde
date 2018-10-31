////////////////////////////////////////////////////////////////////////////////
// UI/UX DISPLAY
////////////////////////////////////////////////////////////////////////////////

void updateCanvasScale() {
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
void displayUI() {
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
    for (float x = 0 ; x < scaleWidth*0.5; x+=scalar*20) {
        line(x, -scaleHeight*0.5, x, scaleHeight*0.5);
        line(-x, -scaleHeight*0.5, -x, scaleHeight*0.5);
    }
    for (float y = 0; y < scaleHeight*0.5; y+=scalar*20) {
        line(-scaleWidth*0.5, y, scaleWidth*0.5, y);
        line(-scaleWidth*0.5, -y, scaleWidth*0.5, -y);
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
void renderNozzle(){
  pushMatrix();
  //Display Dimensions
  // float scaleWidth = canvas_width*scalar;
  // float scaleHeight = canvas_height*scalar;

  translate(origin.x-scaleWidth*0.5,origin.y-scaleHeight*0.5);

  // Nozzle Icon
  stroke( (spraying)?red:blue );
  fill(white,50);
  strokeWeight(3);
  ellipse(posx*scalar,(posy*scalar),10,10);
  noFill();
  strokeWeight(0.5);
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
  text(pos,(posx*scalar),(posy*scalar) + 24.0);

  popMatrix();

}

// DISPLAY STATS
void displayStats(){
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
  text(status, origin.x, origin.y+(scaleHeight*0.5)+25.0);

  // File Selection
  if(fp.length()>0){
    //println(fp);
    String[] path = fp.split(os.contains("Windows") ? "\\\\" : "/");
    //println(path);
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
