package org.md2k.selfreport;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeJSONObject;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.utilities.Report.Log;

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
public class RunnableInsert implements Runnable {
    private static final String TAG = RunnableInsert.class.getSimpleName();
    DataTypeJSONObject dataTypeJSONObject;
    DataSourceBuilder dataSourceBuilder;
    Context context;

    public RunnableInsert(Context context, DataSourceBuilder dataSourceBuilder, DataTypeJSONObject dataTypeJSONObject) {
        Log.d(TAG,"RunnableInsert...");
        this.context=context;
        this.dataSourceBuilder = dataSourceBuilder;
        this.dataTypeJSONObject = dataTypeJSONObject;
    }

    @Override
    public void run() {
        writeToDataKit();
    }

    private boolean writeToDataKit() {
        try {
            Log.d(TAG,"insert...");
            DataKitAPI dataKitAPI = DataKitAPI.getInstance(context);
            DataSourceClient dataSourceClient = dataKitAPI.register(dataSourceBuilder);
            dataKitAPI.insert(dataSourceClient, dataTypeJSONObject);
            return true;
        } catch (Exception e) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.INTENT_STOP));
            return false;
        }
    }
}
