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
package com.alibaba.dubbo.common.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

/**
 * A {@link Future} that accepts completion listeners.  Each listener has an
 * associated executor, and it is invoked using this executor once the future's
 * computation is {@linkplain Future#isDone() complete}.  If the computation has
 * already completed when the listener is added, the listener will execute
 * immediately.
 * <p>
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ListenableFutureExplained">
 * {@code ListenableFuture}</a>.
 * <p>
 * <h3>Purpose</h3>
 * <p>
 * <p>Most commonly, {@code ListenableFuture} is used as an input to another
 * derived {@code Future}, as in {@link Futures#allAsList(Iterable)
 * Futures.allAsList}. Many such methods are impossible to implement efficiently
 * without listener support.
 * <p>
 * <p>It is possible to call {@link #addListener addListener} directly, but this
 * is uncommon because the {@code Runnable} interface does not provide direct
 * access to the {@code Future} result. (Users who want such access may prefer
 * {@link Futures#addCallback Futures.addCallback}.) Still, direct {@code
 * addListener} calls are occasionally useful:<pre>   {@code
 *   final String name = ...;
 *   inFlight.add(name);
 *   ListenableFuture<Result> future = service.query(name);
 *   future.addListener(new Runnable() {
 *     public void run() {
 *       processedCount.incrementAndGet();
 *       inFlight.remove(name);
 *       lastProcessed.set(name);
 *       logger.info("Done with {0}", name);
 *     }
 *   }, executor);}</pre>
 * <p>
 * <h3>How to get an instance</h3>
 * <p>
 * <p>Developers are encouraged to return {@code ListenableFuture} from their
 * methods so that users can take advantages of the utilities built atop the
 * class. The way that they will create {@code ListenableFuture} instances
 * depends on how they currently create {@code Future} instances:
 * <ul>
 * <li>If they are returned from an {@code ExecutorService}, convert that
 * service to a {@link ListeningExecutorService}, usually by calling {@link
 * MoreExecutors#listeningDecorator(java.util.concurrent.ExecutorService)
 * MoreExecutors.listeningDecorator}. (Custom executors may find it more
 * convenient to use {@link ListenableFutureTask} directly.)
 * <li>If they are manually filled in by a call to {@link FutureTask#set} or a
 * similar method, create a {@link SettableFuture} instead. (Users with more
 * complex needs may prefer {@link AbstractFuture}.)
 * </ul>
 * <p>
 * <p>Occasionally, an API will return a plain {@code Future} and it will be
 * impossible to change the return type. For this case, we provide a more
 * expensive workaround in {@code JdkFutureAdapters}. However, when possible, it
 * is more efficient and reliable to create a {@code ListenableFuture} directly.
 * 接受完成侦听器的未来。每个侦听器都有一个关联的执行器，并且在未来的计算完成后使用该执行器调用它。如果在添加侦听器时计算已经完成，则侦听器将立即执行。
 * 最常见的是，ListenableFuture用作另一个派生未来的输入，如在Future . allaslist中。如果没有侦听器支持，许多这样的方法不可能有效地实现。
 * 我们鼓励开发人员从他们的方法中返回ListenableFuture，以便用户能够利用构建在类之上的实用程序。他们创建ListenableFuture实例的方式取决于他们当前如何创建未来实例:
 如果它们从ExecutorService返回，则将该服务转换为ListeningExecutorService，通常通过调用more executor . listeningdecorator。(自定义执行器可能会发现直接使用ListenableFutureTask更方便。)
 如果它们是通过调用FutureTask手工填写的。set或类似的方法，创建一个SettableFuture。(有更复杂需求的用户可能更喜欢AbstractFuture。)
 有时，API会返回一个普通的未来，并且不可能更改返回类型。对于这种情况，我们在JdkFutureAdapters中提供了一个更昂贵的解决方案。然而，在可能的情况下，直接创建ListenableFuture更有效和可靠。
 */
public interface ListenableFuture<V> extends Future<V> {
    /**
     * Registers a listener to be {@linkplain Executor#execute(Runnable) run} on
     * the given executor.  The listener will run when the {@code Future}'s
     * computation is {@linkplain Future#isDone() complete} or, if the computation
     * is already complete, immediately.
     * <p>
     * <p>There is no guaranteed ordering of execution of listeners, but any
     * listener added through this method is guaranteed to be called once the
     * computation is complete.
     * <p>
     * <p>Exceptions thrown by a listener will be propagated up to the executor.
     * Any exception thrown during {@code Executor.execute} (e.g., a {@code
     * RejectedExecutionException} or an exception thrown by {@linkplain
     * MoreExecutors#sameThreadExecutor inline execution}) will be caught and
     * logged.
     * <p>
     * <p>Note: For fast, lightweight listeners that would be safe to execute in
     * any thread, consider {@link MoreExecutors#sameThreadExecutor}. For heavier
     * listeners, {@code sameThreadExecutor()} carries some caveats.  For
     * example, the listener may run on an unpredictable or undesirable thread:
     * <p>
     * <ul>
     * <li>If this {@code Future} is done at the time {@code addListener} is
     * called, {@code addListener} will execute the listener inline.
     * <li>If this {@code Future} is not yet done, {@code addListener} will
     * schedule the listener to be run by the thread that completes this {@code
     * Future}, which may be an internal system thread such as an RPC network
     * thread.
     * </ul>
     * <p>
     * <p>Also note that, regardless of which thread executes the
     * {@code sameThreadExecutor()} listener, all other registered but unexecuted
     * listeners are prevented from running during its execution, even if those
     * listeners are to run in other executors.
     * <p>
     * <p>This is the most general listener interface. For common operations
     * performed using listeners, see {@link
     * com.google.common.util.concurrent.Futures}. For a simplified but general
     * listener interface, see {@link
     * com.google.common.util.concurrent.Futures#addCallback addCallback()}.
     *
     * @param listener the listener to run when the computation is complete
     * @param executor the executor to run the listener in
     * @throws NullPointerException       if the executor or listener was null
     * @throws RejectedExecutionException if we tried to execute the listener
     *                                    immediately but the executor rejected it.
     *                                    注册要在给定的执行程序上运行的侦听器。侦听器将在未来的计算完成时运行，或者，如果计算已经完成，则立即运行。

    没有保证侦听器的执行顺序，但是通过该方法添加的任何侦听器都保证在计算完成后被调用。

    侦听器抛出的异常将传播到执行程序。执行程序期间引发的任何异常。执行(例如，RejectedExecutionException或内联执行引发的异常)将被捕获并记录下来。
    注意:对于在任何线程中执行都是安全的快速、轻量级侦听器，请考虑more executor . samethreadexecutor。对于较重的侦听器，sameThreadExecutor()有一些警告。例如，侦听器可能运行在不可预知或不希望的线程上:

    如果在调用addListener时完成此操作，则addListener将内联执行侦听器。
    如果这个未来还没有完成，addListener将计划由完成这个未来的线程(可能是内部系统线程，比如RPC网络线程)运行侦听器。

    还要注意，无论哪个线程执行sameThreadExecutor()侦听器，在执行期间都将阻止所有其他已注册但未执行的侦听器运行，即使这些侦听器将在其他执行器中运行。

    这是最通用的侦听器接口。有关使用侦听器执行的常见操作，请参见com.google.common.util.concurrent.Futures。有关简化但通用的侦听器接口，请参见addCallback()。
     */
    void addListener(Runnable listener, Executor executor);

    void addListener(Runnable listener);
}
