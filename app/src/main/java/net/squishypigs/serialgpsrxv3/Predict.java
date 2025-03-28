package net.squishypigs.serialgpsrxv3;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.primitives.Doubles;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
public class Predict extends Thread implements Runnable{
    private List<String> raw_packets = new ArrayList<>();

    private String newpacket;
    private int number_of_good_packets=0;
    private String landing_prediction_coords;
    private final int packets_to_use=10;
    private ArrayList<Double> decoded_rocket_latitudes = new ArrayList<>();
    private ArrayList<Double> decoded_rocket_longitudes = new ArrayList<>();
    private ArrayList<String> decoded_rocket_datestamps = new ArrayList<>();
    private ArrayList<Double> recieved_timestamps = new ArrayList<>();
    private ArrayList<Double> decoded_rocket_altitudes = new ArrayList<>();
    private ArrayList<Integer> decoded_rocket_satellites = new ArrayList<>();
    private ArrayList<Double> decoded_rocket_voltages = new ArrayList<>();
    private final String[] check_letters={"A","O","T","H","P","V"};//lAt,lOng,Time,Height,HDOP,Voltage <-- in future ill change satellites to HDOP
    private DataStore datastore;
    private int last_packet_quality = 0; //0 = no packet, 1 = bad packet, 2 = good packet, 3 == in bootup
    private long last_packet_time = 0; //record timestamp of last good packet
    private long startup_time=0;
    // Get SharedPreferences object


    private double user_altitude,descent_rate;
    private boolean onGround=false;

    public Predict(Double altitude,DataStore datastore){
        this.user_altitude = altitude;
        this.datastore=datastore;
        this.startup_time=System.currentTimeMillis();
    }

    @Override
    public void run() {

    }

    private void saveLast10Packets() {
        datastore.saveString("last10packets",getLastPackets());
    }
    private String returnLastPackets() {
        return datastore.getString("last10packets","No Saved Packets");
    }

    public int get_last_packet_quality() {
        boolean inBootup = System.currentTimeMillis() - startup_time < 2000;
        boolean last_packet_expired = (System.currentTimeMillis()- last_packet_time) > 2000;
        Log.i("packet_conditions","inBootup"+String.valueOf(inBootup) + "  expired:" + String.valueOf(last_packet_expired));
        if (inBootup) {
            return 3; // in bootup number
        }else if (!last_packet_expired ) {// if time since last good packet is less than 2 seconds
            return last_packet_quality;
        } else {
            return 0; // say we recieved no packet at all if it has been too long
        }
    }

    public String getDescentRate() {
        final DecimalFormat df = new DecimalFormat("###.0");
        return (df.format(descent_rate*-1) + " m/sec");
    }

    public boolean getOnGround() {
        return onGround;
    }
    public void resetPredict() {
        decoded_rocket_latitudes = new ArrayList<>();
        decoded_rocket_longitudes = new ArrayList<>();
        decoded_rocket_datestamps = new ArrayList<>();
        recieved_timestamps = new ArrayList<>();
        decoded_rocket_altitudes = new ArrayList<>();
        decoded_rocket_satellites = new ArrayList<>();
        decoded_rocket_voltages = new ArrayList<>();
        descent_rate=0;
        number_of_good_packets=0;
        raw_packets = new ArrayList<>();
        Log.i("Predict", "RESET");
    }
//    public void check_auto_reset() {
//        double current_alt= decoded_rocket_altitudes.get(decoded_rocket_altitudes.size()-1);
//        double last_alt= decoded_rocket_altitudes.get(decoded_rocket_altitudes.size()-2);
//        if ( (current_alt - last_alt) > 200) {
//            resetPredict();
//        }
//    }
    public String getLastPackets() {

        if (!raw_packets.isEmpty()) {
            int end = raw_packets.size() - 1;
            int start = end - packets_to_use;

            if (start < 0) {
                start = 0;
            }
            ArrayList<String> packets = new ArrayList<>(raw_packets.subList(start, end));
            String ReturnPackets=packets.toString();
            ReturnPackets=ReturnPackets.replace("[","");
            ReturnPackets=ReturnPackets.replace("]","");
            ReturnPackets=ReturnPackets.replace("\n, ","\n");
            ReturnPackets =ReturnPackets.replace("null","");

            return ReturnPackets;
        }
        else if (!returnLastPackets().isEmpty()) {
            return returnLastPackets();

        } else {
            return "No Saved Packets";
        }
    }
    public String getLanding_prediction_coords() {
        if ( Math.abs(descent_rate) > 0.5) {
            //Log.i("Predict", "Good Coords");
            onGround=false;
            return landing_prediction_coords;
        }
        else if(!decoded_rocket_latitudes.isEmpty()) {
            int i = decoded_rocket_latitudes.size()-1;
            //Log.i("Predict","Landed Coords");
            onGround=true;
            return decoded_rocket_latitudes.get(i) + ", "+decoded_rocket_longitudes.get(i);
        }
        else {
            //Log.i("Predict","No Data");
            return "No Data";
        }
    }
    public void setUserLocation(Location location) {
        this.setUser_altitude(location.getAltitude());
    }
    public void setUser_altitude(double user_altitude) {
        this.user_altitude = user_altitude;
    }

//    public double getUser_altitude() {
//        return user_altitude;
//    }

//    public List<String> getRaw_packets() {
//
//        return raw_packets;
//    }
//    public String getNiceData() {
//        //String lastLong = new String(String.valueOf(decoded_rocket_longitudes.get(decoded_rocket_longitudes.size() - 1)));
//        if (decoded_rocket_latitudes.size() - 1 >=0) {
//            return  String.valueOf(decoded_rocket_latitudes.get(decoded_rocket_latitudes.size() - 1));// + " " +lastLong;
//        } else {
//            return "";
//        }
//    }


