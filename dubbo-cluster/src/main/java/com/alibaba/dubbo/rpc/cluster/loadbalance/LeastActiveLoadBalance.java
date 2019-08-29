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
package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.Random;

/**
 * LeastActiveLoadBalance
 *
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "leastactive";

    private final Random random = new Random();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size(); // Number of invokers
        int leastActive = -1; // The least active value of all invokers 所有调用者中最不活跃的值
        int leastCount = 0; // The number of invokers having the same least active value (leastActive) 具有相同最小活动值(最小活动)的调用程序的数量
        int[] leastIndexs = new int[length]; // The index of invokers having the same least active value (leastActive) 具有相同最小活动值(最小活动)的调用程序索引
        int totalWeight = 0; // The sum of with warmup weights
        int firstWeight = 0; // Initial value, used for comparision
        boolean sameWeight = true; // Every invoker has the same weight value?
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
//            获取方法正在执行的个数
            int active = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getActive(); // Active number
//            获取权重
            int afterWarmup = getWeight(invoker, invocation); // Weight
            if (leastActive == -1 || active < leastActive) { // Restart, when find a invoker having smaller least active value. 重新启动，当发现一个最小活动值的调用程序时。
                leastActive = active; // Record the current least active value 记录当前最不活跃的值
                leastCount = 1; // Reset leastCount, count again based on current leastCount 重置最小计数，根据当前最小计数再次计数
                leastIndexs[0] = i; // Reset
                totalWeight = afterWarmup; // Reset
                firstWeight = afterWarmup; // Record the weight the first invoker 记录第一个调用者的权重
                sameWeight = true; // Reset, every invoker has the same weight value? 重置，每个调用程序都有相同的权值?
            } else if (active == leastActive) { // If current invoker's active value equals with leaseActive, then accumulating. 如果当前调用者的活动值等于leaseActive，则累加。
                leastIndexs[leastCount++] = i; // Record index number of this invoker 记录此调用程序的索引号
                totalWeight += afterWarmup; // Add this invoker's weight to totalWeight. 将此调用程序的权值添加到总权值。
                // If every invoker has the same weight?
                if (sameWeight && i > 0
                        && afterWarmup != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        // assert(leastCount > 0)
        if (leastCount == 1) {
            // If we got exactly one invoker having the least active value, return this invoker directly.如果我们恰好得到一个具有最小活动值的调用程序，则直接返回该调用程序。
            return invokers.get(leastIndexs[0]);
        }
        if (!sameWeight && totalWeight > 0) {
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.如果(并非每个调用程序的权值都相同&至少有一个调用程序的权值>0)，则根据总权值随机选择。
            int offsetWeight = random.nextInt(totalWeight) + 1;
            // Return a invoker based on the random value.根据随机值返回调用程序。
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexs[i];
                offsetWeight -= getWeight(invokers.get(leastIndex), invocation);
                if (offsetWeight <= 0)
                    return invokers.get(leastIndex);
            }
        }
        // If all invokers have the same weight value or totalWeight=0, return evenly.如果所有调用者的权值相同或totalWeight=0，则均匀返回。
        return invokers.get(leastIndexs[random.nextInt(leastCount)]);
    }
}
