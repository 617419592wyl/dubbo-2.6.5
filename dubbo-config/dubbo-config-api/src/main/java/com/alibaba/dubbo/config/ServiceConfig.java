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
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.invoker.DelegateProviderMetaDataInvoker;
import com.alibaba.dubbo.config.model.ApplicationModel;
import com.alibaba.dubbo.config.model.ProviderModel;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.ServiceClassHolder;
import com.alibaba.dubbo.rpc.cluster.ConfiguratorFactory;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.alibaba.dubbo.common.utils.NetUtils.LOCALHOST;
import static com.alibaba.dubbo.common.utils.NetUtils.getAvailablePort;
import static com.alibaba.dubbo.common.utils.NetUtils.getLocalHost;
import static com.alibaba.dubbo.common.utils.NetUtils.isInvalidLocalHost;
import static com.alibaba.dubbo.common.utils.NetUtils.isInvalidPort;

/**
 * ServiceConfig
 *
 * @export
 */
public class ServiceConfig<T> extends AbstractServiceConfig {

    private static final long serialVersionUID = 3033787999037024738L;

    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<String, Integer>();

    private static final ScheduledExecutorService delayExportExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboServiceDelayExporter", true));
    private final List<URL> urls = new ArrayList<URL>();
    private final List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();
    // interface type
    private String interfaceName;
    private Class<?> interfaceClass;
    // reference to interface impl
    private T ref;
    // service name
    private String path;
    // method configuration
    private List<MethodConfig> methods;
    private ProviderConfig provider;
//    是否已加载过服务
    private transient volatile boolean exported;

//    是否未加载过服务
    private transient volatile boolean unexported;

    private volatile String generic;

    public ServiceConfig() {
    }

    public ServiceConfig(Service service) {
//        加载@Service服务配置=》
        appendAnnotation(Service.class, service);
    }

