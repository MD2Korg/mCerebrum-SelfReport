package org.md2k.selfreport;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeJSONObject;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.messagehandler.ResultCallback;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.selfreport.config.Config;
import org.md2k.selfreport.config.ConfigManager;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.Report.LogStorage;
import org.md2k.utilities.UI.AlertDialogs;
import org.md2k.utilities.data_format.Event;
import org.md2k.utilities.icons.Icon;
import org.md2k.utilities.permission.PermissionInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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

public class ServiceSelfReport extends Service {
    private static final String TAG = ServiceSelfReport.class.getSimpleName();
    public static final String SYSTEM = "system";
    public static final String ID = "id";
    public static final String TYPE = "type";
    public static final long REPEAT_TIME = 10000;
    ConfigManager configManager;
    Handler handler;
    DataSourceClient[] dataSourceClients;
    boolean isAlreadyShown;

    @Override
    public void onCreate() {
        super.onCreate();
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.getPermissions(this, new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                if (!result) {
                    Toast.makeText(getApplicationContext(), "!PERMISSION DENIED !!! Could not continue...", Toast.LENGTH_SHORT).show();
                    stopSelf();
                } else {
                    load();
                }
            }
        });
    }
    void load(){
        LogStorage.startLogFileStorageProcess(getApplicationContext().getPackageName());
        Log.w(TAG,"time="+ DateTime.convertTimeStampToDateTime(DateTime.getDateTime())+",timestamp="+ DateTime.getDateTime()+",service_start");

        Log.d(TAG, "onCreate()...");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Constants.INTENT_STOP));
        handler = new Handler();
        configManager = new ConfigManager(this);
        Log.d(TAG, "onCreate()...configManager..isValid=" + configManager.isValid());
        if (configManager.isValid()) {
            Log.d(TAG, "onCreate()...configManager..size=" + configManager.getConfig().size());
            dataSourceClients = new DataSourceClient[configManager.getConfig().size()];
            for (int i = 0; i < configManager.getConfig().size(); i++)
                dataSourceClients[i] = null;
            connectDataKit();
        }
        isAlreadyShown = false;
    }

    private synchronized void connectDataKit() {
        DataKitAPI dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
        try {
            dataKitAPI.connect(new OnConnectionListener() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "DataKit connected...");
                    registerAll();
                    handler.post(runnableSubscribe);
                }
            });
        } catch (DataKitException e) {
            clear();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()...intent=" + intent);
        DataKitAPI dataKitAPI = DataKitAPI.getInstance(this);
        if (intent == null || !intent.hasExtra(ServiceSelfReport.ID) || !intent.hasExtra(ServiceSelfReport.TYPE))
            return START_STICKY;
        String id = intent.getStringExtra(ID);
        String type = intent.getStringExtra(TYPE);
        Log.d(TAG, "onStartCommand()...id=" + id + " type=" + type);
        if (!isAlreadyShown)
            showAlert(configManager.getConfig(id, type));
        return START_STICKY; // or whatever your flag
    }

    void registerAll() {
        try {
            Log.d(TAG, "registerAll()...");
            DataKitAPI dataKitAPI = DataKitAPI.getInstance(ServiceSelfReport.this);
            for (int i = 0; i < configManager.getConfig().size(); i++) {
                DataSourceBuilder dataSourceBuilder = configManager.getConfig().get(i).getDatasource().toDataSourceBuilder();
                dataKitAPI.register(dataSourceBuilder);
            }
        } catch (Exception e) {
            clear();
            stopSelf();
        }
    }

    void showAlert(final Config config) {
        Log.d(TAG, "showAlert()...");
        Drawable drawable;
        if(config.getParameters().getIcon()==null)
          drawable=Icon.get(this,Icon.Id.QUESTION_CIRCLE, ContextCompat.getColor(this,R.color.teal_500), Icon.Size.SMALL);
        else drawable=Icon.get(this, config.getParameters().getIcon(), ContextCompat.getColor(this,R.color.teal_500), Icon.Size.SMALL);
        if (config.getParameters().getType().equals(Config.Parameter.SIMPLE)) {
            Log.d(TAG, "showAlert()...YesNo");

            AlertDialogs.AlertDialog(this, config.getParameters().getTitle(), config.getParameters().getDescription(), drawable, config.getParameters().getPositive_button(), config.getParameters().getNegative_button(), config.getParameters().getNeutral_button(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        isAlreadyShown = false;
                        Toast.makeText(ServiceSelfReport.this, config.getName() + " saved...", Toast.LENGTH_SHORT).show();
                        Event event = new Event(config.getType(), config.getId(), config.getName());
                        DataTypeJSONObject dataTypeJSONObject = prepareData(event);
                        RunnableInsert runnableInsert = new RunnableInsert(ServiceSelfReport.this, config.getDatasource().toDataSourceBuilder(), dataTypeJSONObject);
                        handler.post(runnableInsert);
                    }
                }
            });
        } else {
            AlertDialogs.AlertDialogSingleChoice(this, config.getParameters().getDescription(), config.getParameters().getOptions(), 0, config.getParameters().getPositive_button(), config.getParameters().getNegative_button(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == -1) {
                        dialog.dismiss();
                    } else {
                        Toast.makeText(ServiceSelfReport.this, config.getName() + " saved...", Toast.LENGTH_SHORT).show();
                        Event event = new Event(config.getType(), config.getId(), config.getName() + "(" + config.getParameters().getOptions()[which] + ")");
                        event.addParameters("option", config.getParameters().getOptions()[which]);
                        DataTypeJSONObject dataTypeJSONObject = prepareData(event);
                        RunnableInsert runnableInsert = new RunnableInsert(ServiceSelfReport.this, config.getDatasource().toDataSourceBuilder(), dataTypeJSONObject);
                        handler.post(runnableInsert);
                    }
                }
            });
        }
    }

    private DataTypeJSONObject prepareData(Event event) {
        Gson gson = new Gson();
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(gson.toJson(event));
        return new DataTypeJSONObject(DateTime.getDateTime(), jsonObject);
    }

    Runnable runnableSubscribe = new Runnable() {
        @Override
        public void run() {
            try {
                boolean flag = true;
                for (int i = 0; i < configManager.getConfig().size(); i++) {
                    if (!configManager.getConfig().get(i).getId().equals(SYSTEM)) continue;
                    if (dataSourceClients[i] != null) continue;
                    DataSourceBuilder dataSourceBuilder = configManager.getConfig().get(i).getListen_datasource().toDataSourceBuilder();
                    final ArrayList<DataSourceClient> dataSourceClientAll = DataKitAPI.getInstance(ServiceSelfReport.this).find(dataSourceBuilder);
                    if (dataSourceClientAll.size() == 0) {
                        flag = false;
                        continue;
                    }
                    dataSourceClients[i] = dataSourceClientAll.get(0);
                    final int finalI = i;
                    DataKitAPI.getInstance(ServiceSelfReport.this).subscribe(dataSourceClientAll.get(0), new OnReceiveListener() {
                        @Override
                        public void onReceived(final DataType dataType) {
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    long trigger_time = configManager.getConfig().get(finalI).getTrigger_time();
                                    Event event = new Event(configManager.getConfig().get(finalI).getType(), configManager.getConfig().get(finalI).getId(), configManager.getConfig().get(finalI).getName());
                                    event.addParameters("receive_time", String.valueOf(dataType.getDateTime()));
                                    event.addParameters("datasource_type", dataSourceClientAll.get(0).getDataSource().getType());
                                    event.addParameters("trigger_time", String.valueOf(DateTime.getDateTime() + trigger_time));
                                    RunnableInsert runnableInsert = new RunnableInsert(ServiceSelfReport.this, configManager.getConfig().get(finalI).getDatasource().toDataSourceBuilder(), prepareData(event));
                                    handler.postDelayed(runnableInsert, trigger_time);
                                }
                            });
                            t.start();
                        }
                    });

                }
                if (!flag)
                    handler.postDelayed(this, REPEAT_TIME);
            } catch (DataKitException e) {
                e.printStackTrace();
            }
        }
    };


    private void disconnectDataKit() {
        try {
            DataKitAPI dataKitAPI = DataKitAPI.getInstance(this);
            if (dataKitAPI != null && dataKitAPI.isConnected()) {
                for (int i = 0; i < dataSourceClients.length; i++) {
                    if (dataSourceClients[i] != null) {
                        dataKitAPI.unsubscribe(dataSourceClients[i]);
                        dataSourceClients[i] = null;
                    }
                }
                dataKitAPI.disconnect();
            }
        } catch (DataKitException e) {
            e.printStackTrace();
        }
    }
    synchronized void clear(){
        Log.w(TAG,"time="+ DateTime.convertTimeStampToDateTime(DateTime.getDateTime())+",timestamp="+ DateTime.getDateTime()+",service_stop");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mMessageReceiver);
        disconnectDataKit();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "...onDestroy()");
        clear();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG,"time="+ DateTime.convertTimeStampToDateTime(DateTime.getDateTime())+",timestamp="+ DateTime.getDateTime()+",broadcast_receiver_stop_service");
            clear();
            stopSelf();
        }
    };

}
