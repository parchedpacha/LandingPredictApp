package net.squishypigs.serialgpsrxv3;

import static android.content.pm.ActivityInfo.*;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    public Predict predict;
    public UsbSerialPort port;
    public SerialInputOutputManager usbSIoManager;
    public String landing_prediction = "42.561996,-83.162815",TAG = "Main Activity";
    int LOCATION_REQUEST_CODE = 10001;
    FusedLocationProviderClient fusedLocationProviderClient;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DataStore dataStore = new DataStore(this);
        predict = new Predict(100.0, dataStore);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        EditText User_alt_ET = findViewById(R.id.user_altitude_edit_text);
        this.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        // I do not want to sort out rotations and stuff, plus the users phone might intentionally
        // have its orientation screwed with and need to maintain functionality. For instance, pointing the user's
        // phone up to the sky should not cause the app to rotate at all.


        // SERIAL TRASH ----------------------------------------------------------------------------
        setButton();
        if (check_connection()) {
            start_connection();
            // TODO I think I need to add the listener declaration in here, not quite sure how
        }



        // END SERIAL TRASH ------------------------------------------------------------------------
        View.OnFocusChangeListener listener = (v, hasFocus) -> {
            if (!hasFocus) {
                String text = User_alt_ET.getText().toString();
                predict.setUser_altitude(Double.parseDouble(text));
                //TextView landing_prediction_area=findViewById(R.id.landing_prediction_area);
                //landing_prediction_area.append("\nuser altitude = "+predict.getUser_altitude());
            }
        };
        User_alt_ET.setOnFocusChangeListener(listener);

        @SuppressLint("SetTextI18n") View.OnClickListener copyButtonListener = (v) -> { // update user altitude when user stops having cursor in altitude edit text box
            //I could not give any less of a f*** about the concatenation warning
            //predict.read_one_time();
            CopyLandingCoords();
        };
        Button CopyButton = findViewById(R.id.CopyButton);
        CopyButton.setOnClickListener(copyButtonListener);
        //mainLooper = new Handler(Looper.getMainLooper());

        Handler handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                handler.postDelayed(this, 500);
                //List<String> allpackets= predict.getRaw_packets();
                //addMessage(String.valueOf(allpackets));
                addMessage(predict.getLastPackets());
                TextView landing_prediction_area = findViewById(R.id.landing_prediction_area);
                landing_prediction = predict.getLanding_prediction_coords();
                landing_prediction_area.setText("Landing Prediction:\n" + landing_prediction);
                TextView DescentGauge = findViewById(R.id.Descent_Rate_Text_view);
                DescentGauge.setText("Descent Rate:\n" + predict.getDescentRate());
                Button Connection = findViewById(R.id.connectionButton);
                if (usbSIoManager != null) {  //
                    if (usbSIoManager.getState() == SerialInputOutputManager.State.RUNNING) {
                        Connection.setText("CONNECTED"); }
                }
                else {
                    Connection.setText("CONNECT");
                }

                TextView landedindicator = findViewById(R.id.LANDED_Text_view);
                if (predict.getOnGround()) {
                landedindicator.setVisibility(View.VISIBLE);}
                else {landedindicator.setVisibility(View.GONE);}

            }
        };
        handler.postDelayed(r, 0);

    }

    private void indicate_packet_quality (int packet_quality){
        //0 = none, 1 = bad, 2 = good
        ImageView noPacket = findViewById(R.id.no_packet_view);
        ImageView badPacket = findViewById(R.id.bad_packet_view);
        ImageView goodPacket = findViewById(R.id.good_packet_view);
        if (packet_quality == 2) {
            noPacket.setVisibility(View.GONE);
            badPacket.setVisibility(View.GONE);
            goodPacket.setVisibility(View.VISIBLE);
        } else if (packet_quality == 1) {
            noPacket.setVisibility(View.GONE);
            badPacket.setVisibility(View.VISIBLE);
            goodPacket.setVisibility(View.GONE);
        } else {
            noPacket.setVisibility(View.VISIBLE);
            badPacket.setVisibility(View.GONE);
            goodPacket.setVisibility(View.GONE);
        }

    }



    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            askForLocationPermission();
        }
    }

    void CopyLandingCoords() {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(predict.getLanding_prediction_coords());
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Landing Prediction",predict.getLanding_prediction_coords());
            clipboard.setPrimaryClip(clip);
        }
    }
    private void askForLocationPermission() {
        // https://www.youtube.com/watch?v=rNYaEFl6Fms showed me how to do this, at some point I will encapsulate this whole shitty location process into another file
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.i(TAG, "need to ask for permission");
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode ==LOCATION_REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission granted
                getLastLocation();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void getLastLocation() {
        @SuppressLint("MissingPermission") Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    predict.setUserLocation(location);
                    EditText user_alt= findViewById(R.id.user_altitude_edit_text);
                    DecimalFormat df  = new DecimalFormat("###.0");
                    user_alt.setText(df.format(location.getAltitude()));
                } else {
                    Log.i(TAG,"Null Location");
                }
            }
        });
        locationTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG,"onFailure" + e.getLocalizedMessage());
            }
        });




    }
    //    public void onNewData(byte[] data) { //this is the event listener to get data from the USB serial and stuff it into predictor
