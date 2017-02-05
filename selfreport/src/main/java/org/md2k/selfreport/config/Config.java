package org.md2k.selfreport.config;

import org.md2k.datakitapi.source.datasource.DataSource;

import java.util.ArrayList;
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
public class Config {
    private String id;
    private String type;
    private String name;
    private DataSource datasource;
    private DataSource listen_datasource;
    private Parameter parameters;
    public class Parameter{
        public static final String SINGLE_CHOICE="SINGLE_CHOICE";
        public static final String MULTIPLE_CHOICE="MULTIPLE_CHOICE";
        public static final String SIMPLE="SIMPLE";
        String title;
        String description;
        String icon;
        String type;
        String[] options;
        String positive_button;
        String negative_button;
        String neutral_button;
        long trigger_time;

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String[] getOptions() {
            return options;
        }

        public String getPositive_button() {
            return positive_button;
        }

        public String getNegative_button() {
            return negative_button;
        }

        public String getNeutral_button() {
            return neutral_button;
        }

        public String getIcon() {
            return icon;
        }

        public long getTrigger_time() {
            return trigger_time;
        }
    }

    public String getId() {
        return id;
    }

    public DataSource getDatasource() {
        return datasource;
    }

    public Parameter getParameters() {
        return parameters;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public DataSource getListen_datasource() {
        return listen_datasource;
    }
}
