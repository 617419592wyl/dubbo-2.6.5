/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.listener;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.ExporterListener;
import com.alibaba.dubbo.rpc.Invoker;

import java.util.List;

/**
 * ListenerExporter
 */
//
public class ListenerExporterWrapper<T> implements Exporter<T> {

    private static final Logger logger = LoggerFactory.getLogger(ListenerExporterWrapper.class);

//    DubboExporter
//    injvmExporter interface com.tianhe.lianxi.dubbo.api.HelloFacade -> injvm://127.0.0.1/com.tianhe.lianxi.dubbo.api.HelloFacade?anyhost=true&application=dubbo-provider&bean.name=providers:dubbo:com.tianhe.lianxi.dubbo.api.HelloFacade:1.0.0:helloGroup&bind.ip=172.28.82.218&bind.port=20880&dubbo=2.0.2&executes=200&generic=false&group=helloGroup&interface=com.tianhe.lianxi.dubbo.api.HelloFacade&methods=sayHello&pid=2528&revision=1.0.0&side=provider&timestamp=1573207327686&version=1.0.0
    private final Exporter<T> exporter;

    private final List<ExporterListener> listeners;

    public ListenerExporterWrapper(Exporter<T> exporter, List<ExporterListener> listeners) {
        if (exporter == null) {
            throw new IllegalArgumentException("exporter == null");
        }
        this.exporter = exporter;
        this.listeners = listeners;
        if (listeners != null && !listeners.isEmpty()) {
            RuntimeException exception = null;
            for (ExporterListener listener : listeners) {
                if (listener != null) {
                    try {
                        listener.exported(this);
                    } catch (RuntimeException t) {
                        logger.error(t.getMessage(), t);
                        exception = t;
                    }
                }
            }
            if (exception != null) {
                throw exception;
            }
        }
    }

    @Override
    public Invoker<T> getInvoker() {
        return exporter.getInvoker();
    }

    @Override
    public void unexport() {
        try {
            exporter.unexport();
        } finally {
            if (listeners != null && !listeners.isEmpty()) {
                RuntimeException exception = null;
                for (ExporterListener listener : listeners) {
                    if (listener != null) {
                        try {
                            listener.unexported(this);
                        } catch (RuntimeException t) {
                            logger.error(t.getMessage(), t);
                            exception = t;
                        }
                    }
                }
                if (exception != null) {
                    throw exception;
                }
            }
        }
    }

}
