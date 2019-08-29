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
 * NotifyListener. (API, Prototype, ThreadSafe)
 *
 * @see com.alibaba.dubbo.registry.RegistryService#subscribe(URL, NotifyListener)
 */
public interface NotifyListener {

    /**
     * Triggered when a service change notification is received.
     * <p>
     * Notify needs to support the contract: <br>
     * 1. Always notifications on the service interface and the dimension of the data type. that is, won't notify part of the same type data belonging to one service. Users do not need to compare the results of the previous notification.<br>
     * 2. The first notification at a subscription must be a full notification of all types of data of a service.<br>
     * 3. At the time of change, different types of data are allowed to be notified separately, e.g.: providers, consumers, routers, overrides. It allows only one of these types to be notified, but the data of this type must be full, not incremental.<br>
     * 4. If a data type is empty, need to notify a empty protocol with category parameter identification of url data.<br>
     * 5. The order of notifications to be guaranteed by the notifications(That is, the implementation of the registry). Such as: single thread push, queue serialization, and version comparison.<br>
     *
     * @param urls The list of registered information , is always not empty. The meaning is the same as the return value of {@link com.alibaba.dubbo.registry.RegistryService#lookup(URL)}.
     *             当接收到服务更改通知时触发。
    通知需要支持的合同:总是在服务接口和数据类型维度上进行通知。也就是说，不会通知属于某个服务的同一类型数据的一部分。用户不需要比较上一个通知的结果。2。订阅的第一个通知必须是服务所有类型数据的完整通知。3.在变更时，允许分别通知不同类型的数据，例如:提供者、使用者、路由器、覆盖。它只允许通知这些类型中的一种，但是这种类型的数据必须是完整的，而不是增量的。4所示。如果数据类型为空，则需要用url数据的类别参数标识通知空协议。5。通知的顺序由通知保证(即注册中心的实现)。例如:单线程推送、队列序列化和版本比较。
     */
    void notify(List<URL> urls);

}