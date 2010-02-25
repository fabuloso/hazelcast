/* 
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hazelcast.impl;

import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.impl.base.OrderedRunnablesQueue;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NamedExecutorService {

    private static final int ORDERED_QUEUE_COUNT = 100;
    final ClassLoader classLoader;
    final ExecutorConfig executorConfig;
    final ThreadPoolExecutor threadPoolExecutor;
    final OrderedRunnablesQueue[] orderedRunnablesQueues = new OrderedRunnablesQueue[ORDERED_QUEUE_COUNT];
    final ExecutionLoadBalancer executionLoadBalancer;

    public NamedExecutorService(ClassLoader classLoader, ExecutorConfig executorConfig, ThreadPoolExecutor threadPoolExecutor) {
        this.classLoader = classLoader;
        this.executorConfig = executorConfig;
        this.threadPoolExecutor = threadPoolExecutor;
        for (int i = 0; i < ORDERED_QUEUE_COUNT; i++) {
            orderedRunnablesQueues[i] = new OrderedRunnablesQueue();
        }
        executionLoadBalancer = new RoundRobinLoadBalancer();
    }

    public void appendState(StringBuffer sbState) {
        sbState.append("\nExecutor." + getName() + ".size=" + threadPoolExecutor.getQueue().size());
    }

    public String getName() {
        return executorConfig.getName();
    }

    public void stop() {
        try {
            threadPoolExecutor.shutdown();
            threadPoolExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public void execute(Runnable runnable) {
        threadPoolExecutor.execute(runnable);
    }

    public void executeOrderedRunnable(int hash, Runnable runnable) {
        int index = Math.abs(hash % ORDERED_QUEUE_COUNT);
        final OrderedRunnablesQueue eventQueue = orderedRunnablesQueues[index];
        final int size = eventQueue.offerRunnable(runnable);
        if (size == 1) execute(eventQueue);
    }

    public ExecutionLoadBalancer getExecutionLoadBalancer() {
        return executionLoadBalancer;
    }
}