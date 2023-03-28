package net.squishypigs.serialgpsrxv3;

import android.location.Location;
import android.util.Log;


import com.google.common.primitives.Doubles;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Predict extends Thread implements Runnable{
    private List<String> raw_packets = new ArrayList<String>();
    private byte[] bytebuffer;
    private String lastpacketString;
    private String newpacket;

    private int newpacketlength;
    private int number_of_good_packets=0;
    private String landing_prediction_coords;
    private ArrayList<Double> decoded_rocket_latitudes = new ArrayList<Double>();
    private ArrayList<Double> decoded_rocket_longitudes = new ArrayList<Double>();
    private ArrayList<String> decoded_rocket_datestamps = new ArrayList<String>();
    private ArrayList<Double> decoded_rocket_altitudes = new ArrayList<Double>();
    private ArrayList<Integer> decoded_rocket_satellites = new ArrayList<Integer>();
    private ArrayList<Double> decoded_rocket_voltages = new ArrayList<Double>();
    private final String[] check_letters={"A","O","T","H","S","V"};
    private UsbSerialPort port;
    private double user_altitude;
//    private double user_latitude;
//    private double user_longitude;
    private int satellites;
    private double voltage;
    //private Time
    public String getLanding_prediction_coords() {
        return landing_prediction_coords;
    }
    public void setUser_altitude(double user_altitude) {
        this.user_altitude = user_altitude;
    }
    public double getUser_altitude() {
        return user_altitude;
    }

    public List<String> getRaw_packets() {

        return raw_packets;
    }
    public String getNiceData() {
        //String lastLong = new String(String.valueOf(decoded_rocket_longitudes.get(decoded_rocket_longitudes.size() - 1)));
        if (decoded_rocket_latitudes.size() - 1 >=0) {
            return new String(String.valueOf(decoded_rocket_latitudes.get(decoded_rocket_latitudes.size() - 1)));// + " " +lastLong;
        } else {
            return "";
        }
    }
    public void read_one_time() {  // might rename this, whatever to get it working
        //This method needs to run on a new thread, it will watch the serial connection for new
        // bytes and dispatch the parsePacket method to update the Predict class's data and send
        // it to the user. Additionally the extrapolate method will be called to generate the
        // expected landing coordinates and that will also be displayed to the user.

        if (port !=null && port.isOpen()) {
            try {
                newpacketlength=port.read(bytebuffer,500);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //swap these out with add packet to encapsulate the whole process
            collectRawBytes(bytebuffer);
            //addPacket();

        }
    }

    /**
     *
     * @param data incoming byte data
     */
    public void collectRawBytes (byte[] data) { //accept incoming bytes into the buffer
        String incoming = new String(data, StandardCharsets.UTF_8);
        newpacket = newpacket + incoming; //add new chars onto our buffer
        if (newpacket.contains("\n")) { //if our buffer has a newline, then it has one complete packet, probably
            if (newpacket.endsWith("\n")) { //if it ends on that newline, clear the buffer
                raw_packets.add(newpacket);
                newpacket="";
            }else{ //if we have more thant the endline, then split on it, save both ends
                String[] split_packets = newpacket.split("\n");
                raw_packets.add(split_packets[0]);
                newpacket = split_packets[1];

            }
            parsePacket();
            if (number_of_good_packets > 1) {
            extrapolate();
        }
        }
    }

    public void parsePacket() { //this method parses a given packet for our data, and if its good, updates our list of data
        //A42.558071  ,O-83.180877 ,T12:00:19,H02000.9,S13,V8.12
        // A sample packet, lAt, lOng, Time, Height, Satellites, Voltage
        // TODO make sure the string by this point is split into individual packets
        lastpacketString = raw_packets.get(raw_packets.size()-1);
        String[] parts=lastpacketString.split("[,]",0);
        boolean[] bad_fields = {false,false,false,false,false,false};

        for (int i=0;i<parts.length;i++) { //check our strings to make sure each has the correct letter, and mark if otherwise
            if (!parts[i].contains((check_letters[i]))) {
                bad_fields[i]=true;
            }
        }
        if (!any(bad_fields)) { // this is run if any of the fields don't contain their field letter
            StringTokenizer telemetryTokens=new StringTokenizer(lastpacketString,","); //substring is used to remove the identification character
            if (telemetryTokens.countTokens() == check_letters.length) { //make sure we have as many telemetry numbers as we expect before trying to store their values into our pseudo database

                Log.w("Predict",lastpacketString);
                number_of_good_packets++;
                // TODO Fix the decoding, its off by several orders of magnitude, maybe try to put these values in a string then send to the textview?
                decoded_rocket_latitudes.add(Double.parseDouble(telemetryTokens.nextToken().substring(5))); // null at the beginning so start at index 5

                decoded_rocket_longitudes.add(Double.parseDouble(telemetryTokens.nextToken().substring(1)));
                decoded_rocket_datestamps.add(telemetryTokens.nextToken().substring(1)); //TODO parse this time of format T12:00:19 into the java date format for math reasons
                decoded_rocket_altitudes.add(Double.parseDouble(telemetryTokens.nextToken().substring(1)));
                decoded_rocket_satellites.add(Integer.parseInt(telemetryTokens.nextToken().substring(1)));
                decoded_rocket_voltages.add(Double.parseDouble(telemetryTokens.nextToken().substring(1)));
            }
        }
        // ast this point we have completely parsed the packet and assigned its contents to our respective variables. Now move onto the extrapolate function
    }


    /**
     *
     * @param loc Current User Location
     */
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

    /**
     * \
     * @param locations x coordinates, distances
     * @param altitudes y coordinates, altitudes
     * @param user_altitude user y altitude
     * @return landing location
     */
    private double linear_interp (double[] locations, double[] altitudes, double user_altitude) {
        //this function generates a slope
        double slope = 0; //create a variable to hold our calculated slopes

        for (int i=0;i< locations.length-1;i++) {
            slope +=  (altitudes[i+1]-altitudes[i])/(locations[i+1]-locations[i] );
        }

        slope=slope/(locations.length-1);


        return (altitudes[altitudes.length-1] - user_altitude)/slope;




    }

    private void extrapolate() {

        double landingLat=linear_interp(Doubles.toArray(decoded_rocket_latitudes), Doubles.toArray(decoded_rocket_altitudes),user_altitude);
        double landingLong=linear_interp(Doubles.toArray(decoded_rocket_longitudes), Doubles.toArray(decoded_rocket_altitudes),user_altitude);
        // set values as our landing prediction
        final DecimalFormat df = new DecimalFormat("00.00000");
        landing_prediction_coords = df.format(landingLat) + ", " + df.format(landingLong);
        //maybe set a UI indicator showing that packets have been recieved, decoded, and that our prediction is good and ready for use
    }



    private boolean any(boolean[] myBooleanArray) {
        for (boolean value : myBooleanArray) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    }
