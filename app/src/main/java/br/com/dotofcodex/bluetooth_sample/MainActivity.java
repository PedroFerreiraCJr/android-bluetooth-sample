package br.com.dotofcodex.bluetooth_sample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_BLUETOOTH = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
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

    private BluetoothAdapter adapter;
    private IntentFilter filterActionFound;
    private IntentFilter filterState;
    private BroadcastReceiver receiverAction;
    private BroadcastReceiver receiverState;
    private List<BluetoothDevice> bts;

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

        discover.setOnClickListener((View v) -> {
            requestBluetoothAdditionalPermission();
            startDiscover();
        });

        discoverability.setOnClickListener((View v) -> {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            int seconds = 60 * 2;
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
            startActivity(intent);
        });

        filterActionFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        receiverAction = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (!bts.contains(device)) {
                        bts.add(device);
                    }

                    for (BluetoothDevice dev : bts) {
                        Log.i(TAG, String.format("%s, %s", dev.getName(), dev.getAddress()));
                    }
                }
            }
        };

        filterState = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
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

        if (bts == null) {
            bts = new ArrayList<>();
        }

        registerReceiver(receiverAction, filterActionFound);
        registerReceiver(receiverState, filterState);
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
}
