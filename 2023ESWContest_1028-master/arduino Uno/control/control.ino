int Mlrdirpin = 7; //Motor X direction pin
int Mlrsteppin = 6; //Motor X step pin
int Mlren=8; //Motor X enable pin
int Mfbdirpin = 4; //Motor Y direction pin
int Mfbsteppin = 5; //Motor Y step pin
int Mfben=12; //Motor Y enable pin

int lightonoff = 3;

char signal;

void setup() {
  pinMode(Mlrdirpin,OUTPUT);
  pinMode(Mlrsteppin,OUTPUT);
  pinMode(Mlren,OUTPUT);
  pinMode(Mfbdirpin,OUTPUT);
  pinMode(Mfbsteppin,OUTPUT);
  pinMode(Mfben,OUTPUT);

  pinMode(lightonoff,OUTPUT);
  
  digitalWrite(Mlren,HIGH);// Low Level Enable
  digitalWrite(Mfben,HIGH);// Low Level Enable
  
  digitalWrite(Mlrdirpin,HIGH);
  digitalWrite(Mfbdirpin,HIGH);
  
  Serial.begin(9600); 
}

//set spin LOW or HIGH
void motorset() {
  //left or right
  if(signal == 'l'){
    digitalWrite(Mlren,LOW);
    digitalWrite(Mlrdirpin,LOW);
    digitalWrite(Mfben,HIGH);
  }
  else if (signal == 'r'){
    digitalWrite(Mlren,LOW);
    digitalWrite(Mlrdirpin,HIGH);
    digitalWrite(Mfben,HIGH);
  }

  //forward or backward
  else if (signal == 'f'){
    digitalWrite(Mfben,LOW);
    digitalWrite(Mfbdirpin,LOW);
    digitalWrite(Mlren,HIGH);
  }
  else if (signal == 'b'){
    digitalWrite(Mfben,LOW);
    digitalWrite(Mfbdirpin,HIGH);
    digitalWrite(Mlren,HIGH);
  }

  //stop
  else if (signal == 's'){
    digitalWrite(Mlren,HIGH);
    digitalWrite(Mfben,HIGH);
  }  
}

//on or off
void onoff() {
  if (signal == 'o') {
    digitalWrite(lightonoff, HIGH);
  }
  else if (signal == 'x') {
    digitalWrite(lightonoff, LOW);
  }
}

void loop() {
  int j;

  int MlrdirState = digitalRead(Mlrdirpin);
  int MfbdirState = digitalRead(Mfbdirpin);
  
  if (Serial.available()){
    signal = Serial.read();
    Serial.println(signal);
    motorset();
  }
  
  digitalWrite(Mlrsteppin,LOW); 
  digitalWrite(Mfbsteppin,LOW);
  delay(1); // low -> fast
  digitalWrite(Mlrsteppin,HIGH); //Rising step
  digitalWrite(Mfbsteppin,HIGH);  
  
  onoff();
}
