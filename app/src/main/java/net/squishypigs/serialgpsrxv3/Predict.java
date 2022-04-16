package net.squishypigs.serialgpsrxv3;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Predict {
    private List<String> raw_packets = new ArrayList<String>();
    private int number_of_good_packets;
    private String landing_prediction_coords;
    private double[] decoded_rocket_latitudes;
    private double[] decoded_rocket_longitudes;
    private double[] decoded_rocket_altitudes;
    private double user_altitude;
//    private double user_latitude;
//    private double user_longitude;
    private int satellites;
    private double voltage;
    //private Time


    public void setUser_altitude(double user_altitude) {
        this.user_altitude = user_altitude;
    }

    public double getUser_altitude() {
        return user_altitude;
    }

    public Predict(Location loc) {
        // Setter for user location and altitude
        this.user_altitude = loc.getAltitude();
//        this.user_latitude = loc.getLatitude();
//        this.user_longitude = loc.getLongitude();

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

//    public interface MyCallback {
//        // Declaration of the template function for the interface
//        public void updateMyText(String myString);
//    }
}
