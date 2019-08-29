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
package org.apache.dubbo.bootstrap;

import com.alibaba.dubbo.config.DubboShutdownHook;
import com.alibaba.dubbo.config.ServiceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * A bootstrap class to easily start and stop Dubbo via programmatic API.
 * The bootstrap class will be responsible to cleanup the resources during stop.
 * 通过编程API轻松启动和停止Dubbo的引导类。引导类将负责在停止期间清理资源。
 */
public class DubboBootstrap {

    /**
     * The list of ServiceConfig
     */
    private List<ServiceConfig> serviceConfigList;

    /**
     * Whether register the shutdown hook during start? 启动时是否登记关机挂钩?
     */
    private final boolean registerShutdownHookOnStart;

    /**
     * The shutdown hook used when Dubbo is running under embedded environment 在嵌入式环境下运行Dubbo时使用的关闭钩子
     */
    private DubboShutdownHook shutdownHook;

    public DubboBootstrap() {
        this(true, DubboShutdownHook.getDubboShutdownHook());
    }

    public DubboBootstrap(boolean registerShutdownHookOnStart) {
        this(registerShutdownHookOnStart, DubboShutdownHook.getDubboShutdownHook());
    }

    public DubboBootstrap(boolean registerShutdownHookOnStart, DubboShutdownHook shutdownHook) {
        this.serviceConfigList = new ArrayList<ServiceConfig>();
        this.shutdownHook = shutdownHook;
        this.registerShutdownHookOnStart = registerShutdownHookOnStart;
    }

    /**
     * Register service config to bootstrap, which will be called during {@link DubboBootstrap#stop()} 将服务配置注册到bootstrap，将在stop()期间调用它
     * @param serviceConfig the service
     * @return the bootstrap instance
     */
    public DubboBootstrap registerServiceConfig(ServiceConfig serviceConfig) {
        serviceConfigList.add(serviceConfig);
        return this;
    }

    public void start() {
        if (registerShutdownHookOnStart) {
//            启动时注册销毁钩子方法
            registerShutdownHook();
        } else {
            // DubboShutdown hook has been registered in AbstractConfig,
            // we need to remove it explicitly
            removeShutdownHook();
        }
        for (ServiceConfig serviceConfig: serviceConfigList) {
//            解析配置并注册服务=》
            serviceConfig.export();
        }
    }

    public void stop() {
        for (ServiceConfig serviceConfig: serviceConfigList) {
//            停止服务
            serviceConfig.unexport();
        }
//        销毁资源，包括注册信息、协议=》
        shutdownHook.destroyAll();
        if (registerShutdownHookOnStart) {
            removeShutdownHook();
        }
    }

    /**
     * Register the shutdown hook
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Remove this shutdown hook
     */
    public void removeShutdownHook() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        catch (IllegalStateException ex) {
            // ignore - VM is already shutting down
        }
    }
}
