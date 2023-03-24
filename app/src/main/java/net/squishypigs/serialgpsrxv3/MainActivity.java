package net.squishypigs.serialgpsrxv3;

import static android.content.pm.ActivityInfo.*;


import androidx.appcompat.app.AppCompatActivity;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    public Predict predict;
    public UsbSerialPort port;
    public Handler mainLooper;
    public SerialInputOutputManager usbSIoManager;
    public Thread watcherThread;
    public Runnable runnable;
    public String landing_prediction ="42.561996,-83.162815";
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        predict = new Predict(100.0);

        EditText User_alt_ET =  findViewById(R.id.user_altitude_edit_text);
        this.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        // I do not want to sort out rotations and stuff, plus the users phone might intentionally
        // have its orientation screwed with and need to maintain functionality. For instance, pointing the user's
        // phone up to the sky should not cause the app to rotate at all.


        // SERIAL TRASH ----------------------------------------------------------------------------
        setButton();
        if (check_connection()) {
            usbSIoManager = start_connection();
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

        @SuppressLint("SetTextI18n") View.OnClickListener readButtonListener = (v) -> {
            //I could not give any less of a f*** about the concatenation warning
            predict.read_one_time();
            List<String> allpackets= predict.getRaw_packets();
            TextView packet_area= findViewById(R.id.packet_text_view);
            packet_area.setText(String.valueOf(allpackets));
            TextView landing_prediction_area=findViewById(R.id.landing_prediction_area);
            landing_prediction =predict.getLanding_prediction_coords();
            landing_prediction_area.setText("Landing Prediction: "+ landing_prediction);
        };
        Button readButton = findViewById(R.id.readbutton);
        readButton.setOnClickListener(readButtonListener);
        //mainLooper = new Handler(Looper.getMainLooper());
    }
    // TODO Add serial Opening function
    // TODO Add event driven serial data read
    // TODO add serial data parser and Quality Control
    // TODO Add math for generating landing location
    // TODO add precise location permission / some way to get current altitude

    public void onNewData() { //this is the event listener to get data from the USB serial and stuff it into predictor
        predict.addPacket();
    }

    public void send_to_gmaps_callback(View app) {
        Uri location = Uri.parse("geo:0,0?q=" + landing_prediction + "(landing+prediction)"); // z param is zoom level, mor Z == more zoom
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, location); // use the URI to create out intent
        startActivity(mapIntent); // tell android to launch the map


    }
//    public void user_altitude_set_callback(View app) {
//        TextView user_alt = findViewById(R.id.user_altitude_edit_text);
//
//        predict.setUser_altitude(Double.parseDouble(user_alt.toString()) );
//    }

    public void connection_button_callback(View view) {


        setButton();

    }

    public void setButton()  {
        ToggleButton buttonView = findViewById(R.id.connectionButton);
        if (buttonView.isChecked()) {
            start_connection();

            if (port ==null || !port.isOpen()) {
                buttonView.setChecked(false);
            }
        } else {
            if (port != null) {
                if (port.isOpen()) {
                    try{port.close();} catch (IOException e) {
                        e.printStackTrace();
                    }

                    ImageView iv = findViewById(R.id.connectedStar);
                    iv.setVisibility(View.INVISIBLE);
                }
            }
        }
    }



    public boolean check_connection() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        ToggleButton buttonView = findViewById(R.id.connectionButton);
        return !availableDrivers.isEmpty();
    }
    public SerialInputOutputManager start_connection()  {
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
                ImageView iv = findViewById(R.id.connectedStar);
                iv.setVisibility(View.VISIBLE);
                buttonView.setChecked(true);
                usbSIoManager = new SerialInputOutputManager(port, (SerialInputOutputManager.Listener) this);
                usbSIoManager.start();
                return usbSIoManager;
                //begin_watching_serial(); //<-- here the serial watcher thread will be ran
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return null; //this function runs after we check if a port is open, so this *should* never return null, haha
    }

//    public static Thread begin_watching_serial(final Runnable runnable) { // I believe I want to use this method to begin the background serial watching thread
//        final Thread t = new Thread() {
//            @Override
//            public void run() {
//                try {
//                    runnable.run();
//                } finally {
//
//                }
//            }
//        };
//        t.start();
//        return t;
//    }



}