    /** This function Runs the show
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
                saveLast10Packets();
                //last_packet_quality = 2;
            }else{ //if we have more thant the endline, then split on it, save both ends
                String[] split_packets = newpacket.split("\n");
                raw_packets.add(split_packets[0]);
                newpacket = split_packets[1];
                saveLast10Packets();
                //last_packet_quality = 2;
            }
            parsePacket();
            last_packet_time = System.currentTimeMillis(); // record timestamp
            Log.i("last_packet_time",String.valueOf(last_packet_time));
            if (number_of_good_packets > 1) {
            extrapolate();
            //check_auto_reset();
        }
        }
    }

    public void parsePacket() { //this method parses a given packet for our data, and if its good, updates our list of data
        //A42.558071  ,O-83.180877 ,T12:00:19,H02000.9,S13,V8.12
        // A sample packet, lAt, lOng, Time, Height, Satellites, Voltage
        // TODO make sure the string by this point is split into individual packets
        String lastpacketString = raw_packets.get(raw_packets.size() - 1);

        if (lastpacketString.contains("null")) { // if our string has "null" in it, we need to excise it
            lastpacketString = lastpacketString.replace("null","");

        }
        //Log.i("Predict", lastpacketString);
        String[] parts= lastpacketString.split("[,]",0);
        boolean[] bad_fields = {false,false,false,false,false,false};
        if(parts.length !=6) { //only verify packets of correct length
            bad_fields[0]=true; // if packet is not of correct length, flag it
            last_packet_quality = 1; // bad packet, notify user
            //Log.i("Predict","Fields = "+parts.length);
        }
        if (!any(bad_fields)) { // this is run if all of the fields are good
            StringTokenizer telemetryTokens=new StringTokenizer(lastpacketString,","); //substring is used to remove the identification character
            if (telemetryTokens.countTokens() == check_letters.length) { //make sure we have as many telemetry numbers as we expect before trying to store their values into our pseudo database
                number_of_good_packets++;
                boolean has_check_letters = false; //start off by assuming we have a bad packet
                for (String item : check_letters) { //loop over our last packet, once per check letter
                    if (lastpacketString.contains(item)) { // I think we are checking if we have check letters at all, and choosing a different import function based on that.
                        has_check_letters=true;
                        break;
                    }
                }
                last_packet_quality = 2; // Good packet! notify user

                if (has_check_letters) {
                    append_packet_data_omit_letters( telemetryTokens);
                }
                else{
                    append_packet_data_no_letters(telemetryTokens);
                }
                if (decoded_rocket_latitudes.get(decoded_rocket_latitudes.size()-1) == 0) {
                    last_packet_quality = 1; // bad packet, notify user
                }
            } else {
                last_packet_quality = 1;} // bad packet, notify user
        } else {
            last_packet_quality = 1; }// bad packet, notify user

        // ast this point we have completely parsed the packet and assigned its contents to our respective variables. Now move onto the extrapolate function
    }


    private void append_packet_data_omit_letters( StringTokenizer telemetryTokens){
        decoded_rocket_latitudes.add(Double.parseDouble(telemetryTokens.nextToken().substring(1)));
        decoded_rocket_longitudes.add(Double.parseDouble(telemetryTokens.nextToken().substring(1)));
        decoded_rocket_datestamps.add(telemetryTokens.nextToken().substring(1));
        decoded_rocket_altitudes.add(Double.parseDouble(telemetryTokens.nextToken().substring(1)));
        decoded_rocket_satellites.add(Integer.parseInt(telemetryTokens.nextToken().substring(1)));
        decoded_rocket_voltages.add(Double.parseDouble(telemetryTokens.nextToken().substring(1)));
        recieved_timestamps.add( (double)Instant.now().getEpochSecond());
    }

    private void append_packet_data_no_letters(StringTokenizer telemetryTokens){
        decoded_rocket_latitudes.add(Double.parseDouble(telemetryTokens.nextToken()));
        decoded_rocket_longitudes.add(Double.parseDouble(telemetryTokens.nextToken()));
        decoded_rocket_datestamps.add(telemetryTokens.nextToken());
        decoded_rocket_altitudes.add(Double.parseDouble(telemetryTokens.nextToken()));
        decoded_rocket_satellites.add(Integer.parseInt(telemetryTokens.nextToken()));
        decoded_rocket_voltages.add(Double.parseDouble(telemetryTokens.nextToken()));
        recieved_timestamps.add((double) Instant.now().getEpochSecond());
    }
//    /**
//     *
//     * @param loc Current User Location
//     */
//    public Predict(Location loc) {
//        // Setter for user location and altitude when I figure out how tf i am gonna do that
//        this.user_altitude = loc.getAltitude();
//    }

//    public Predict() {
//        //the void case, for making the class exist, then updating it all later
//    }

