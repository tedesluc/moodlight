package ch.zhaw.hhrt.moodlight_reference_design;
//package com.chiralcode.colorPicker;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chiralcode.colorpicker.ColorPicker;
import com.chiralcode.colorpicker.ColorPickerDialog;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


/**
 * <h1>Android-App for remote control of a <b>Moodlight</b> over Bluetooth.</h1>
 * <br>
 * This is the reference design for the Electro Technics Project <b>ETP</b>
 * in the second year of the electronics engineering curriculum
 * at the Zurich University of Applied Sciences.
 * <br>
 * This code is intended as a <b>template</b> or starting point for own implementations.
 * <br> <br>
 * <img src="{@docRoot}/AppActivity.png" alt="screenshot">
 * <br> <br>
 * The standard programming approach would be to use one class for each task:
 * <ul>
 *     <li>Activity lifecycle of the app</li>
 *     <li>Graphical user interface</li>
 *     <li>Parse and process received data from the Moodlight</li>
 *     <li>Serial communication</li>
 *     <li>Communication over Bluetooth</li>
 *     <li>Functions for debugging</li>
 * </ul>
 * <br>
 * Considering that this is a really small app running on a smartphone with limited resources,
 * I decided to put all the code into one single class.
 * However, I grouped the methods in the above order to increase readability and maintainability.
 * <br>
 * Thus the resources and the context are easily accessible without writing extra code.
 * For the same reason, the fields are accessed directly instead of using setter and getter methods.
 * <br> <br>
 * <i>Notes:<br>
 * The name of the Bluetooth module of the Moodlight is defined in the file "settings.xml"<br>
 * - Enter the name of the Bluetooth device as listed in the Bluetooth list of the smartphone.<br>
 * - All the other settings are also in the file "settings.xml"</i>
 * <br>
 * @author Hanspeter Hochreutener, hhrt@zhaw.ch
 * @author <br>JAVA code partially copied from:
 * <u><a href=
 * "http://stackoverflow.com/questions/13450406/how-to-receive-serial-data-using-android-bluetooth"
 * target="_blank">user1911300 @ stackoverflow.com</a></u>
 * @since October 2015
 * <br> <br>
 * Use these settings to "Generate JavaDoc..."
 * <br>
 * <img src="{@docRoot}/JavaDocSettings.png" alt="JavaDocSettings">
 * <br> <br>
 */
public class MainActivity extends Activity {


    /*********************************************************************************************
     * Declaration of fields
     *********************************************************************************************/


    /** Transmitted text message */                         EditText TX_message;
    /** Received text message */                            TextView RX_message;
    /** Delimiter character at the end of a message */      byte TXRX_delimiter;

    /** SeekBar position changed by user input */           volatile boolean activeSeekBar;

    /** Stream for transmitting */                          OutputStream outStream;
    /** Stream for receiving */                             InputStream inStream;
    /** Stop the thread that handles data reception */      volatile boolean stopDataInThread;
    /** Buffer for received characters */                   byte[] dataInBuffer;
    /** Actual position in the buffer */                    int dataInBufferPosition;

    /** Bluetooth adapter */                                BluetoothAdapter myBTadapter;
    /** Bluetooth device */                                 BluetoothDevice  myBTdevice;
    /** Bluetooth socket */                                 BluetoothSocket  myBTsocket;


    /*********************************************************************************************
     * Activity lifecycle of the app
     *********************************************************************************************/


    /**
     * Called when clicked on the icon of this app to start it.
     * <br>Initializes the app
     * and checks if the specified Bluetooth device is already paired.
     * @param savedInstanceState restores the previously saved settings, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logInfo("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Bond variables to resources
        TX_message = (EditText) findViewById(R.id.bluetoothTX_message);
        RX_message = (TextView) findViewById(R.id.bluetoothRX_message);
        TXRX_delimiter = (byte) getResources().getInteger(R.integer.TXRX_delimiter);
        // Register the listeners for the SeekBars
        changeSeekBarListener(R.id.white_slider, getString(R.string.white_label));
        changeSeekBarListener(R.id.red_slider,   getString(R.string.red_label)  );
        changeSeekBarListener(R.id.green_slider, getString(R.string.green_label));
        changeSeekBarListener(R.id.blue_slider,  getString(R.string.blue_label) );
        activeSeekBar = true;
        //findBTdevice();
    }


    /**
     * Called when (re)started. That is, after onCreate() or after having been hidden.
     * <br>Opens a connection to the specified Bluetooth device
     * and synchronizes the SeekBar sliders to reflect the the actual values from the Moodlight.
     */
    @Override
    protected void onStart() {
        logInfo("onStart");
        super.onStart();
        //openBTdevice();
        synchNow(getWindow().getCurrentFocus());
    }


