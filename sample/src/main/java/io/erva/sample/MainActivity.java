package io.erva.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import static io.erva.sample.utils.ConfigFileConverter.readAndFixConfigFile;

public class MainActivity extends Activity {

    private static final int PICKFILE_RESULT_CODE = 11;
    private String config;
    private TextView configStateTextView;
    private Button startVpnButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configStateTextView = findViewById(R.id.tv_config_state);
        findViewById(R.id.btn_chose_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
            }
        });
        startVpnButton  = findViewById(R.id.btn_start_vpn);
        startVpnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //todo start vpn
                Toast.makeText(MainActivity.this, config, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_RESULT_CODE && resultCode == RESULT_OK && data != null) {
            Uri returnUri = data.getData();
            try {
                assert returnUri != null;
                BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getContentResolver().openInputStream(returnUri))));
                config = readAndFixConfigFile(br);
                br.close();
                startVpnButton.setVisibility(View.VISIBLE);
                configStateTextView.setText(R.string.config_file_loaded);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("MainActivity", "File not found.");
                configStateTextView.setText("Error while loading config file");
            }
        }
    }


}