    /**
     * \
     * @param locations x coordinates, distances
     * @param altitudes y coordinates, altitudes
     * @param user_altitude user y altitude
     * @return landing location
     */
    private double linear_interp (double[] locations, double[] altitudes, double user_altitude) {
        //this function generates a slope
        double slope_answer = slope(locations,altitudes); //create a variable to hold our calculated slopes
        //Log.i("EXTRAPOLATE", "Locations: " + String.valueOf(user_altitude));


        // the actual landing spot isnt just x= m/b, that assumes we are starting at x=0 and decending until we hit y=0,
        // we actually are starting from our last point, so we must add that in here
        //Log.i("Predict",String.valueOf((altitudes[altitudes.length-1] - user_altitude)/slope + locations[locations.length-1]));
        return (altitudes[altitudes.length-1] - user_altitude)/slope_answer + locations[locations.length-1];
    }

    private double slope(double[] x, double[] y){
        double ans =0;
        for (int i=0;i< x.length-1;i++) { //MUST  stop 1 before length because this for loop accesses two indexes at once
            ans +=  (y[i+1]-y[i])/(x[i+1]-x[i] );
        }
        //Log.i("Slope", Arrays.toString(x) + Arrays.toString(y));
        ans=ans/(x.length-1);
        return ans;
    }
    private void extrapolate() {
        // the logic here determines how much of the array we want to use for prediction. if we use
        // all of it, we will get the initial rise in hight from the launch, which will spoil our
        // results. 5 points should be good, but 10 would be better
        int size=decoded_rocket_latitudes.size();
        //Log.i("Predict","Size of PacketList="+size);
        int start,end;
        //Log.i("Predict","Extrapolating");
        if ( size==0) {
            start = 0;
            end =0;
            return; // cannot extrapolate 0 datapoints haha
        } else if (size < packets_to_use) {
            start=0;
            end =size-1;
        } else {
            start = size- packets_to_use;
            end = size-1;
        }
        final DecimalFormat df = new DecimalFormat("###.00000");
        //Log.i("Predict","Start="+start);
        //Log.i("Predict","End="+end);
        double landingLat=linear_interp(Doubles.toArray(decoded_rocket_latitudes.subList(start,end)), Doubles.toArray(decoded_rocket_altitudes.subList(start,end)),user_altitude);
        double landingLong=linear_interp(Doubles.toArray(decoded_rocket_longitudes.subList(start,end)), Doubles.toArray(decoded_rocket_altitudes.subList(start,end)),user_altitude);
        descent_rate = slope(Doubles.toArray(recieved_timestamps.subList(start,end)),Doubles.toArray(decoded_rocket_altitudes.subList(start,end)));
        //Log.i("Predict","Descent Rate: " + df.format(descent_rate));
        // set values as our landing prediction

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