    /**
     * Called when stopped. That is, hidden in the background or before onDestroy().
     * <br>Closes the connection to the specified Bluetooth device.
     */
    @Override
    protected void onStop() {
        logInfo("onStop");
        //closeBTdevice();
        super.onStop();
    }


    /*********************************************************************************************
     * Graphical user interface
     *********************************************************************************************/


    /**
     * Send the data which is in the "TX_message" field.
     * <br>This is also called when the "Send Now" button is clicked.
     * @param view is the active view for displaying messages
     */
    public void sendNow(View view) {
        logInfo("Send now");
        //sendData();
    }


    /**
     * Called by a click on the "Synch Now" button or in onStart().
     * <br>Synchronizes the SeekBar sliders to reflect the the actual values from the Moodlight.
     * @param view is the active view for displaying messages
     */
    public void synchNow(View view) {
        logInfo("Synch Now");
        // Ask the Moodlight which values are actually set by sending these questions
        TX_message.setText(getString(R.string.white_label) + " ?", TextView.BufferType.NORMAL);
        //sendData();
        TX_message.setText(getString(R.string.red_label)   + " ?", TextView.BufferType.NORMAL);
        //sendData();
        TX_message.setText(getString(R.string.green_label) + " ?", TextView.BufferType.NORMAL);
        //sendData();
        TX_message.setText(getString(R.string.blue_label)  + " ?", TextView.BufferType.NORMAL);
        //sendData();
        TX_message.setText(getString(R.string.idle_label), TextView.BufferType.NORMAL);
        //sendData();                             // Set the Moodlight back to idle mode
    }

