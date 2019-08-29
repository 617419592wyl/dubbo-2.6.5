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
package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.URL;

import java.util.List;

/**
 * RegistryService. (SPI, Prototype, ThreadSafe)
 *
 * @see com.alibaba.dubbo.registry.Registry
 * @see com.alibaba.dubbo.registry.RegistryFactory#getRegistry(URL)
 */
public interface RegistryService {

    /**
     * Register data, such as : provider service, consumer address, route rule, override rule and other data.
     * <p>
     * Registering is required to support the contract:<br>
     * 1. When the URL sets the check=false parameter. When the registration fails, the exception is not thrown and retried in the background. Otherwise, the exception will be thrown.<br>
     * 2. When URL sets the dynamic=false parameter, it needs to be stored persistently, otherwise, it should be deleted automatically when the registrant has an abnormal exit.<br>
     * 3. When the URL sets category=routers, it means classified storage, the default category is providers, and the data can be notified by the classified section. <br>
     * 4. When the registry is restarted, network jitter, data can not be lost, including automatically deleting data from the broken line.<br>
     * 5. Allow URLs which have the same URL but different parameters to coexist,they can't cover each other.<br>
     *
     * @param url  Registration information , is not allowed to be empty, e.g: dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     *             注册数据，如:提供者服务、用户地址、路由规则、覆盖规则等。
    支持本合同需要注册:当URL设置check=false参数时。当注册失败时，异常不会被抛出并在后台重试。否则，将抛出异常。2。当URL设置dynamic=false参数时，需要持久存储，否则，当注册者有异常退出时，需要自动删除。3.当URL设置category=router时，表示分类存储，默认类别是provider，数据可以由分类部分通知。4所示。注册表重启后，网络抖动，数据无法丢失，包括自动从断行中删除数据。5。允许具有相同URL但不同参数的URL共存，它们不能相互覆盖。
     */
    void register(URL url);

    /**
     * Unregister
     * <p>
     * Unregistering is required to support the contract:<br>
     * 1. If it is the persistent stored data of dynamic=false, the registration data can not be found, then the IllegalStateException is thrown, otherwise it is ignored.<br>
     * 2. Unregister according to the full url match.<br>
     *
     * @param url Registration information , is not allowed to be empty, e.g: dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     *            支持本合同，需要注销注册:如果是dynamic=false的持久化存储数据，则无法找到注册数据，则抛出IllegalStateException，否则将忽略它。2。根据完整的url匹配取消注册。
     */
    void unregister(URL url);

    /**
     * Subscrib to eligible registered data and automatically push when the registered data is changed.
     * <p>
     * Subscribing need to support contracts:<br>
     * 1. When the URL sets the check=false parameter. When the registration fails, the exception is not thrown and retried in the background. <br>
     * 2. When URL sets category=routers, it only notifies the specified classification data. Multiple classifications are separated by commas, and allows asterisk to match, which indicates that all categorical data are subscribed.<br>
     * 3. Allow interface, group, version, and classifier as a conditional query, e.g.: interface=com.alibaba.foo.BarService&version=1.0.0<br>
     * 4. And the query conditions allow the asterisk to be matched, subscribe to all versions of all the packets of all interfaces, e.g. :interface=*&group=*&version=*&classifier=*<br>
     * 5. When the registry is restarted and network jitter, it is necessary to automatically restore the subscription request.<br>
     * 6. Allow URLs which have the same URL but different parameters to coexist,they can't cover each other.<br>
     * 7. The subscription process must be blocked, when the first notice is finished and then returned.<br>
     *
     * @param url      Subscription condition, not allowed to be empty, e.g. consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener A listener of the change event, not allowed to be empty
     *                 订阅符合条件的注册数据，并在注册数据更改时自动推送。
    1.订阅需要支持的合同:当URL设置check=false参数时。当注册失败时，异常不会被抛出并在后台重试。2。当URL设置category=router时，它只通知指定的分类数据。多个分类用逗号分隔，并允许星号匹配，这表示订阅了所有分类数据。3.允许接口、组、版本和分类器作为条件查询，例如:接口=com.alibaba.foo.BarService&version=1.0.0 4。查询条件允许星号匹配，订阅所有接口所有包的所有版本，如:接口=*&group=*&version=*&classifier=* 5。当注册表重新启动并出现网络抖动时，需要自动恢复订阅请求。6。允许具有相同URL但不同参数的URL共存，它们不能相互覆盖。7所示。当第一个通知完成并返回时，必须阻塞订阅流程。
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * Unsubscribe
     * <p>
     * Unsubscribing is required to support the contract:<br>
     * 1. If don't subscribe, ignore it directly.<br>
     * 2. Unsubscribe by full URL match.<br>
     *
     * @param url      Subscription condition, not allowed to be empty, e.g. consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener A listener of the change event, not allowed to be empty
     *                 支持本合同需要取消订阅:如果不订阅，直接忽略它。2。通过完整的URL匹配取消订阅。
     */
    void unsubscribe(URL url, NotifyListener listener);

    /**
     * Query the registered data that matches the conditions. Corresponding to the push mode of the subscription, this is the pull mode and returns only one result.
     *
     * @param url Query condition, is not allowed to be empty, e.g. consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @return The registered information list, which may be empty, the meaning is the same as the parameters of {@link com.alibaba.dubbo.registry.NotifyListener#notify(List<URL>)}.
     * @see com.alibaba.dubbo.registry.NotifyListener#notify(List)
     * 查询与条件匹配的已注册数据。对应于订阅的push模式，这是pull模式，只返回一个结果。
     */
    List<URL> lookup(URL url);

}