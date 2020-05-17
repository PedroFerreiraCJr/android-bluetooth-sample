package br.com.dotofcodex.bluetooth_sample;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_BLUETOOTH = 1;
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

    private BluetoothAdapter adapter;
    private IntentFilter filter;
    private BroadcastReceiver receiver;
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

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        receiver = new BroadcastReceiver() {
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

        adapter.startDiscovery();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (bts == null) {
            bts = new ArrayList<>();
        }

        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (bts != null && !bts.isEmpty()) {
            bts.clear();
        }

        unregisterReceiver(receiver);
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
