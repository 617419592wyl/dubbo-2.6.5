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
package com.alibaba.dubbo.common.extension;

import com.alibaba.dubbo.common.URL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide helpful information for {@link ExtensionLoader} to inject dependency extension instance.为ExtensionLoader注入依赖扩展实例提供有用的信息。
 *
 * @see ExtensionLoader
 * @see URL
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Adaptive {
    /**
     * Decide which target extension to be injected. The name of the target extension is decided by the parameter passed
     * in the URL, and the parameter names are given by this method.
     * <p>
     * If the specified parameters are not found from {@link URL}, then the default extension will be used for
     * dependency injection (specified in its interface's {@link SPI}).
     * <p>
     * For examples, given <code>String[] {"key1", "key2"}</code>:
     * <ol>
     * <li>find parameter 'key1' in URL, use its value as the extension's name</li>
     * <li>try 'key2' for extension's name if 'key1' is not found (or its value is empty) in URL</li>
     * <li>use default extension if 'key2' doesn't appear either</li>
     * <li>otherwise, throw {@link IllegalStateException}</li>
     * </ol>
     * If default extension's name is not give on interface's {@link SPI}, then a name is generated from interface's
     * class name with the rule: divide classname from capital char into several parts, and separate the parts with
     * dot '.', for example: for {@code com.alibaba.dubbo.xxx.YyyInvokerWrapper}, its default name is
     * <code>String[] {"yyy.invoker.wrapper"}</code>. This name will be used to search for parameter from URL.
     *
     * @return parameter key names in URL
     * 决定注入哪个目标扩展。目标扩展名由URL中传递的参数决定，参数名由此方法给出。
    如果从URL中没有找到指定的参数，那么将使用默认扩展名进行依赖项注入(在其接口的SPI中指定)。
    例如，给定字符串[]{"key1"， "key2"}:
    在URL中查找参数'key1'，使用其值作为扩展名
    如果URL中没有找到“key1”(或其值为空)，则尝试使用“key2”作为扩展名
    如果“key2”也不出现，则使用默认扩展名
    否则,抛出IllegalStateException
    如果接口SPI上没有给出默认的扩展名，那么使用规则从接口的类名生成一个名称:将classname从大写char分隔成几个部分，然后用点分隔这些部分。例如:com. alibaba.com .dubbo.xxx。yyinvokerwrapper，它的默认名字是String[] {"yyy.invoker.wrapper"}。此名称将用于从URL搜索参数。
     */
    String[] value() default {};

}