    @Deprecated
    private static List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }
        return protocols;
    }

    @Deprecated
    private static List<ProviderConfig> convertProtocolToProvider(List<ProtocolConfig> protocols) {
        if (protocols == null || protocols.isEmpty()) {
            return null;
        }
        List<ProviderConfig> providers = new ArrayList<ProviderConfig>(protocols.size());
        for (ProtocolConfig provider : protocols) {
            providers.add(convertProtocolToProvider(provider));
        }
        return providers;
    }

    @Deprecated
    private static ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName(provider.getProtocol().getName());
        protocol.setServer(provider.getServer());
        protocol.setClient(provider.getClient());
        protocol.setCodec(provider.getCodec());
        protocol.setHost(provider.getHost());
        protocol.setPort(provider.getPort());
        protocol.setPath(provider.getPath());
        protocol.setPayload(provider.getPayload());
        protocol.setThreads(provider.getThreads());
        protocol.setParameters(provider.getParameters());
        return protocol;
    }

    @Deprecated
    private static ProviderConfig convertProtocolToProvider(ProtocolConfig protocol) {
        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol(protocol);
        provider.setServer(protocol.getServer());
        provider.setClient(protocol.getClient());
        provider.setCodec(protocol.getCodec());
        provider.setHost(protocol.getHost());
        provider.setPort(protocol.getPort());
        provider.setPath(protocol.getPath());
        provider.setPayload(protocol.getPayload());
        provider.setThreads(protocol.getThreads());
        provider.setParameters(protocol.getParameters());
        return provider;
    }

    private static Integer getRandomPort(String protocol) {
        protocol = protocol.toLowerCase();
        if (RANDOM_PORT_MAP.containsKey(protocol)) {
            return RANDOM_PORT_MAP.get(protocol);
        }
        return Integer.MIN_VALUE;
    }

    private static void putRandomPort(String protocol, Integer port) {
        protocol = protocol.toLowerCase();
        if (!RANDOM_PORT_MAP.containsKey(protocol)) {
            RANDOM_PORT_MAP.put(protocol, port);
        }
    }

    public URL toUrl() {
        return urls.isEmpty() ? null : urls.iterator().next();
    }

    public List<URL> toUrls() {
        return urls;
    }

    @Parameter(excluded = true)
    public boolean isExported() {
        return exported;
    }

    @Parameter(excluded = true)
    public boolean isUnexported() {
        return unexported;
    }

    public synchronized void export() {
        if (provider != null) {
            if (export == null) {
                export = provider.getExport();
            }
            if (delay == null) {
                delay = provider.getDelay();
            }
        }
//        不注册服务结束
        if (export != null && !export) {
            return;
        }

        if (delay != null && delay > 0) {
            delayExportExecutor.schedule(new Runnable() {
                @Override
                public void run() {
//                    加载服务=》
                    doExport();
                }
            }, delay, TimeUnit.MILLISECONDS);
        } else {
//            =》
            doExport();
        }
    }

    protected synchronized void doExport() {
//        不注册服务结束
        if (unexported) {
            throw new IllegalStateException("Already unexported!");
        }
//        注册过服务结束，这里是用volatile变量的线程扩展性告诉其他线程有线程在进行服务export
        if (exported) {
            return;
        }
        exported = true;
//        接口名为空
        if (interfaceName == null || interfaceName.length() == 0) {
            throw new IllegalStateException("<dubbo:service interface=\"\" /> interface not allow null!");
        }
//        从系统变量读取默认配置，属性值的key值是dubbo.provider+<dubbo:application name+providerConfig的属性名=》
        checkDefault();
        if (provider != null) {
            if (application == null) {
//                <dubbo:application name="dubbo-provider" id="dubbo-provider" />
                application = provider.getApplication();
            }
            if (module == null) {
                module = provider.getModule();
            }
//            <dubbo:registry address="zookeeper://192.168.50.251:2181" />
            if (registries == null) {
                registries = provider.getRegistries();
            }
            if (monitor == null) {
                monitor = provider.getMonitor();
            }
//            <dubbo:protocol name="dubbo" port="-1" id="dubbo" />
            if (protocols == null) {
                protocols = provider.getProtocols();
            }
        }
        if (module != null) {
            if (registries == null) {
                registries = module.getRegistries();
            }
            if (monitor == null) {
                monitor = module.getMonitor();
            }
        }
        if (application != null) {
            if (registries == null) {
                registries = application.getRegistries();
            }
            if (monitor == null) {
                monitor = application.getMonitor();
            }
        }
        if (ref instanceof GenericService) {
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)) {
                generic = Boolean.TRUE.toString();
            }
        } else {
            try {
//                加载接口类 com.tianhe.lianxi.dubbo.api.HelloFacade
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
//            检查接口和方法的合法性=》
            checkInterfaceAndMethods(interfaceClass, methods);
//            检查接口的引用是否为null，引用类型是接口的实现类=》
            checkRef();
            generic = Boolean.FALSE.toString();
        }
        if (local != null) {
            if ("true".equals(local)) {
                local = interfaceName + "Local";
            }
            Class<?> localClass;
            try {
                localClass = ClassHelper.forNameWithThreadContextClassLoader(local);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
//            服务实现类和接口类型不一样
            if (!interfaceClass.isAssignableFrom(localClass)) {
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceName);
            }
        }
        if (stub != null) {
            if ("true".equals(stub)) {
                stub = interfaceName + "Stub";
            }
            Class<?> stubClass;
            try {
                stubClass = ClassHelper.forNameWithThreadContextClassLoader(stub);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(stubClass)) {
                throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + interfaceName);
            }
        }
//        检查应用配置=》
        checkApplication();
//        检查注册中心配置=》
        checkRegistry();
//        检查协议配置=》
        checkProtocol();
//        从系统属性中解析其他配置=》
        appendProperties(this);
//        检查接口的mock=》
        checkStubAndMock(interfaceClass);
        if (path == null || path.length() == 0) {
            path = interfaceName;
        }
//        服务注册与发现=》
        doExportUrls();
//        服务名 组名+接口名+版本号
        ProviderModel providerModel = new ProviderModel(getUniqueServiceName(), this, ref);
