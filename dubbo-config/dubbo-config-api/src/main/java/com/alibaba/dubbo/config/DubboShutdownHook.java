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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;
import com.alibaba.dubbo.rpc.Protocol;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The shutdown hook thread to do the clean up stuff.
 * This is a singleton in order to ensure there is only one shutdown hook registered.
 * Because {@link ApplicationShutdownHooks} use {@link java.util.IdentityHashMap}
 * to store the shutdown hooks.
 * 关闭钩子线程来做清理工作。这是一个单例对象，以确保只有一个已注册的关闭钩子。因为applicationshutdownhook使用java.util。存储关闭钩子的IdentityHashMap。
 */
public class DubboShutdownHook extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DubboShutdownHook.class);

    private static final DubboShutdownHook dubboShutdownHook = new DubboShutdownHook("DubboShutdownHook");

    public static DubboShutdownHook getDubboShutdownHook() {
        return dubboShutdownHook;
    }

    /**
     * Has it already been destroyed or not?
     */
    private final AtomicBoolean destroyed;

    private DubboShutdownHook(String name) {
        super(name);
        this.destroyed = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        if (logger.isInfoEnabled()) {
            logger.info("Run shutdown hook now.");
        }
        destroyAll();
    }

    /**
     * Destroy all the resources, including registries and protocols.
     */
    public void destroyAll() {
//        自旋锁，保证只处理一次，一般在项目中多线程情况下做线程开关很好用
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        // destroy all the registries 销毁所有注册信息=》
        AbstractRegistryFactory.destroyAll();
        // destroy all the protocols 销毁所有协议信息
        ExtensionLoader<Protocol> loader = ExtensionLoader.getExtensionLoader(Protocol.class);
        for (String protocolName : loader.getLoadedExtensions()) {
            try {
                Protocol protocol = loader.getLoadedExtension(protocolName);
                if (protocol != null) {
//                    销毁协议
                    protocol.destroy();
                }
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    }


}
