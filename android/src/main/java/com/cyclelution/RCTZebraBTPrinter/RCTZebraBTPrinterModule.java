package com.cyclelution.RCTZebraBTPrinter;

import java.lang.reflect.Method;
import java.util.Set;
import javax.annotation.Nullable;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.util.Base64;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Callback;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;

import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DeviceFilter;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import static com.cyclelution.RCTZebraBTPrinter.RCTZebraBTPrinterPackage.TAG;

@SuppressWarnings("unused")
public class RCTZebraBTPrinterModule extends ReactContextBaseJavaModule {

    // Debugging
    private static final boolean D = true;
    private final ReactApplicationContext reactContext;

    private Connection printerConnection;
    private ZebraPrinter printer;

    private String delimiter = "";

    public RCTZebraBTPrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);

        if (D) Log.d(TAG, "Bluetooth module started");

        this.reactContext = reactContext;
    }

    @ReactMethod 
    public void portDiscovery(String type, final Promise response) {
      new Thread(new Runnable() {
        public void run() {
          try {
            if (D) Log.d(TAG, "Looking for printers");
            final WritableArray printers = new WritableNativeArray();
            BluetoothDiscoverer.findPrinters(reactContext, new DiscoveryHandler() {
              public void discoveryError(String message) {
                if (D) Log.d(TAG, "Bluetooth discovery erroed");
              }
      
              public void discoveryFinished() {
                if (D) Log.d(TAG, "Bluetooth discovery finished");
                response.resolve(printers);
              }
      
              public void foundPrinter(DiscoveredPrinter printer) {
                if (D) Log.d(TAG, "Bluetooth discovery has found a printer");
                WritableMap printerInfo = new WritableNativeMap();
                printerInfo.putString("type", "Bluetooth");
                printerInfo.putString("address", printer.address);
                printers.pushMap(printerInfo);
              }
            });
          } catch (ConnectionException e) {
            if (D) Log.d(TAG, "Failed to find bluetooth printers");
          }
        }
      }).start();
    }

    @ReactMethod
    public void printLabel(ReadableMap printerInfo, String command, Promise response) {
      new Thread(new Runnable() {
        public void run() {
          if ("Bluetooth".equals(printerInfo.getString("type"))) {
            printerConnection = null;
            printerConnection = new BluetoothConnectionInsecure(printerInfo.getString("address"));

            try {
              printerConnection.open();
              try {
                  printerConnection.write(command.getBytes());
                  if (printerConnection instanceof BluetoothConnection) {
                      String friendlyName = ((BluetoothConnection) printerConnection).getFriendlyName();
                      if (D) Log.d(TAG, "printLabel printed with " + friendlyName);
                  }
              } catch (ConnectionException e) {
                response.resolve(false);
              } finally {
                printerConnection.close();
                response.resolve(true);
              }
            } catch ( ConnectionException e) {
              response.resolve(false);
            }
          }  
        }
      }).start();          
    }

    @Override
    public String getName() {
        return "RCTZebraBTPrinter";
    }

}
