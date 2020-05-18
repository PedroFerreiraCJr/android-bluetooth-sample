package br.com.dotofcodex.bluetooth_sample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ChangedPackages;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_BLUETOOTH = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;

    private static final int STATE_CONNECTING = 0;
    private static final int STATE_CONNECTED = 1;
    private static final int STATE_READ = 2;
    private static final int STATE_WRITE = 3;
    private static final int STATE_CONNECTION_FAILED = 4;

    private static final String BLUETOOTH_APP_NAME = "Bluetooth Chat";
    private static final UUID BLUETOOTH_APP_UUID = UUID.fromString("5afc04a6-9c91-49d2-9271-ecf35d2f7158");

    private static final String BLUETOOTH_ON = "Bluetooth Enabled";
    private static final String BLUETOOTH_OFF = "Bluetooth Disabled";

    @BindView(R.id.bt_enable)
    protected Button enable;

    @BindView(R.id.bt_disable)
    protected Button disable;

    @BindView(R.id.tv_status)
    protected TextView status;

    @BindView(R.id.tv_devices)
    protected TextView devices;

    @BindView(R.id.bt_discover)
    protected Button discover;

    @BindView(R.id.bt_discoverability)
    protected Button discoverability;

    @BindView(R.id.bt_send)
    protected Button send;

    private BluetoothAdapter adapter;
    private BroadcastReceiver receiverAction;
    private BroadcastReceiver receiverState;
    private BroadcastReceiver receiverScan;
    private List<BluetoothDevice> bts;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Snackbar snackbar = Snackbar.make(getWindow().getDecorView(), "Bluetooth not supported", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("OK", (View v) -> {
                finish();
            });
            snackbar.show();
        }

        enable.setOnClickListener((View v) -> {
            if (!adapter.isEnabled()) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_BLUETOOTH);
            }
            else {
                status.setText(BLUETOOTH_ON);
            }
        });

        disable.setOnClickListener((View v) -> {
            if (adapter.isEnabled()) {
                adapter.disable();
                status.setText(BLUETOOTH_OFF);
            }
        });

        // start as a client
        discover.setOnClickListener((View v) -> {
            requestBluetoothAdditionalPermission();
            startDiscover();
        });

        // start as a server
        discoverability.setOnClickListener((View v) -> {
            try {
                new BluetoothServer(adapter, handler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            int seconds = 60 * 2;
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
            startActivity(intent);
        });

        send.setOnClickListener((View v) -> {
            if (BluetoothChat.getCurrentInstance() != null) {
                Log.i(TAG, "Message sent...");
                BluetoothChat.getCurrentInstance().write("Pedro Ferreira de Carvalho Junior".getBytes(Charset.forName("UTF-8")));
            }
        });

        IntentFilter filterAction = new IntentFilter();
        filterAction.addAction(BluetoothDevice.ACTION_FOUND);
        filterAction.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        receiverAction = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (!bts.contains(device)) {
                        bts.add(device);

                        if (device.getName() != null && device.getName().contains("Lenovo")) {
                            try {
                                new BluetoothClient(adapter, device, handler).start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    for (BluetoothDevice dev : bts) {
                        Log.i(TAG, String.format("%s, %s", dev.getName(), dev.getAddress()));
                    }
                    return;
                }

                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                    Log.i(TAG, "Discovery finished");
                    bts.clear();
                }
            }
        };

        IntentFilter filterState = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        receiverState = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    int type = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    String message = null;
                    switch (type) {
                        case BluetoothAdapter.STATE_TURNING_ON: {
                            message = "Bluetooth turning on";
                            break;
                        }
                        case BluetoothAdapter.STATE_ON: {
                            message = "Bluetooth On";
                            break;
                        }
                        case BluetoothAdapter.STATE_TURNING_OFF: {
                            message = "Bluetooth turning off";
                            break;
                        }
                        case BluetoothAdapter.STATE_OFF: {
                            message = "Bluetooth Off";
                            break;
                        }
                        case BluetoothAdapter.STATE_CONNECTED: {
                            message = "Connected";
                            break;
                        }
                        case BluetoothAdapter.STATE_CONNECTING: {
                            message = "Connecting";
                            break;
                        }
                        case BluetoothAdapter.STATE_DISCONNECTED: {
                            message = "Disconnected";
                            break;
                        }
                        case BluetoothAdapter.STATE_DISCONNECTING: {
                            message = "Disconnecting";
                            break;
                        }
                        default: {
                            message = "None State";
                        }
                    }

                    Log.i(TAG, message);
                }
            }
        };

        IntentFilter filterScan = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        receiverScan = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {
                    int type = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                    String message = null;
                    switch (type) {
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE: {
                            message = "Scan Mode connectable";
                            break;
                        }
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE: {
                            message = "Scan Mode connectable discoverable";
                            break;
                        }
                        case BluetoothAdapter.SCAN_MODE_NONE: {
                            message = "Scan Mode none";
                            break;
                        }
                        default: {
                            message = "Scan Mode error";
                        }
                    }

                    Log.i(TAG, message);
                }
            }
        };

        if (bts == null) {
            bts = new ArrayList<>();
        }

        handler = new Handler(new HandlerCallbackImpl(this));

        registerReceiver(receiverAction, filterAction);
        registerReceiver(receiverState, filterState);
        registerReceiver(receiverScan, filterScan);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bts != null && !bts.isEmpty()) {
            bts.clear();
        }

        try {
            unregisterReceiver(receiverAction);
        } catch (IllegalArgumentException e) {  }

        try {
            unregisterReceiver(receiverState);
        } catch (IllegalArgumentException e) {  }

        try {
            unregisterReceiver(receiverScan);
        } catch (IllegalArgumentException e) {  }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(getWindow().getDecorView(), "Bluetooth Additional Permission Granted", Snackbar.LENGTH_SHORT);
            }
            else {
                Snackbar.make(getWindow().getDecorView(), "Bluetooth Additional Permission Denied", Snackbar.LENGTH_SHORT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                Snackbar.make(getWindow().getDecorView(), "Bluetooth permission granted", Snackbar.LENGTH_SHORT).show();
                listBondDevices();
                status.setText(BLUETOOTH_ON);
            }
            else {
                Snackbar.make(getWindow().getDecorView(), "Bluetooth permission NOT granted", Snackbar.LENGTH_SHORT).show();
                status.setText(BLUETOOTH_OFF);
            }
        }
    }

    private void startDiscover() {
        if (!adapter.isEnabled()) {
            Snackbar.make(getWindow().getDecorView(), "Bluetooth not enabled", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (adapter.isDiscovering()) {
            Log.i(TAG, "starting discovery again...");
            adapter.cancelDiscovery();
            adapter.startDiscovery();
            return;
        }

        Log.i(TAG, "just starting...");
        adapter.startDiscovery();
    }

    private void requestBluetoothAdditionalPermission() {
        if (!isPermissionGranted()) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void listBondDevices() {
        final Set<BluetoothDevice> devices = adapter.getBondedDevices();

        if (devices.isEmpty()) {
            this.devices.setText("There are no bond devices");
            return;
        }

        String[] names = new String[devices.size()];
        int i = 0;
        for (BluetoothDevice device : devices) {
            names[i] = device.getName();
            i++;
        }

        i = 0;
        this.devices.setText(names[i]);
        for (String device : names) {
            if (i != 0) {
                this.devices.setText(this.devices.getText().toString().concat("\n").concat(device));
            }
            i++;
        }
    }

    private static class HandlerCallbackImpl implements Handler.Callback {
        private Context ctx;

        public HandlerCallbackImpl(Context context) {
            super();
            this.ctx = context;
        }

        @Override
        public boolean handleMessage(@NonNull Message message) {
            Log.i(TAG, "New message");
            Log.i(TAG, String.valueOf(message.what));
            switch (message.what) {
                case STATE_CONNECTING: {
                    // update ui
                    break;
                }
                case STATE_CONNECTED: {
                    // update ui
                    break;
                }
                case STATE_CONNECTION_FAILED: {
                    // update ui
                    break;
                }
                case STATE_READ: {
                    // update ui
                    byte[] bytes = (byte[])message.obj;
                    Log.i(TAG, new String(bytes, 0, message.arg1));
                    Toast.makeText(ctx, new String(bytes, 0, message.arg1), Toast.LENGTH_LONG).show();
                    break;
                }
                case STATE_WRITE: {
                    Toast.makeText(ctx, "Writing...", Toast.LENGTH_LONG).show();
                    break;
                }
                case 1000: {
                    Toast.makeText(ctx, "Connected as Client", Toast.LENGTH_LONG).show();
                    break;
                }
                case 2000: {
                    Toast.makeText(ctx, "Connected as Server", Toast.LENGTH_LONG).show();
                    break;
                }
            }

            return true;
        }
    }

    private static class BluetoothServer extends Thread {

        private final BluetoothAdapter adapter;
        private final Handler handler;
        private final BluetoothServerSocket bss;
        private BluetoothChat chat;

        public BluetoothServer(BluetoothAdapter adapter, Handler handler) throws IOException {
            super();
            this.adapter = adapter;
            this.handler = handler;
            this.bss = adapter.listenUsingRfcommWithServiceRecord(BLUETOOTH_APP_NAME, BLUETOOTH_APP_UUID);
        }

        public BluetoothChat getBluetoothChat() {
            return this.chat;
        }

        @Override
        public void run() {
            BluetoothSocket bs = null;

            while (bs == null) {
                Message message = Message.obtain();
                message.what = STATE_CONNECTING;
                handler.sendMessage(message);

                try {
                    bs = bss.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if (adapter.isDiscovering()) {
                    adapter.cancelDiscovery();
                }

                if (bs != null) {
                    message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    handler.obtainMessage(2000).sendToTarget();
                    chat = new BluetoothChat(bs, handler);
                    chat.start();

                    break;
                }
            }
        }
    }

    private static class BluetoothClient extends Thread {

        private final BluetoothAdapter adapter;
        private final BluetoothSocket bs;
        private final Handler handler;
        private BluetoothChat chat;

        public BluetoothClient(BluetoothAdapter adapter, BluetoothDevice device, Handler handler) throws IOException {
            super();
            this.adapter = adapter;
            this.handler = handler;
            this.bs = device.createRfcommSocketToServiceRecord(BLUETOOTH_APP_UUID);
        }

        public BluetoothChat getBluetoothChat() {
            return this.chat;
        }

        @Override
        public void run() {
            Message message = null;

            try {
                bs.connect();

                message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();

                message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }

            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }

            handler.obtainMessage(1000).sendToTarget();

            chat = new BluetoothChat(bs, handler);
            chat.start();
        }
    }

    private static class BluetoothChat extends Thread {

        private static BluetoothChat instance;

        private final BluetoothSocket bs;
        private final Handler handler;
        private final InputStream is;
        private final OutputStream os;

        public BluetoothChat(BluetoothSocket socket, Handler handler) {
            super();
            this.bs = socket;
            this.handler = handler;

            InputStream is = null;
            OutputStream os = null;
            try {
                is = socket.getInputStream();
                os = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.is = is;
            this.os = os;

            BluetoothChat.instance = this;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;

            while (true) {
                try {
                    bytes = is.read(buffer);
                    handler.obtainMessage(STATE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public static BluetoothChat getCurrentInstance() {
            return BluetoothChat.instance;
        }

        public void write(byte[] buffer) {
            try {
                os.write(buffer);
                handler.obtainMessage(STATE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
