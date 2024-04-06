#include <PrintEx.h>
/*
  SerialPassthrough sketch

  Some boards, like the Arduino 101, the MKR1000, Zero, or the Micro, have one
  hardware serial port attached to Digital pins 0-1, and a separate USB serial
  port attached to the IDE Serial Monitor. This means that the "serial
  passthrough" which is possible with the Arduino UNO (commonly used to interact
  with devices/shields that require configuration via serial AT commands) will
  not work by default.

  This sketch allows you to emulate the serial passthrough behaviour. Any text
  you type in the IDE Serial monitor will be written out to the serial port on
  Digital pins 0 and 1, and vice-versa.

  On the 101, MKR1000, Zero, and Micro, "Serial" refers to the USB Serial port
  attached to the Serial Monitor, and "Serial1" refers to the hardware serial
  port attached to pins 0 and 1. This sketch will emulate Serial passthrough
  using those two Serial ports on the boards mentioned above, but you can change
  these names to connect any two serial ports on a board that has multiple ports.

  created 23 May 2016
  by Erik Nyquist
*/
const long Lat1 =  42558172;
const long Lon1 = -83181356;
const long Lat2 =  42557864;
const long Lon2 = -83179865;
const long altmax = 150+330; // Starting height in meters
const long altmin = 150; // ending height in meters
int sats = 13;
float Calt, Clat, Clon, Volts;
//char output [65];// = "                                                                ";

PrintEx myPrint = Serial;
void setup() {
  
  Serial.begin(9600);

}

void loop() {

  for (int i = 1; i <= 60; i++) {

    Clat = (float)map(i, 1, 60, Lat1, Lat2) / 1000000.0; //generate fake gps points from starting to ending, for testing my RX setup
    Clon = (float)map(i, 1, 60, Lon1, Lon2) / 1000000.0;
    Volts = (float)map(i, 1, 60, 840, 750) /100.0;
    Calt = (float) map(i, 1, 60, altmax*10, altmin*10)/10.0 +random(-3,4);
    
    myPrint.printf("%-011.6f,%-011.6f,12:00:%02.0d,%-07.1f,%02.0d,%04.2f\n",Clat,Clon,i-1,Calt,sats,Volts);

    delay(1000);
  }

}
