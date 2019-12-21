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
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * @date 2017/11/23
 */
//
public class ProviderInvokerWrapper<T> implements Invoker {
//    DelegateProviderMetaDataInvoker
    private Invoker<T> invoker;
    private URL originUrl;//registry://192.168.50.251:2181/com.alibaba.dubbo.registry.RegistryService?application=dubbo-provider&dubbo=2.0.2&export=dubbo%3A%2F%2F172.28.82.218%3A20880%2Fcom.tianhe.lianxi.dubbo.api.HelloFacade%3Fanyhost%3Dtrue%26application%3Ddubbo-provider%26bean.name%3Dproviders%3Adubbo%3Acom.tianhe.lianxi.dubbo.api.HelloFacade%3A1.0.0%3AhelloGroup%26bind.ip%3D172.28.82.218%26bind.port%3D20880%26dubbo%3D2.0.2%26executes%3D200%26generic%3Dfalse%26group%3DhelloGroup%26interface%3Dcom.tianhe.lianxi.dubbo.api.HelloFacade%26methods%3DsayHello%26pid%3D14414%26revision%3D1.0.0%26side%3Dprovider%26timestamp%3D1573209627465%26version%3D1.0.0&pid=14414&registry=zookeeper&timestamp=1573209627099
    private URL registryUrl;//zookeeper://192.168.50.251:2181/com.alibaba.dubbo.registry.RegistryService?application=dubbo-provider&dubbo=2.0.2&export=dubbo%3A%2F%2F172.28.82.218%3A20880%2Fcom.tianhe.lianxi.dubbo.api.HelloFacade%3Fanyhost%3Dtrue%26application%3Ddubbo-provider%26bean.name%3Dproviders%3Adubbo%3Acom.tianhe.lianxi.dubbo.api.HelloFacade%3A1.0.0%3AhelloGroup%26bind.ip%3D172.28.82.218%26bind.port%3D20880%26dubbo%3D2.0.2%26executes%3D200%26generic%3Dfalse%26group%3DhelloGroup%26interface%3Dcom.tianhe.lianxi.dubbo.api.HelloFacade%26methods%3DsayHello%26pid%3D14414%26revision%3D1.0.0%26side%3Dprovider%26timestamp%3D1573209627465%26version%3D1.0.0&pid=14414&timestamp=1573209627099
    private URL providerUrl;//dubbo://172.28.82.218:20880/com.tianhe.lianxi.dubbo.api.HelloFacade?anyhost=true&application=dubbo-provider&bean.name=providers:dubbo:com.tianhe.lianxi.dubbo.api.HelloFacade:1.0.0:helloGroup&dubbo=2.0.2&executes=200&generic=false&group=helloGroup&interface=com.tianhe.lianxi.dubbo.api.HelloFacade&methods=sayHello&pid=14414&revision=1.0.0&side=provider&timestamp=1573209627465&version=1.0.0
    private volatile boolean isReg;

    public ProviderInvokerWrapper(Invoker<T> invoker,URL registryUrl,URL providerUrl) {
        this.invoker = invoker;
        this.originUrl = URL.valueOf(invoker.getUrl().toFullString());
        this.registryUrl = URL.valueOf(registryUrl.toFullString());
        this.providerUrl = providerUrl;
    }

    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public URL getUrl() {
        return invoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void destroy() {
        invoker.destroy();
    }

    public URL getOriginUrl() {
        return originUrl;
    }

    public URL getRegistryUrl() {
        return registryUrl;
    }

    public URL getProviderUrl() {
        return providerUrl;
    }

    public Invoker<T> getInvoker() {
        return invoker;
    }

    public boolean isReg() {
        return isReg;
    }

    public void setReg(boolean reg) {
        isReg = reg;
    }
}
