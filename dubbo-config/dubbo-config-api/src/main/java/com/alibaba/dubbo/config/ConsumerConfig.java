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

/**
 * ConsumerConfig
 *
 * @export
 */
//
public class ConsumerConfig extends AbstractReferenceConfig {

    private static final long serialVersionUID = 2827274711143680600L;

    // is default or not
    private Boolean isDefault;

    // networking framework client uses: netty, mina, etc. 网络通讯中间件类型
    private String client;

    // consumer thread pool type: cached, fixed, limit, eager
//    线程池类型
//    fixed java自带线程池
//    cached java自带线程池
//    limited 线程数只增大不会减少
//    eager java的线程池增加线程策略是核心线程数占满了往队列中放，队列也放满了没超过线程池的最大线程数才会创建线程，这个线程池增加线程的策略是currentPoolSize<submittedTaskCount<maxPoolSize
// 满足这个条件时会增加线程，submittedTaskCount是dubbo扩展的一个计数器，在执行线程的时候增加计数，线程执行完减少计数。
    private String threadpool;

    // consumer threadpool core thread size 核心线程数，此参数对fixed线程池无效，fiexed类型的线程数核心线程数和最大线程数一致
    private Integer corethreads;

    // consumer threadpool thread size 线程池的最大线程数
    private Integer threads;

    // consumer threadpool queue size 消费者线程池队列长度
    private Integer queues;

    @Override
    public void setTimeout(Integer timeout) {
        super.setTimeout(timeout);
        String rmiTimeout = System.getProperty("sun.rmi.transport.tcp.responseTimeout");
        if (timeout != null && timeout > 0
                && (rmiTimeout == null || rmiTimeout.length() == 0)) {
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(timeout));
        }
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        this.threadpool = threadpool;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public Integer getCorethreads() {
        return corethreads;
    }

    public void setCorethreads(Integer corethreads) {
        this.corethreads = corethreads;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }
}