package net.squishypigs.serialgpsrxv3;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.text.format.Time;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Predict extends Thread implements Runnable{
    private List<String> raw_packets = new ArrayList<String>();
    private String lastpacketString;
    private byte[] newpacket;
    private int number_of_good_packets;
    private String landing_prediction_coords;
    private double[] decoded_rocket_latitudes;
    private double[] decoded_rocket_longitudes;
    private double[] decoded_rocket_altitudes;
    private UsbSerialPort port;
    private double user_altitude;
//    private double user_latitude;
//    private double user_longitude;
    private int satellites;
    private double voltage;
    //private Time
    public void run() {  // might rename this, whatever to get it working
        //This method needs to run on a new thread, it will watch the serial connection for new
        // bytes and dispatch the parsePacket method to update the Predict class's data and send
        // it to the user. Additionally the extrapolate method will be called to generate the
        // expected landing coordinates and that will also be displayed to the user.

        while (port !=null && port.isOpen()) {
            try {
                port.read(newpacket,500);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parsePacket();

        }
    }
    public void parsePacket() {
        lastpacketString = new String(newpacket, StandardCharsets.UTF_8);

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