//        注册服务提供者模式=》
        ApplicationModel.initProviderModel(getUniqueServiceName(), providerModel);
    }

    private void checkRef() {
        // reference should not be null, and is the implementation of the given interface 引用不应该为空，而是给定接口的实现
        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
//        引用的对象不是接口的对象
        if (!interfaceClass.isInstance(ref)) {
            throw new IllegalStateException("The class "
                    + ref.getClass().getName() + " unimplemented interface "
                    + interfaceClass + "!");
        }
    }

    public synchronized void unexport() {
//        没有注册过服务结束
        if (!exported) {
            return;
        }
//        没有注册服务结束
        if (unexported) {
            return;
        }
        if (!exporters.isEmpty()) {
            for (Exporter<?> exporter : exporters) {
                try {
//                    =》
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn("unexpected err when unexport" + exporter, t);
                }
            }
            exporters.clear();
        }
        unexported = true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doExportUrls() {
//        解析注册中心地址=》
        List<URL> registryURLs = loadRegistries(true);
        for (ProtocolConfig protocolConfig : protocols) {
//            从协议配置中加载解析注册中心地址=》
            doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }
    }

    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        String name = protocolConfig.getName();
        if (name == null || name.length() == 0) {
//            默认dubbo协议
            name = "dubbo";
        }

        Map<String, String> map = new HashMap<String, String>();
//        side是provider
        map.put(Constants.SIDE_KEY, Constants.PROVIDER_SIDE);//"side" -> "provider"
        map.put(Constants.DUBBO_VERSION_KEY, Version.getProtocolVersion());//"dubbo" -> "2.0.2"
//        时间属性
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));//"pid" -> "2528"
        }
//        从这里配置解析可以看到下面的优先级最高，后面的配置会把前面的配置覆盖
//        application配置追加到map=》
        appendParameters(map, application);//"application" -> "dubbo-provider"
//        module配置追加到map
        appendParameters(map, module);
//        provider配置追加到map
        appendParameters(map, provider, Constants.DEFAULT_KEY);
//        协议配置追加到map
        appendParameters(map, protocolConfig);
//        默认配置追加到map
        appendParameters(map, this);
