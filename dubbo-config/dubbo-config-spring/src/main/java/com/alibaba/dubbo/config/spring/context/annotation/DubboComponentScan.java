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
package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dubbo Component Scan {@link Annotation},scans the classpath for annotated components that will be auto-registered as
 * Spring beans. Dubbo-provided {@link Service} and {@link Reference}.Dubbo组件扫描注释，扫描类路径，寻找将自动注册为Spring bean的带注释组件。杜博提供的服务和参考。
 *
 * @see Service
 * @see Reference
 * @since 2.5.7
 */
//dubbo源码解析之annotation注解形式配置解析
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DubboComponentScanRegistrar.class)
public @interface DubboComponentScan {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
     * declarations e.g.: {@code @DubboComponentScan("org.my.pkg")} instead of
     * {@code @DubboComponentScan(basePackages="org.my.pkg")}.
     *
     * @return the base packages to scan
     * basePackages()属性的别名。允许更简洁的注释声明，例如:@DubboComponentScan(“org.my.pkg”)而不是@DubboComponentScan(basePackages=“org.my.pkg”)。
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated @Service classes. {@link #value()} is an
     * alias for (and mutually exclusive with) this attribute.
     * <p>
     * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
     * package names.
     *
     * @return the base packages to scan
     * 扫描带注释的@Service类的基本包。value()是该属性的别名(且与该属性互斥)。
    使用basePackageClasses()作为基于字符串的包名称的类型安全替代。
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to
     * scan for annotated @Service classes. The package of each class specified will be
     * scanned.
     *
     * @return classes from the base packages to scan
     * basePackages()的类型安全替代方案，用于指定扫描带注释的@Service类的包。将扫描指定的每个类的包。
     */
    Class<?>[] basePackageClasses() default {};

}
