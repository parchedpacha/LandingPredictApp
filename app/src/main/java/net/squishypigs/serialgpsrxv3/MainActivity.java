package net.squishypigs.serialgpsrxv3;

import static android.content.pm.ActivityInfo.*;
import static net.squishypigs.serialgpsrxv3.Predict.*;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    public static Predict predict;
    public UsbSerialPort port;
    public Handler mainLooper;
    public SerialInputOutputManager usbIoManager;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        predict = new Predict(100.0);
        EditText User_alt_ET =  findViewById(R.id.user_altitude_edit_text);
        this.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        // SERIAL TRASH ----------------------------------------------------------------------------
        setbutton();
        start_connection();

        // END SERIAL TRASH ------------------------------------------------------------------------

        View.OnFocusChangeListener listener = (v, hasFocus) -> {

            if (!hasFocus) {

                String text = User_alt_ET.getText().toString();
                predict.setUser_altitude(Double.parseDouble(text));
                TextView landing_prediction_area=findViewById(R.id.landing_prediction_area);
                landing_prediction_area.append("\nuser altitude = "+predict.getUser_altitude());
            }

        };


        User_alt_ET.setOnFocusChangeListener(listener);
        mainLooper = new Handler(Looper.getMainLooper());

            
            
    }
    // TODO Add serial Opening function
    // TODO Add event driven serial data read
    // TODO add serial data parser and Quality Control
    // TODO Add math for generating landing location
    // TODO add precise location permission / some way to get current altitude







    public void send_to_gmaps_callback(View app) {
        Uri location = Uri.parse("geo:0,0?q=" + "42.561996,-83.162815" + "(landing+prediction)"); // z param is zoom level, mor Z == more zoom
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, location); // use the URI to create out intent
        startActivity(mapIntent); // tell android to launch the map


    }
    public void user_altitude_set_callback(View app) {
        TextView user_alt = findViewById(R.id.user_altitude_edit_text);

        predict.setUser_altitude(Double.parseDouble(user_alt.toString()) );
    }

//    @Override
//    public void updateMyText(String myString) {
//        ((TextView) findViewById(R.id.landing_prediction_area)).setText(myString);
//    }

    public void connection_button_callback(View view) {


        setbutton();

    }
    public void setbutton()  {
        ToggleButton buttonview = findViewById(R.id.connectionButton);
        if (buttonview.isChecked()) {
            start_connection();

            if (port ==null || !port.isOpen()) {
                buttonview.setChecked(false);
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

    public void start_connection() {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        ToggleButton buttonview = findViewById(R.id.connectionButton);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

        if (connection != null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here

            port = driver.getPorts().get(0);
            try {
                // if  all the checks have passed, then connect, and also show the user that the connection was successful
                port.open(connection);
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                ImageView iv = findViewById(R.id.connectedStar);
                iv.setVisibility(View.VISIBLE);
                buttonview.setChecked(true);
                usbIoManager = new SerialInputOutputManager(port, (SerialInputOutputManager.Listener) this);
                usbIoManager.start();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }



}