    /**
     * Called by a click on the "Synch Now" button or in onStart().
     * <br>Synchronizes the SeekBar sliders to reflect the the actual values from the Moodlight.
     * @param view is the active view for displaying messages
     */
    public void startColorPicker(View view) {
        logInfo("Open Color Picker");

        int initialColor = getColor();

        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, new ColorPickerDialog.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                setColor(color);
                setColor(color);
            }
        });



        colorPickerDialog.show();
    }


    /**
     * Change/register listeners for the SeekBars.
     * @param id      of the SeekBar
     * @param command string to be sent to Bluetooth device
     */
    private void changeSeekBarListener(int id, final String command) {
        SeekBar bar = (SeekBar) findViewById(id);
        bar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {     // Uses an inner anonymous class
                    public void onProgressChanged(SeekBar SeekBar, int progress, boolean fromUser) {
                        // When the SeekBar position is changed by the user of the smartphone
                        // the new value must be sent to the Moodlight to change its state.
                        // When the SeekBar position is changed
                        // due to a value received from the Moodlight
                        // this onProgressChanged method is also called.
                        // In this case, however, nothing has to be sent back to the Moodlight
                        // to prevent iterative loops Moodlight => smartphone => Moodlight => ...
                        logInfo("SeekBar " + command + " changed to " + SeekBar.getProgress()
                                + " : activeSeekBar = " + activeSeekBar);
                        if (activeSeekBar) {
                            TX_message.setText(command + " " + SeekBar.getProgress(),
                                    TextView.BufferType.NORMAL);
                            //sendData();
                        }
                    }

                    public void onStartTrackingTouch(SeekBar SeekBar) {
                    }

                    public void onStopTrackingTouch(SeekBar SeekBar) {
                    }
                }
        );
    }


    /*********************************************************************************************
     * Parse and process received data from the Moodlight
     *********************************************************************************************/


    /**
     * Processes the received data.
     */
    private void processReceivedData() {
        int id;
        String command = "";
        int value = 0;
        String msg = RX_message.getText().toString();
        String[] words = msg.split("\\s");      // Split the message at white-space characters
        if (words.length > 0) {
            command = words[0].toLowerCase();   // First word is interpreted as command
        }                                       // Command is not case sensitive
        if (words.length > 1) {                 // Is there a second word?
            try {
                value = Integer.parseInt(words[1].trim());  // Second word is interpreted as number
            } catch (NumberFormatException e) {
                value = 0;
            }
        }
        /* Compare the received command with each possible value
         * to find out which SeekBar has to be adjusted.
         * Normally this would be programmed with: switch (command) case ...
         * As switch can not compare to string resources I used: if ... else if ...
         */
        if        (command.equals(getString(R.string.white_label))){
            id = R.id.white_slider;
        } else if (command.equals(getString(R.string.red_label))){
            id = R.id.red_slider;
        } else if (command.equals(getString(R.string.green_label))){
            id = R.id.green_slider;
        } else if (command.equals(getString(R.string.blue_label))){
            id = R.id.blue_slider;
        } else if (command.equals(getString(R.string.idle_label))){
            id = 0;                             // Nothing to do
        } else {
            logInfo("Unknown command: " + command);
            id = 0;
        }
        if ((id != 0) && (words.length > 1)) {  // Valid command and value were received
            activeSeekBar = false;              // Disable onProgressChanged() to prevent iteration
            SeekBar bar = (SeekBar) findViewById(id);
            bar.setProgress(value);
            activeSeekBar = true;               // Enable onProgressChanged() again
        }
    }


    /*********************************************************************************************
     * Serial communication
     *********************************************************************************************/


    /**
     * Sends the data.
     */
    private void sendData() {
        String msg = TX_message.getText().toString();
        msg += (char) TXRX_delimiter;           // Append delimiter character
        try {
            outStream.write(msg.getBytes());
            logInfo("sent: " + msg);
        } catch (Exception e) {
            logError("Data could not be sent: " + e);
            finish();
        }
        synchronized (this) {
            try {               // Wait a bit to give the Moodlight time to process the data
                wait(getResources().getInteger(R.integer.wait_time));
            } catch (Exception e) {
                logError("Error while waiting: " + e);
            }
        }
    }


    /**
     * Starts a new Thread which listens for incoming data and handles it.
     */
    private void listenForData() {
        final Handler RXmessageHandler = new Handler();
        // Handler needed to access outer class from inner Thread
        stopDataInThread = false;               // Stop inner Thread flag
        dataInBuffer = new byte[256];           // Temporary buffer
        dataInBufferPosition = 0;               // Position of last byte in the buffer
        Thread dataInThread = new Thread(       // Thread listens for incoming data
                new Runnable() {                // Uses inner classes
                    public void run() {
                        while (!Thread.currentThread().isInterrupted() && !stopDataInThread) {
                            try {
                                int bytesAvailable = inStream.available();
                                if (bytesAvailable > 0) {
                                    byte[] packetBytes = new byte[bytesAvailable];
                                    // Temporary variable for InputStream
                                    bytesAvailable = inStream.read(packetBytes);
                                    for (int i = 0; i < bytesAvailable; i++) {
                                        if (packetBytes[i] == TXRX_delimiter) {
                                                    // Delimiter character was received
                                            final String data = new String(dataInBuffer, 0,
                                                    dataInBufferPosition);
                                            // Make a String from received bytes
                                            dataInBufferPosition = 0;   // Start anew with next byte
                                            RXmessageHandler.post(
                                                    new Runnable() {    // Process received String
                                                        public void run() {
                                                            logInfo("received: " + data);
                                                            RX_message.setText(data);
                                                            processReceivedData();
                                                        }
                                                    }
                                            );
                                        } else {    // Normal character was received
                                            dataInBuffer[dataInBufferPosition++] = packetBytes[i];
                                            // Store the received byte in the temporary buffer
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logError("Error in dataInThread: " + e);
                                stopDataInThread = true;
                            }
                            synchronized (this) {
                                try {           // Wait a bit to use less CPU time
                                    wait(getResources().getInteger(R.integer.wait_time));
                                } catch (Exception e) {
                                    logError("Error while waiting: " + e);
                                }
                            }

                        }
                    }
                }
        );
        dataInThread.start();
    }


    /*********************************************************************************************
     * Communication over Bluetooth
     *********************************************************************************************/

    /**
     * Checks if a the specified Bluetooth device is paired.
     */
    private void findBTdevice() {
        String deviceName = getString(R.string.device_name);
        Boolean paired = false;
        myBTadapter = BluetoothAdapter.getDefaultAdapter();
        if (myBTadapter == null) {
            logError("No Bluetooth adapter available. Running this app not possible.");
            toastMessage("No Bluetooth adapter available.\nRunning this app not possible.");
            finish();
        }
        if (!myBTadapter.isEnabled()) {         // Turn on Bluetooth adapter
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Set<BluetoothDevice> pairedDevices = myBTadapter.getBondedDevices();
        if (pairedDevices.size() > 0) {         // Check if device is already paired
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {              // Name of Bluetooth module
                    myBTdevice = device;
                    paired = true;
                    break;
                }
            }
        }
        if (paired) {
            logInfo("Bluetooth device " + deviceName + " is paired.");
        } else {
            logError("Bluetooth device " + deviceName + " is NOT paired.");
            toastMessage("Bluetooth device " + deviceName
                    + " is NOT paired yet.\nCheck available Bluetooth devices in the smartphone");
            finish();                           // Stop the app
        }
    }


    /**
     * Opens a connection to the specified Bluetooth device.
     */
    private void openBTdevice() {
        Boolean success = false;
        int tryCount = 0;
        myBTadapter.cancelDiscovery();      // Cancel discovery because it is heavy weighted
        while (!success && tryCount < getResources().getInteger(R.integer.connect_retry)) {
            tryCount++;
            try {
                UUID uuid = UUID.fromString(getString(R.string.UUID));  // SerialPortService ID
                myBTsocket = myBTdevice.createRfcommSocketToServiceRecord(uuid);
                myBTsocket.connect();           // Try to establishing the connection
                success = true;
                logInfo("Bluetooth device successfully opened at try number " + tryCount);
            } catch (Exception e) {
                logError("Attempt " + tryCount + " to open Bluetooth device failed: " + e);
            }
            if (!success) {
                try {
                    myBTsocket.close();         // Make sure that the connection is closed
                } catch (Exception e) {
                    logError("Bluetooth device could not be closed: " + e);
                    finish();
                }
                synchronized (this) {
                    try {                       // Wait a while before attempting to connect again
                        wait(getResources().getInteger(R.integer.close_time));
                    } catch (Exception e) {
                        logError("Error while waiting: " + e);
                    }
                }
            }
        }
        if (!success) {
            logError("Connection to Bluetooth device failed");
            toastMessage("Connection to Bluetooth device failed.\nStart the app again, please.");
            finish();                           // Stop the app immediately
        }
        try {
            outStream = myBTsocket.getOutputStream();
            inStream = myBTsocket.getInputStream();
            listenForData();                    // Start a new Thread which handles incomming data
        } catch (Exception e) {
            logError("getOutputStream or getInputStream failed: " + e);
            finish();                           // Stop the app
        }
    }


    /**
     * Closes the connection to the specified Bluetooth device.
     */
    private void closeBTdevice() {
        try {
            stopDataInThread = true;
            outStream.close();
            inStream.close();
            myBTsocket.close();
            logInfo("BT device closed");
        } catch (Exception e) {
            logError("BT device could not be closed: " + e);
            finish();
        }
        synchronized (this) {
            try {                               // Wait, make sure the connection is really closed
                wait(getResources().getInteger(R.integer.close_time));
            } catch (Exception e) {
                logError("Error while waiting: " + e);
            }
        }
    }


    /*********************************************************************************************
     * Functions for debugging
     *********************************************************************************************/

    /**
     * Show error in the log output.
     * @param message is written to the log output which is useful for debugging
     */
    private void logError(String message) {
        Log.e(getString(R.string.app_name), message);    // for debugging
    }

    /**
     * Show information in the log output.
     * @param message is written to the log output which is useful for debugging
     */
    private void logInfo(String message) {
        Log.i(getString(R.string.app_name), message);    // for debugging
    }

    /**
     * Show a message in a Toast pop-up.
     * @param message is also shown in normal operation, not only when debugging!
     */
    private void toastMessage(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private int getColor()
    {
        int color = 0;
        int red = getProgressBarValue(R.id.red_slider);
        int green = getProgressBarValue(R.id.green_slider);
        int blue = getProgressBarValue(R.id.blue_slider);

        color = ((red & 0xFF) << 16) + ((green & 0xFF) << 8) + (blue & 0xFF);

        return color;
    }

    private void setColor(int color) {
        int red, green, blue, white;

        red = (color >> 16) & 0xFF;
        setProgressBar(R.id.red_slider, red);
        colorPickerSend(R.string.red_label, red);

        green = (color >> 8) & 0xFF;
        setProgressBar(R.id.green_slider, green);
        colorPickerSend(R.string.green_label, green);

        blue = color & 0xFF;
        setProgressBar(R.id.blue_slider, blue);
        colorPickerSend(R.string.blue_label, blue);

        white = Math.max(red, Math.max(green, blue));
        setProgressBar(R.id.white_slider, white);
        colorPickerSend(R.string.white_label, white);

        TX_message.setText(getString(R.string.idle_label), TextView.BufferType.NORMAL);
        //sendData();
    }

    private int getProgressBarValue(int id)
    {
        int value = 0;
        SeekBar bar = (SeekBar) findViewById(id);
        value = bar.getProgress();

        return value;
    }

    private void setProgressBar(int id, int value)
    {
        activeSeekBar = false;
        SeekBar bar = (SeekBar) findViewById(id);
        bar.setProgress(value);
        activeSeekBar = true;
    }

    private void colorPickerSend(int id, int value)
    {
        TX_message.setText(getString(id) + " " + Integer.toString(value), TextView.BufferType.NORMAL);
        //sendData();
    }
}
