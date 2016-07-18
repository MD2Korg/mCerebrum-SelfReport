package org.md2k.selfreport;

import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeJSONObject;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.selfreport.config.Config;
import org.md2k.selfreport.config.ConfigManager;
import org.md2k.utilities.UI.AlertDialogs;
import org.md2k.utilities.data_format.Event;

import java.util.HashMap;


/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class ActivitySelfReport extends AppCompatActivity {
    private static final String TAG = ActivitySelfReport.class.getSimpleName();
    String id;
    DataKitAPI dataKitAPI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConfigManager configManager = new ConfigManager(this);
        if (configManager == null || configManager.getConfig() == null || !configManager.isValid() || !getIntent().hasExtra("id"))
            finish();
        else {
            id = getIntent().getStringExtra("id");
            for (int i = 0; i < configManager.getConfig().size(); i++) {
                if (configManager.getConfig().get(i).getId().equals(id)) {
                    prepareDatakit(configManager.getConfig().get(i));
                    break;
                }
            }
        }
    }
    private void prepareDatakit(final Config config){
        dataKitAPI= DataKitAPI.getInstance(this);
        try {
            dataKitAPI.connect(new OnConnectionListener() {
                @Override
                public void onConnected() {
                    showAlert(config);
                }
            });
        } catch (DataKitException e) {
            finish();
        }

    }
    void showAlert(final Config config){
        final HashMap<String, String> parameters = config.getParameters();
        if (parameters.size() == 2) {
            AlertDialogs.AlertDialog(this, parameters.get("s1"), parameters.get("s2"), org.md2k.utilities.R.drawable.ic_smoking_teal_48dp, "Ok", "Cancel", null, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        Toast.makeText(ActivitySelfReport.this, config.getName()+" saved...", Toast.LENGTH_SHORT).show();
                        try {
                            writeToDataKit(config.getDatasource(), parameters.get("s2"));
                        } catch (DataKitException e) {
                            e.printStackTrace();
                        }
                    }
                    finish();
                }
            });
        } else {
            final String[] items = new String[parameters.size() - 2];
            for (int i = 2; i < parameters.size(); i++) {
                items[i - 2] = (parameters.get("s" + Integer.toString(i + 1)));
            }
            AlertDialogs.AlertDialogSingleChoice(this, parameters.get("s2"), items, 0, "Ok", "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == -1) {
                        dialog.dismiss();
                        finish();
                    } else {
                        Toast.makeText(ActivitySelfReport.this, config.getName()+" saved...", Toast.LENGTH_SHORT).show();
                        try {
                            writeToDataKit(config.getDatasource(), parameters.get("s2") + " (" + items[which] + ")");
                        } catch (DataKitException e) {
                            e.printStackTrace();
                        }
                        finish();
                    }
                }
            });
        }
    }
    private boolean writeToDataKit(DataSource dataSource, String msg) throws DataKitException {
        if (!dataKitAPI.isConnected()) return false;
        Gson gson = new Gson();
        JsonObject sample = new JsonParser().parse(gson.toJson(new Event(Event.SMOKING, Event.TYPE_SELF_REPORT, msg))).getAsJsonObject();
        DataTypeJSONObject dataTypeJSONObject = new DataTypeJSONObject(DateTime.getDateTime(), sample);
        DataSourceClient dataSourceClient=dataKitAPI.register(dataSource.toDataSourceBuilder());
        dataKitAPI.insert(dataSourceClient, dataTypeJSONObject);
        return true;
    }
    @Override
    public void onDestroy(){
        if(dataKitAPI!=null)
            dataKitAPI.disconnect();
        super.onDestroy();
    }
}
