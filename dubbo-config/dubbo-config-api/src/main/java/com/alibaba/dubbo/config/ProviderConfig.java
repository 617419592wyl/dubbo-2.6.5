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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.common.threadpool.ThreadPool;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.remoting.Dispatcher;
import com.alibaba.dubbo.remoting.Transporter;
import com.alibaba.dubbo.remoting.exchange.Exchanger;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;

import java.util.Arrays;

/**
 * ProviderConfig
 *
 * @export
 * @see com.alibaba.dubbo.config.ProtocolConfig
 * @see com.alibaba.dubbo.config.ServiceConfig
 */
//服务提供者配置
public class ProviderConfig extends AbstractServiceConfig {
    private static final long serialVersionUID = 6913423882496634749L;

    // ======== protocol default values, it'll take effect when protocol's attributes are not set 协议默认值，不设置协议属性时生效 ========

    // service IP addresses (used when there are multiple network cards available) 指定服务ip，当多网卡时使用
    private String host;

    // service port
    private Integer port;

    // context path
    private String contextpath;

    // thread pool 线程池类型，默认值fixed，可选fixed、cached
    private String threadpool;

    // thread pool size (fixed size) 指定线程池线程数，默认值200
    private Integer threads;

    // IO thread pool size (fixed size) io线程数，默认cpu线程数+1
    private Integer iothreads;

    // thread pool queue length 默认值0，dubbo建议任务直接处理不要加入队列
    private Integer queues;

    // max acceptable connections provider 默认值9，服务提供者的最大连接数
    private Integer accepts;

    // protocol codec 协议编解码支持，默认值dubbo
    private String codec;

    // charset
    private String charset;

    // payload max length 请求和响应的长度限制，单位为字节，默认8m
    private Integer payload;

    // buffer size
    private Integer buffer;

    // transporter 网络传输方式，可选netty、mina、grizzly、http等，客户端和服务端可以单独设置
    private String transporter;

    // how information gets exchanged 信息交换方式 header，默认HeaderExchanger
    // <dubbo:protocol exchanger=""/>
//    <dubbo:provider exchanger=""/>
    private String exchanger;

    // thread dispatching mode 线程转发模式，默认值all
//    all 所有消息都派发到线程池，包括请求、响应、连接事件、断开事件、心跳监测等
//    connection 在io线程上，将连接断开事件放入队列，有序逐个执行，其他消息派发到线程池
//    direct 所有消息都不派发到线程池，全部在io线程上直接执行
//    execution 只请求消息派发到线程池，不含响应，响应和其他连接断开事件，心跳检测等消息，直接在io线程上执行
//    message 只有请求响应消息派发到线程池，其他连接断开事件，心跳检测等消息，直接在io线程上执行
    private String dispatcher;

    // networker 网络连接器
//    multicast=com.alibaba.dubbo.remoting.p2p.support.MulticastNetworker
//file=com.alibaba.dubbo.remoting.p2p.support.FileNetworker
    private String networker;

    // server impl server端协议，dubbo协议默认netty，http协议默认servlet
    private String server;

    // client impl client端协议，dubbo协议默认netty，http协议默认servlet
    private String client;

    // supported telnet commands, separated with comma. 支持的telnet命令
//    clear=com.alibaba.dubbo.remoting.telnet.support.command.ClearTelnetHandler
//exit=com.alibaba.dubbo.remoting.telnet.support.command.ExitTelnetHandler
//help=com.alibaba.dubbo.remoting.telnet.support.command.HelpTelnetHandler
//status=com.alibaba.dubbo.remoting.telnet.support.command.StatusTelnetHandler
//log=com.alibaba.dubbo.remoting.telnet.support.command.LogTelnetHandler
    private String telnet;

    // command line prompt
    private String prompt;

    // status check 默认支持 memory 内存状态检查，load 加载状态检查
    //​RegistryStatusChecker 服务注册状态检查
    //ServerStatusChecker server状态检查
    //SpringStatusChecker spring容器状态检查
    //ThreadPoolStatusChecker 线程池状态检查
//    这四中spi没有继承进来，需要自己扩展
    private String status;

    // wait time when stop 服务停止等待时间
    private Integer wait;

    // if it's default
    private Boolean isDefault;

    @Deprecated
    public void setProtocol(String protocol) {
        this.protocols = Arrays.asList(new ProtocolConfig[]{new ProtocolConfig(protocol)});
    }

    @Parameter(excluded = true)
    public Boolean isDefault() {
        return isDefault;
    }

    @Deprecated
    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Parameter(excluded = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Parameter(excluded = true)
    public Integer getPort() {
        return port;
    }

    @Deprecated
    public void setPort(Integer port) {
        this.port = port;
    }

    @Deprecated
    @Parameter(excluded = true)
    public String getPath() {
        return getContextpath();
    }

    @Deprecated
    public void setPath(String path) {
        setContextpath(path);
    }

    @Parameter(excluded = true)
    public String getContextpath() {
        return contextpath;
    }

    public void setContextpath(String contextpath) {
        checkPathName("contextpath", contextpath);
        this.contextpath = contextpath;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
//        //        这里可以同过threadpool参数指定线程池，默认是fixed
        checkExtension(ThreadPool.class, "threadpool", threadpool);
        this.threadpool = threadpool;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getIothreads() {
        return iothreads;
    }

    public void setIothreads(Integer iothreads) {
        this.iothreads = iothreads;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public Integer getAccepts() {
        return accepts;
    }

    public void setAccepts(Integer accepts) {
        this.accepts = accepts;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Integer getPayload() {
        return payload;
    }

    public void setPayload(Integer payload) {
        this.payload = payload;
    }

    public Integer getBuffer() {
        return buffer;
    }

    public void setBuffer(Integer buffer) {
        this.buffer = buffer;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getTelnet() {
        return telnet;
    }

    public void setTelnet(String telnet) {
        checkMultiExtension(TelnetHandler.class, "telnet", telnet);
        this.telnet = telnet;
    }

    @Parameter(escaped = true)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        checkMultiExtension(StatusChecker.class, "status", status);
        this.status = status;
    }

    @Override
    public String getCluster() {
        return super.getCluster();
    }

    @Override
    public Integer getConnections() {
        return super.getConnections();
    }

    @Override
    public Integer getTimeout() {
        return super.getTimeout();
    }

    @Override
    public Integer getRetries() {
        return super.getRetries();
    }

    @Override
    public String getLoadbalance() {
        return super.getLoadbalance();
    }

    @Override
    public Boolean isAsync() {
        return super.isAsync();
    }

    @Override
    public Integer getActives() {
        return super.getActives();
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        checkExtension(Transporter.class, "transporter", transporter);
        this.transporter = transporter;
    }

    public String getExchanger() {
        return exchanger;
    }

    public void setExchanger(String exchanger) {
        checkExtension(Exchanger.class, "exchanger", exchanger);
        this.exchanger = exchanger;
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #getDispatcher()}
     */
    @Deprecated
    @Parameter(excluded = true)
    public String getDispather() {
        return getDispatcher();
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #setDispatcher(String)}
     */
    @Deprecated
    public void setDispather(String dispather) {
        setDispatcher(dispather);
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(String dispatcher) {
        checkExtension(Dispatcher.class, Constants.DISPATCHER_KEY, exchanger);
        checkExtension(Dispatcher.class, "dispather", exchanger);
        this.dispatcher = dispatcher;
    }

    public String getNetworker() {
        return networker;
    }

    public void setNetworker(String networker) {
        this.networker = networker;
    }

    public Integer getWait() {
        return wait;
    }

    public void setWait(Integer wait) {
        this.wait = wait;
    }

}