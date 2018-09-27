////////////////////////////////////////////////////////////////////////////////
// RENDERING
////////////////////////////////////////////////////////////////////////////////

// INITIALIZE GCODE PREVIEW
void initPreview(){
    preview = new PShape();
}

// RENDER PREVIEW TO CANVAS
void renderPreview(){
    if( preview == null ) return;
    if(  previewstate > 0 ) {
        if (!previewing) {
            previewstate = 0;
        }
        updatePreview();
    }
    preview.enableStyle();
    shape(preview, origin.x - (canvas_width*0.5*scalar), origin.y-(canvas_height*0.5*scalar));
}

void updatePreview(){
    if (gcode.size() > 0) {
        generatePreview(gcode, startselect, previewstate);
    }
}

// GENERATE PREVIEW
void generatePreview(StringList g, int p, int e){
  preview = new PShape();
  PVector last = new PVector(0,0);
  int type;
  color c;
  float o;
  float w;

    if (p > 0) {
        String cmd = g.get(p);
        type = int(parseNumber(cmd,"G",-1));
        int o_ = 1;
        while(type < 0 || type > 3) {
            cmd = g.get(p-o_);
            type = int(parseNumber(cmd, "G", -1));
            o_++;
        }
        renderLine(last, cmd, blue, 255, 2);
    }

    for(int i = p; i<g.size()-e; i++){
        String cmd = g.get(i);
        type = int(parseNumber(cmd,"G",-1));
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
void renderLine(PVector l, String cmd, color c, float o, float w ){
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
void renderArc( PVector l, String cmd, int dir, color c, float o, float w ){
    PShape a;

    float cx = parseNumber(cmd, "I", 0.0)+l.x;
    float cy = parseNumber(cmd, "J", 0.0)+l.y;
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
void renderPoint( PVector l, color c, float o, float w){
    PShape p;
    stroke(c,o);
    strokeWeight(w*3);
    p = createShape(POINT, l.x*scalar, l.y*scalar);
    preview.addChild(p);
}