//        predict.collectRawBytes(data);
//    }

    public void send_to_gmaps_callback(View app) {
        if (landing_prediction !=null) {
            if (!landing_prediction.contains("No Data")) {
                Uri location = Uri.parse("geo:0,0?q=" + landing_prediction + "(landing+prediction)"); // z param is zoom level, mor Z == more zoom

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, location); // use the URI to create out intent
                startActivity(mapIntent); // tell android to launch the map
            } else {
                Toast.makeText(this,"No Prediction!",Toast.LENGTH_SHORT).show();
            }
        }
    }
//    public void user_altitude_set_callback(View app) {
//        TextView user_alt = findViewById(R.id.user_altitude_edit_text);
//
//        predict.setUser_altitude(Double.parseDouble(user_alt.toString()) );
//    }

    public void connection_button_callback(View view) {

        if (check_connection()) {
            setButton();
        } else {
            ToggleButton buttonView = findViewById(R.id.connectionButton);
            buttonView.setChecked(false);
            Toast.makeText(this,"No USB Device!",Toast.LENGTH_SHORT).show();
        }

    }

    public void setButton()  { // when the user presses the connect button, try to
        ToggleButton buttonView = findViewById(R.id.connectionButton);
        if (buttonView.isChecked() && check_connection()) { //if we are moving from unpressed to pressed, and we have a usable serial port

            start_connection();

            if (port ==null || !port.isOpen()) {
                buttonView.setChecked(false);
            }
        } else { //if we are disconnecting
            if (port != null) {
                if (port.isOpen()) {
                    try{port.close();} catch (IOException e) {
                        e.printStackTrace();
                        ImageView iv = findViewById(R.id.connectedStar);
                        iv.setVisibility(View.INVISIBLE);
                        buttonView.setChecked(false);
                    }

                    ImageView iv = findViewById(R.id.connectedStar);
                    iv.setVisibility(View.INVISIBLE);
                    buttonView.setChecked(false);
                }
            }
        }
    }

    public void clearpackets_callback(View view) {
        TextView packet_area = findViewById(R.id.packet_text_view);
        packet_area.setText("");
        predict.resetPredict();
    }

    public boolean check_connection() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        //ToggleButton buttonView = findViewById(R.id.connectionButton);
        return !availableDrivers.isEmpty();
    }
    public void start_connection()  {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        ToggleButton buttonView = findViewById(R.id.connectionButton);

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

        if (connection != null) {
            port = driver.getPorts().get(0);
            try {
                // if  all the checks have passed, then connect, and also show the user that the connection was successful
                port.open(connection);
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                port.setDTR(false);
                port.setRTS(true);
                ImageView iv = findViewById(R.id.connectedStar);
                iv.setVisibility(View.VISIBLE);
                buttonView.setChecked(true);
                usbSIoManager = new SerialInputOutputManager(port);
                usbSIoManager.start();
                usbSIoManager.setListener(new SerialInputOutputManager.Listener() {
                    /**
                     * Called when new incoming data is available.
                     *
                     * @param data incoming serial data
                     */
                    @Override
                    public void onNewData(byte[] data) {
                        predict.collectRawBytes(data);
                        //TextView packet_area= findViewById(R.id.packet_text_view);
                        //packet_area.setText(String.valueOf(new String(data)));
                    }

                    /**
                     * Called when {@link SerialInputOutputManager#run()} aborts due to an error.
                     *
                     * @param e the error
                     */
                    @Override
                    public void onRunError(Exception e) {
                        Log.e("Serial Listener (I made)", "Error" + e.getLocalizedMessage());
                    }

                });
                //return usbSIoManager;
                //begin_watching_serial(); //<-- here the serial watcher thread will be ran
            } catch (IOException e) {
                System.out.println( "Oopsie woopsie In Out Ewwow");
                e.printStackTrace();
            }

        }

        //return null; //this function runs after we check if a port is open, so this *should* never return null, haha
    }



    private void addMessage(String msg) {

        TextView packet_area= findViewById(R.id.packet_text_view);
        packet_area.setText(msg+ "\n");
        // append the new string
        //packet_area.append(msg ); //this is for appending, which I dont need atm
        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        //final int scrollAmount = packet_area.getLayout().getLineTop(packet_area.getLineCount()) - packet_area.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0

            packet_area.scrollTo(0, 0); //packet_area.getHeight()
    }

}
