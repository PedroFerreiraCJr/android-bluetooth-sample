package br.com.dotofcodex.bluetooth_sample;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_BLUETOOTH = 1;

    @BindView(R.id.bt_enable)
    protected Button enable;

    @BindView(R.id.bt_disable)
    protected Button disable;

    @BindView(R.id.tv_status)
    protected TextView status;

    private BluetoothAdapter adapter;

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
        });

        disable.setOnClickListener((View v) -> {
            if (adapter.isEnabled()) {
                adapter.disable();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                Snackbar.make(getWindow().getDecorView(), "Bluetooth permission granted", Snackbar.LENGTH_SHORT).show();
            }
            else {
                Snackbar.make(getWindow().getDecorView(), "Bluetooth permission NOT granted", Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}
