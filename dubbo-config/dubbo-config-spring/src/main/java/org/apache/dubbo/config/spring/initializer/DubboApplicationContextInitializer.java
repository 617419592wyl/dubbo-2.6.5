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
package org.apache.dubbo.config.spring.initializer;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Automatically register {@link DubboApplicationListener} to Spring context
 * A {@link org.springframework.web.context.ContextLoaderListener} class is defined in
 * src/main/resources/META-INF/web-fragment.xml
 * In the web-fragment.xml, {@link DubboApplicationContextInitializer} is defined in context params.
 * This file will be discovered if running under a servlet 3.0+ container.
 * Even if user specifies {@link org.springframework.web.context.ContextLoaderListener} in web.xml,
 * it will be merged to web.xml.
 * If user specifies <metadata-complete="true" /> in web.xml, this will no take effect,
 * unless user configures {@link DubboApplicationContextInitializer} explicitly in web.xml.
 * 自动注册DubboApplicationListener到Spring上下文一个org.springframe .web.context。ContextLoaderListener类在src/main/resources/META-INF/web-fragment中定义。
 * web片段中的xml。DubboApplicationContextInitializer是在上下文参数中定义的。如果在servlet 3.0+容器下运行，将发现此文件。即使用户指定了org.springframe .web.context。
 * ContextLoaderListener web。它将被合并到web.xml。如果用户在web中指定。除非用户在web.xml中显式配置DubboApplicationContextInitializer，否则这不会生效。
 */
public class DubboApplicationContextInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
//        dubbo源码解析之服务注册
        applicationContext.addApplicationListener(new DubboApplicationListener());
    }
}
