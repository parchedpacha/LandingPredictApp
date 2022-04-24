package net.squishypigs.serialgpsrxv3;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Predict extends Thread implements Runnable{
    private List<String> raw_packets = new ArrayList<String>();
    private String lastpacketString;
    private byte[] newpacket; // TODO does this need to be an arraylist to avoid issues?
    private int newpacketlength;
    private int number_of_good_packets;
    private String landing_prediction_coords;
    private ArrayList<Double> decoded_rocket_latitudes;
    private ArrayList<Double> decoded_rocket_longitudes;
    private ArrayList<String> decoded_rocket_datestamps;
    private ArrayList<Double> decoded_rocket_altitudes;
    private ArrayList<Integer> decoded_rocket_satellites;
    private ArrayList<Double> decoded_rocket_voltages;
    private final String[] check_letters={"A","O","T","H","S","V"};
    private UsbSerialPort port;
    private double user_altitude;
//    private double user_latitude;
//    private double user_longitude;
    private int satellites;
    private double voltage;
    //private Time
    public void read_one_time() {  // might rename this, whatever to get it working
        //This method needs to run on a new thread, it will watch the serial connection for new
        // bytes and dispatch the parsePacket method to update the Predict class's data and send
        // it to the user. Additionally the extrapolate method will be called to generate the
        // expected landing coordinates and that will also be displayed to the user.

        if (port !=null && port.isOpen()) {
            try {
                newpacketlength=port.read(newpacket,500);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parsePacket();

        }
    }
    public void parsePacket() { //this method parses a given packet for our data, and if its good, updates our list of data
        //A42.558071  ,O-83.180877 ,T12:00:19,H02000.9,S13,V8.12
        // A sample packet, lAt, lOng, Time, Height, Satellites, Voltage
        // TODO make sure the string by this point is split into individual packets
        lastpacketString = new String(newpacket, StandardCharsets.UTF_8);
        String[] parts=lastpacketString.split("[,]",0);
        boolean[] bad_fields = {false,false,false,false,false,false};

        for (int i=0;i<parts.length;i++) { //check our strings to make sure each has the correct letter, and mark if otherwise
            if (!parts[i].contains((check_letters[i]))) {
                bad_fields[i]=true;
            }
        }
        if (!any(bad_fields)) { // this is run if any of the fields don't contain their field letter
            StringTokenizer telemetryTokens=new StringTokenizer(lastpacketString,",");
            if (telemetryTokens.countTokens() == check_letters.length) { //make sure we have as many teleetry numbers as we expect before trying to store their values into our pseudo database
                decoded_rocket_latitudes.add(Double.parseDouble(telemetryTokens.nextToken()));
                decoded_rocket_longitudes.add(Double.parseDouble(telemetryTokens.nextToken()));
                decoded_rocket_datestamps.add(telemetryTokens.nextToken()); //TODO parse this time of format T12:00:19 into the java date format for math reasons
                decoded_rocket_altitudes.add(Double.parseDouble(telemetryTokens.nextToken()));
            }

        }

    }
    private boolean any(boolean[] myBooleanArray) {
        for (boolean value : myBooleanArray) {
            if (value) {
                return true;
            }
        }
        return false;
    }
    public void setUser_altitude(double user_altitude) {
        this.user_altitude = user_altitude;
    }

    public double getUser_altitude() {
        return user_altitude;
    }

    public Predict(Location loc) {
        // Setter for user location and altitude when I figure out how tf i am gonna do that
        this.user_altitude = loc.getAltitude();
    }
    public Predict(Double altitude){
        this.user_altitude = altitude;
    }
    public Predict() {
        //the void case, for making the class exist, then updating it all later
    }

    public void addPacket( String raw_packet) {
        // Adds a packet to our list of packets, and begins a new prediction
        this.raw_packets.add( raw_packet);
        // TODO add function to convert raw packets into list of coords and altitudes

        // regex or something to convert/tokenize the string into individual values


        // append those values onto our lists of altitudes, voltages, longitides, latitudes

        // call the extrapolate function to update the prediction
    }

    private void extrapolate() {
        // TODO add math to extrapolate data points down to ground level, and set our current landing prediction

        // times

        // lats

        //longs

        //alts

        //user alt

        // function of lat output vs altitude input

        //function of long output vs altitude input

        // run both functions at lat = 0, or alt = user alt


        // set values as our landing prediction

        //maybe set a UI indicator showing that packets have been recieved, decoded, and that our prediction is good and ready for use
    }

    public String getLanding_prediction_coords() {
        return landing_prediction_coords;
    }
}
