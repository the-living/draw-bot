////////////////////////////////////////////////////////////////////////////////
// FILE I/O
////////////////////////////////////////////////////////////////////////////////

// LOAD FILES
void loadSingle(){
  selectInput("Select a file to load:", "fileSelected");
}

void loadFolder(){
  selectFolder("Select a folder of drawings to load:", "folderSelected");
}

void folderSelected( File f ){
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

void fileSelected( File f ){
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
String[] listFiles( String dir ){
  File file = new File(dir);
  if( file.isDirectory() ){
    return file.list();
  }
  return null;
}

Boolean fileCheck( String f, String ext ){
  return f.contains(ext);
}

// CHECK FILE EXTENSION
Boolean checkDir( String[] files, String ext ){
  for( int i = 0; i<files.length; i++){
    if ( fileCheck(files[i], ext) ) return true;
  }
  return false;
}

// PROCESS FILES
StringList processJSONs( String[] f ){
  StringList g = new StringList(); //clear gcode buffer
  PVector p;

  g.append( gSpray(false) );
  g.append( gDwell(pausepen) );
  g.append( home() );

  for( int i = 0; i < f.length; i++){
    if( !fileCheck(f[i],"json") ) continue;

    JSONArray coords = loadJSONArray( fp + "\\" + f[i] );

    p = extractPos( coords.getFloat(0), -coords.getFloat(1) );
    g.append( gSpray(false) );
    g.append( gDwell(pausepen) );
    g.append( gLine( p.x, p.y, false ) );
    g.append( gDwell(0.5) );
    g.append( gSpray(true) );
    g.append( gDwell(pausepen) );

    for( int k = 2; k < coords.size(); k+=2 ){
      p = extractPos( coords.getFloat(k),coords.getFloat(k+1) );
      g.append( gLine(p.x, p.y, true) );
    }
    g.append( gSpray(false) );
    g.append( gDwell(pausepen) );
  }
  g.append( gSpray(false) );
  g.append( gDwell(pausepen) );
  g.append( home() );

  print("GCODE LINES GENERATED: " + g.size() + "\n");
  return g;
}

StringList processGCODEs( String[] f ){
  String[] load;
  StringList g = new StringList();
  breaks = new IntList();

  g.append( gSpray(false) );
  g.append( gDwell(pausepen));
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
    g.append( gDwell(pausepen));
  }
  g.append( gSpray(false));
  g.append( gDwell(pausepen));
  g.append( home() );

  return g;
}

StringList processJSON( String f ){
  StringList g = new StringList();
  if( !fileCheck(f,"json") ){
    print("ERROR - NOT A JSON FILE\n");
    return g;
  }

  PVector p;
  JSONArray coords = loadJSONArray( f );
  p = extractPos(coords.getFloat(0), -coords.getFloat(1));
  g.append( gSpray(false) );
  g.append( gDwell(pausepen) );
  g.append( home() );

  g.append( gLine( p.x, p.y, false ) );
  g.append( gDwell(0.5) );
  g.append( gSpray(true) );
  g.append( gDwell(pausepen) );

  for( int i = 2; i < coords.size(); i+=2 ){
    p = extractPos( coords.getFloat(i),coords.getFloat(i+1) );
    g.append( gLine(p.x, p.y, true) );
  }

  g.append( gSpray(false) );
  g.append( gDwell(pausepen) );
  g.append( home() );

  return g;
}

StringList processGCODE( String f ){
  StringList g = new StringList();
  breaks = new IntList();
  if( !fileCheck(f,"txt") ){
    print("ERROR - NOT A GCODE FILE\n");
    return g;
  }
  String[] load = loadStrings(f);
  g.append( gSpray(false) );
  g.append( gDwell(pausepen) );
  g.append( home() );

  for (int i = 0; i < load.length; i++){
    if (load[i].contains("G0X")) breaks.append(g.size());
    g.append( load[i] );
  }
  g.append( gSpray(false) );
  g.append( gDwell(pausepen) );
  g.append( home() );
  return g;
}

PVector extractPos(float x, float y){
  float x_s = (canvas_width*0.5)-canvas_margin;
  float y_s = (canvas_height*0.5)-canvas_margin;
  float x_off = canvas_width*0.5;
  float y_off = canvas_height*0.5;

  return new PVector( x_off + x * x_s, y_off + y * y_s );
}