//        "executes" -> "200"
// "interface" -> "com.tianhe.lianxi.dubbo.api.HelloFacade"
// "version" -> "1.0.0"
//        "bean.name" -> "providers:dubbo:com.tianhe.lianxi.dubbo.api.HelloFacade:1.0.0:helloGroup"
//        "group" -> "helloGroup"
        if (methods != null && !methods.isEmpty()) {
            for (MethodConfig method : methods) {
//                追加method的配置到map
                appendParameters(map, method, method.getName());
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                List<ArgumentConfig> arguments = method.getArguments();
                if (arguments != null && !arguments.isEmpty()) {
                    for (ArgumentConfig argument : arguments) {
                        // convert argument type
                        if (argument.getType() != null && argument.getType().length() > 0) {
//                            查询接口的方法
                            Method[] methods = interfaceClass.getMethods();
                            // visit all methods
                            if (methods != null && methods.length > 0) {
                                for (int i = 0; i < methods.length; i++) {
                                    String methodName = methods[i].getName();
                                    // target the method, and get its signature
                                    if (methodName.equals(method.getName())) {
                                        Class<?>[] argtypes = methods[i].getParameterTypes();
                                        // one callback in the method 一个回调方法
                                        if (argument.getIndex() != -1) {
                                            if (argtypes[argument.getIndex()].getName().equals(argument.getType())) {
//                                                方法参数追加到map
                                                appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                                            } else {
                                                throw new IllegalArgumentException("argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                            }
                                        } else {
                                            // multiple callbacks in the method 多个回调方法
                                            for (int j = 0; j < argtypes.length; j++) {
                                                Class<?> argclazz = argtypes[j];
                                                if (argclazz.getName().equals(argument.getType())) {
                                                    appendParameters(map, argument, method.getName() + "." + j);
                                                    if (argument.getIndex() != -1 && argument.getIndex() != j) {
                                                        throw new IllegalArgumentException("argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (argument.getIndex() != -1) {
                            appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                        } else {
                            throw new IllegalArgumentException("argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                        }

                    }
                }
            } // end of methods for
        }

        if (ProtocolUtils.isGeneric(generic)) {
            map.put(Constants.GENERIC_KEY, generic);
            map.put(Constants.METHODS_KEY, Constants.ANY_VALUE);
        } else {
//            查询接口的版本号
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put("revision", revision);
            }

//            查询接口的所有方法 sayHello
            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0) {
                logger.warn("NO method found in service interface " + interfaceClass.getName());
                map.put(Constants.METHODS_KEY, Constants.ANY_VALUE);
            } else {
                map.put(Constants.METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));//"methods" -> "sayHello"
            }
        }
//        解析token属性
        if (!ConfigUtils.isEmpty(token)) {
            if (ConfigUtils.isDefault(token)) {
                map.put(Constants.TOKEN_KEY, UUID.randomUUID().toString());
            } else {
                map.put(Constants.TOKEN_KEY, token);
            }
        }
//        如果是本地协议不进行服务注册injvm，本地测试可用这种协议
        if (Constants.LOCAL_PROTOCOL.equals(protocolConfig.getName())) {
            protocolConfig.setRegister(false);
            map.put("notify", "false");
        }
        // export service
        String contextPath = protocolConfig.getContextpath();
        if ((contextPath == null || contextPath.length() == 0) && provider != null) {
            contextPath = provider.getContextpath();
        }

//        查询配置的host=》172.28.82.218
        String host = this.findConfigedHosts(protocolConfig, registryURLs, map);
//        查询配置的端口=》
        Integer port = this.findConfigedPorts(protocolConfig, name, map);
        URL url = new URL(name, host, port, (contextPath == null || contextPath.length() == 0 ? "" : contextPath + "/") + path, map);

        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .hasExtension(url.getProtocol())) {
            url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }

//        解析scope属性
        String scope = url.getParameter(Constants.SCOPE_KEY);
        // don't export when none is configured scope值是none的时候不进行服务注册
        if (!Constants.SCOPE_NONE.toString().equalsIgnoreCase(scope)) {

            // export to local if the config is not remote (export to remote only when config is remote) 如果配置不是远程的，导出到本地(只有配置是远程的才导出到远程)
            if (!Constants.SCOPE_REMOTE.toString().equalsIgnoreCase(scope)) {
//                =》dubbo://172.28.82.218:20880/com.tianhe.lianxi.dubbo.api.HelloFacade?anyhost=true&application=dubbo-provider&bean.name=providers:dubbo:com.tianhe.lianxi.dubbo.api.HelloFacade:1.0.0:helloGroup&bind.ip=172.28.82.218&bind.port=20880&dubbo=2.0.2&executes=200&generic=false&group=helloGroup&interface=com.tianhe.lianxi.dubbo.api.HelloFacade&methods=sayHello&pid=2528&revision=1.0.0&side=provider&timestamp=1573207327686&version=1.0.0
                exportLocal(url);
            }
            // export to remote if the config is not local (export to local only when config is local) 如果配置不是本地的，则导出到远程(仅当配置是本地的时才导出到本地)
            if (!Constants.SCOPE_LOCAL.toString().equalsIgnoreCase(scope)) {
                if (logger.isInfoEnabled()) {
                    logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                }
//                registry://192.168.50.251:2181/com.alibaba.dubbo.registry.RegistryService?application=dubbo-provider&dubbo=2.0.2&pid=70847&registry=zookeeper&timestamp=1573208445243
                if (registryURLs != null && !registryURLs.isEmpty()) {
                    for (URL registryURL : registryURLs) {
//                        获取dynamic参数值
                        url = url.addParameterIfAbsent(Constants.DYNAMIC_KEY, registryURL.getParameter(Constants.DYNAMIC_KEY));
//                        加载监视器的地址=》
                        URL monitorUrl = loadMonitor(registryURL);
                        if (monitorUrl != null) {
//                            添加monitor属性到url
                            url = url.addParameterAndEncoded(Constants.MONITOR_KEY, monitorUrl.toFullString());
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                        }

                        // For providers, this is used to enable custom proxy to generate invoker 对于提供程序，这用于启用自定义代理来生成调用程序，解析proxy属性值
                        String proxy = url.getParameter(Constants.PROXY_KEY);
                        if (StringUtils.isNotEmpty(proxy)) {
//                            添加proxy属性值到url
                            registryURL = registryURL.addParameter(Constants.PROXY_KEY, proxy);
                        }

//                        javassistProxyFactory
//                        interface com.tianhe.lianxi.dubbo.api.HelloFacade -> registry://192.168.50.251:2181/com.alibaba.dubbo.registry.RegistryService?application=dubbo-provider&dubbo=2.0.2&export=dubbo%3A%2F%2F172.28.82.218%3A20880%2Fcom.tianhe.lianxi.dubbo.api.HelloFacade%3Fanyhost%3Dtrue%26application%3Ddubbo-provider%26bean.name%3Dproviders%3Adubbo%3Acom.tianhe.lianxi.dubbo.api.HelloFacade%3A1.0.0%3AhelloGroup%26bind.ip%3D172.28.82.218%26bind.port%3D20880%26dubbo%3D2.0.2%26executes%3D200%26generic%3Dfalse%26group%3DhelloGroup%26interface%3Dcom.tianhe.lianxi.dubbo.api.HelloFacade%26methods%3DsayHello%26pid%3D91252%26revision%3D1.0.0%26side%3Dprovider%26timestamp%3D1573208996919%26version%3D1.0.0&pid=91252&registry=zookeeper&timestamp=1573208996167
                        Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, url.toFullString()));
                        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

//                      ProtocolFilterWrapper.export服务注册=》 DestroyableExporter
                        Exporter<?> exporter = protocol.export(wrapperInvoker);
                        exporters.add(exporter);
                    }
                } else {
                    Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class) interfaceClass, url);
                    DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                    Exporter<?> exporter = protocol.export(wrapperInvoker);
                    exporters.add(exporter);
                }
            }
        }
        this.urls.add(url);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void exportLocal(URL url) {
//        injvm协议
        if (!Constants.LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
            URL local = URL.valueOf(url.toFullString())
                    .setProtocol(Constants.LOCAL_PROTOCOL)
//                    127.0.0.1 本地host
                    .setHost(LOCALHOST)
                    .setPort(0);
            ServiceClassHolder.getInstance().pushServiceClass(getServiceClass(ref));
//            服务注册 com.alibaba.dubbo.rpc.protocol.ProtocolFilterWrapper.export=》
//            interface com.tianhe.lianxi.dubbo.api.HelloFacade -> injvm://127.0.0.1/com.tianhe.lianxi.dubbo.api.HelloFacade?anyhost=true&application=dubbo-provider&bean.name=providers:dubbo:com.tianhe.lianxi.dubbo.api.HelloFacade:1.0.0:helloGroup&bind.ip=172.28.82.218&bind.port=20880&dubbo=2.0.2&executes=200&generic=false&group=helloGroup&interface=com.tianhe.lianxi.dubbo.api.HelloFacade&methods=sayHello&pid=2528&revision=1.0.0&side=provider&timestamp=1573207327686&version=1.0.0
            Exporter<?> exporter = protocol.export(
                    proxyFactory.getInvoker(ref, (Class) interfaceClass, local));
            exporters.add(exporter);
            logger.info("Export dubbo service " + interfaceClass.getName() + " to local registry");
        }
    }

    protected Class getServiceClass(T ref) {
        return ref.getClass();
    }

    /**
     * Register & bind IP address for service provider, can be configured separately.
     * Configuration priority: environment variables -> java system properties -> host property in config file ->
     * /etc/hosts -> default network address -> first available network address
     * 注册和绑定服务提供商的IP地址，可以单独配置。配置优先级:环境变量-> java系统属性-配置文件中的>主机属性-> /etc/hosts ->默认网络地址->第一个可用网络地址
     *
     * @param protocolConfig
     * @param registryURLs
     * @param map
     * @return
     */
    private String findConfigedHosts(ProtocolConfig protocolConfig, List<URL> registryURLs, Map<String, String> map) {
        boolean anyhost = false;

//        获取指定的ip地址，查询系统变量DUBBO_IP_TO_BIND
        String hostToBind = getValueFromConfig(protocolConfig, Constants.DUBBO_IP_TO_BIND);
//        验证是否是本地服务=》
        if (hostToBind != null && hostToBind.length() > 0 && isInvalidLocalHost(hostToBind)) {
            throw new IllegalArgumentException("Specified invalid bind ip from property:" + Constants.DUBBO_IP_TO_BIND + ", value:" + hostToBind);
        }

        // if bind ip is not found in environment, keep looking up 如果指定的ip地址是无效的，就用本地ip地址
        if (hostToBind == null || hostToBind.length() == 0) {
//            从协议配置中获取host这个属性值指定的ip地址
            hostToBind = protocolConfig.getHost();
            if (provider != null && (hostToBind == null || hostToBind.length() == 0)) {
//                从服务提供者获取服务ip
                hostToBind = provider.getHost();
            }
//            验证是否是本地ip
            if (isInvalidLocalHost(hostToBind)) {
                anyhost = true;
                try {
                    hostToBind = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    logger.warn(e.getMessage(), e);
                }
//                验证是否是本地ip
                if (isInvalidLocalHost(hostToBind)) {
                    if (registryURLs != null && !registryURLs.isEmpty()) {
                        for (URL registryURL : registryURLs) {
//                            如果registry属性值是multicast
                            if (Constants.MULTICAST.equalsIgnoreCase(registryURL.getParameter("registry"))) {
                                // skip multicast registry since we cannot connect to it via Socket 跳过组播注册表，因为我们无法通过套接字连接到它
                                continue;
                            }
                            try {
                                Socket socket = new Socket();
                                try {
//                                    从url中获取ip地址
                                    SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                                    socket.connect(addr, 1000);
                                    hostToBind = socket.getLocalAddress().getHostAddress();
                                    break;
                                } finally {
                                    try {
                                        socket.close();
                                    } catch (Throwable e) {
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn(e.getMessage(), e);
                            }
                        }
                    }
//                    如果是本地ip获取本地ip
                    if (isInvalidLocalHost(hostToBind)) {
                        hostToBind = getLocalHost();
                    }
                }
            }
        }

        map.put(Constants.BIND_IP_KEY, hostToBind);

        // registry ip is not used for bind ip by default 注册表ip默认情况下不用于绑定ip，从系统变量查询DUBBO_IP_TO_REGISTRY值
        String hostToRegistry = getValueFromConfig(protocolConfig, Constants.DUBBO_IP_TO_REGISTRY);
        if (hostToRegistry != null && hostToRegistry.length() > 0 && isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + Constants.DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        } else if (hostToRegistry == null || hostToRegistry.length() == 0) {
            // bind ip is used as registry ip by default
            hostToRegistry = hostToBind;
        }

        map.put(Constants.ANYHOST_KEY, String.valueOf(anyhost));

        return hostToRegistry;
    }

    /**
     * Register port and bind port for the provider, can be configured separately
     * Configuration priority: environment variable -> java system properties -> port property in protocol config file
     * -> protocol default port
     * 注册端口和绑定端口对于提供者来说，可以分别配置优先级:环境变量-> java系统属性->端口属性在协议配置文件->协议默认端口
     *
     * @param protocolConfig
     * @param name
     * @return
     */
    private Integer findConfigedPorts(ProtocolConfig protocolConfig, String name, Map<String, String> map) {
        Integer portToBind = null;

        // parse bind port from environment 获取指定的端口号，查询系统变量DUBBO_PORT_TO_BIND值
        String port = getValueFromConfig(protocolConfig, Constants.DUBBO_PORT_TO_BIND);
//        端口处理=》
        portToBind = parsePort(port);

        // if there's no bind port found from environment, keep looking up. 如果指定的端口号不可用
        if (portToBind == null) {
//            从协议配置中获取port这个属性值指定的端口号
            portToBind = protocolConfig.getPort();
            if (provider != null && (portToBind == null || portToBind == 0)) {
//                从服务提供者中查询端口
                portToBind = provider.getPort();
            }
//            dubbo协议的默认端口20880
            final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
            if (portToBind == null || portToBind == 0) {
                portToBind = defaultPort;
            }
//            如果协议指定的port端口号不可用就随机生成一个端口号
            if (portToBind == null || portToBind <= 0) {
                portToBind = getRandomPort(name);
                if (portToBind == null || portToBind < 0) {
//                    验证默认端口20880是否可用
                    portToBind = getAvailablePort(defaultPort);
                    putRandomPort(name, portToBind);
                }
                logger.warn("Use random available port(" + portToBind + ") for protocol " + name);
            }
        }

        // save bind port, used as url's key later
        map.put(Constants.BIND_PORT_KEY, String.valueOf(portToBind));

        // registry port, not used as bind port by default 查询系统变量DUBBO_PORT_TO_REGISTRY值
        String portToRegistryStr = getValueFromConfig(protocolConfig, Constants.DUBBO_PORT_TO_REGISTRY);
//        端口处理=》
        Integer portToRegistry = parsePort(portToRegistryStr);
        if (portToRegistry == null) {
            portToRegistry = portToBind;
        }

        return portToRegistry;
    }

    private Integer parsePort(String configPort) {
        Integer port = null;
        if (configPort != null && configPort.length() > 0) {
            try {
                Integer intPort = Integer.parseInt(configPort);
//                端口号区间不能<=0或者>65535
                if (isInvalidPort(intPort)) {
                    throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
                }
                port = intPort;
            } catch (Exception e) {
                throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
            }
        }
        return port;
    }

    private String getValueFromConfig(ProtocolConfig protocolConfig, String key) {
        String protocolPrefix = protocolConfig.getName().toUpperCase() + "_";
        String port = ConfigUtils.getSystemProperty(protocolPrefix + key);
        if (port == null || port.length() == 0) {
            port = ConfigUtils.getSystemProperty(key);
        }
        return port;
    }

    private void checkDefault() {
//        如果provider配置为空，创建默认配置
        if (provider == null) {
            provider = new ProviderConfig();
        }
//        解析配置=》
        appendProperties(provider);
    }

    private void checkProtocol() {
//        如果没有协议配置就从provider中获取协议配置
        if ((protocols == null || protocols.isEmpty())
                && provider != null) {
//            从provider中获取协议配置
            setProtocols(provider.getProtocols());
        }
        // backward compatibility 如果provider协议配置也没有就创建默认协议配置
        if (protocols == null || protocols.isEmpty()) {
            setProtocol(new ProtocolConfig());
        }
//        <dubbo:protocol name="dubbo" port="-1" id="dubbo" />
        for (ProtocolConfig protocolConfig : protocols) {
            if (StringUtils.isEmpty(protocolConfig.getName())) {
//                如果协议名称为空，默认dubbo协议
                protocolConfig.setName(Constants.DUBBO_VERSION_KEY);
            }
//            从系统属性中解析协议配置=》
            appendProperties(protocolConfig);
        }
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (ref instanceof GenericService) {
            return GenericService.class;
        }
        try {
            if (interfaceName != null && interfaceName.length() > 0) {
                this.interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            }
        } catch (ClassNotFoundException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }
        return interfaceClass;
    }

    /**
     * @param interfaceClass
     * @see #setInterface(Class)
     * @deprecated
     */
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (id == null || id.length() == 0) {
            id = interfaceName;
        }
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    @Parameter(excluded = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        checkPathName(Constants.PATH_KEY, path);
        this.path = path;
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    // ======== Deprecated ========

    @SuppressWarnings("unchecked")
    public void setMethods(List<? extends MethodConfig> methods) {
        this.methods = (List<MethodConfig>) methods;
    }

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }

    public String getGeneric() {
        return generic;
    }

    public void setGeneric(String generic) {
        if (StringUtils.isEmpty(generic)) {
            return;
        }
        if (ProtocolUtils.isGeneric(generic)) {
            this.generic = generic;
        } else {
            throw new IllegalArgumentException("Unsupported generic type " + generic);
        }
    }

    public List<URL> getExportedUrls() {
        return urls;
    }

    /**
     * @deprecated Replace to getProtocols()
     */
    @Deprecated
    public List<ProviderConfig> getProviders() {
        return convertProtocolToProvider(protocols);
    }

    /**
     * @deprecated Replace to setProtocols()
     */
    @Deprecated
    public void setProviders(List<ProviderConfig> providers) {
        this.protocols = convertProviderToProtocol(providers);
    }

    @Parameter(excluded = true)
    public String getUniqueServiceName() {
        StringBuilder buf = new StringBuilder();
        if (group != null && group.length() > 0) {
            buf.append(group).append("/");
        }
        buf.append(interfaceName);
        if (version != null && version.length() > 0) {
            buf.append(":").append(version);
        }
        return buf.toString();
    }
}
