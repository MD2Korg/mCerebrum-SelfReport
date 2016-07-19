package org.md2k.selfreport;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeJSONObject;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.selfreport.config.Config;
import org.md2k.selfreport.config.ConfigManager;
import org.md2k.utilities.Report.Log;

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
    public static final long REPEAT_TIME = 10000;
    DataKitAPI dataKitAPI;
    ConfigManager configManager;
    Handler handler;
    ArrayList<Config> configs;
    ArrayList<DataSourceClient> dataSourceClients;
    HashMap<Integer, Integer> hashMap;

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        handler = new Handler();
        hashMap=new HashMap<>();

        configManager = new ConfigManager(this);
        if (isValid())
            connectDataKit();
    }

    private boolean isValid() {
        configs = new ArrayList<>();
        if (configManager == null || configManager.getConfig() == null || !configManager.isValid())
            return false;
        for (int i = 0; i < configManager.getConfig().size(); i++) {
            if (configManager.getConfig().get(i).getType().equals(SYSTEM)) {
                configs.add(configManager.getConfig().get(i));
            }
        }
        if (configs.size() != 0)
            dataSourceClients = new ArrayList<>(configs.size());
        return configs.size() != 0;
    }

    Runnable runnableSubscribe = new Runnable() {
        @Override
        public void run() {
            try {
                boolean flag = true;
                for (int i = 0; i < configs.size(); i++) {
                    if (dataSourceClients.get(i) == null) {
                        DataSourceBuilder dataSourceBuilder = configs.get(i).getListen_datasource().toDataSourceBuilder();
                        ArrayList<DataSourceClient> dataSourceClientAll = DataKitAPI.getInstance(ServiceSelfReport.this).find(dataSourceBuilder);
                        if (dataSourceClientAll.size() >= 1) {
                            dataSourceClients.set(i, dataSourceClientAll.get(0));
                            final int finalI = i;
                            DataKitAPI.getInstance(ServiceSelfReport.this).subscribe(dataSourceClients.get(0), new OnReceiveListener() {
                                @Override
                                public void onReceived(DataType dataType) {
                                    HashMap<String, String> parameters=configs.get(finalI).getParameters();
                                    int minTime=Integer.getInteger(parameters.get("s1"));
                                    int maxTime=Integer.getInteger(parameters.get("s2"));
                                    Random rn = new Random();
                                    long answer = rn.nextInt(maxTime-minTime) + minTime;
                                    DataTypeJSONObject dataTypeJSONObject=new DataTypeJSONObject(DateTime.getDateTime(), null);
                                    RunnableInsert runnableInsert=new RunnableInsert(dataKitAPI, configs.get(finalI).getDatasource().toDataSourceBuilder(), dataTypeJSONObject);
                                    handler.postDelayed(runnableInsert, answer);
                                }
                            });
                        } else
                            flag = false;
                    }
                }
                if (!flag)
                    handler.postDelayed(this, REPEAT_TIME);
            } catch (DataKitException e) {
                e.printStackTrace();
            }
        }
    };

    private void connectDataKit() {
        dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
        try {
            dataKitAPI.connect(new OnConnectionListener() {
                @Override
                public void onConnected() {
                    handler.post(runnableSubscribe);
                }
            });
        } catch (DataKitException e) {
            stopSelf();
        }
    }

    private void disconnectDataKit() {
        try {
            DataKitAPI dataKitAPI = DataKitAPI.getInstance(this);
            if (dataKitAPI != null && dataKitAPI.isConnected()) {
                for (int i = 0; i < dataSourceClients.size(); i++) {
                    if (dataSourceClients.get(i) != null)
                        dataKitAPI.unsubscribe(dataSourceClients.get(i));
                }
                dataKitAPI.disconnect();
            }
        } catch (DataKitException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        disconnectDataKit();
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}