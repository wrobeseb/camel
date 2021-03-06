/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.camel.spi.ThreadPoolFactory;
import org.apache.camel.spi.ThreadPoolProfile;
import org.apache.camel.util.concurrent.RejectableScheduledThreadPoolExecutor;
import org.apache.camel.util.concurrent.RejectableThreadPoolExecutor;
import org.apache.camel.util.concurrent.SizedScheduledExecutorService;

/**
 * Factory for thread pools that uses the JDK {@link Executors} for creating the thread pools.
 */
public class DefaultThreadPoolFactory implements ThreadPoolFactory {

    public ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return Executors.newCachedThreadPool(threadFactory);
    }
    
    @Override
    public ExecutorService newThreadPool(ThreadPoolProfile profile, ThreadFactory factory) {
        return newThreadPool(profile.getPoolSize(), 
                             profile.getMaxPoolSize(), 
                             profile.getKeepAliveTime(),
                             profile.getTimeUnit(),
                             profile.getMaxQueueSize(), 
                             profile.getRejectedExecutionHandler(),
                             factory);
    }

    public ExecutorService newThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit,
                                         int maxQueueSize, RejectedExecutionHandler rejectedExecutionHandler,
                                         ThreadFactory threadFactory) throws IllegalArgumentException {

        // If we set the corePoolSize to be 0, the whole camel application will hang in JDK5
        // just add a check here to throw the IllegalArgumentException
        if (corePoolSize < 1) {
            throw new IllegalArgumentException("The corePoolSize can't be lower than 1");
        }

        // validate max >= core
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException("MaxPoolSize must be >= corePoolSize, was " + maxPoolSize + " >= " + corePoolSize);
        }

        BlockingQueue<Runnable> workQueue;
        if (corePoolSize == 0 && maxQueueSize <= 0) {
            // use a synchronous queue
            workQueue = new SynchronousQueue<Runnable>();
            // and force 1 as pool size to be able to create the thread pool by the JDK
            corePoolSize = 1;
            maxPoolSize = 1;
        } else if (maxQueueSize <= 0) {
            // unbounded task queue
            workQueue = new LinkedBlockingQueue<Runnable>();
        } else {
            // bounded task queue
            workQueue = new LinkedBlockingQueue<Runnable>(maxQueueSize);
        }

        ThreadPoolExecutor answer = new RejectableThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, timeUnit, workQueue);
        answer.setThreadFactory(threadFactory);
        if (rejectedExecutionHandler == null) {
            rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        }
        answer.setRejectedExecutionHandler(rejectedExecutionHandler);
        return answer;
    }
    
    @Override
    public ScheduledExecutorService newScheduledThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {
        RejectedExecutionHandler rejectedExecutionHandler = profile.getRejectedExecutionHandler();
        if (rejectedExecutionHandler == null) {
            rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        }

        ScheduledThreadPoolExecutor answer = new RejectableScheduledThreadPoolExecutor(profile.getPoolSize(), threadFactory, rejectedExecutionHandler);
        // TODO: when JDK7 we should setRemoveOnCancelPolicy(true)

        // need to wrap the thread pool in a sized to guard against the problem that the
        // JDK created thread pool has an unbounded queue (see class javadoc), which mean
        // we could potentially keep adding tasks, and run out of memory.
        if (profile.getMaxPoolSize() > 0) {
            return new SizedScheduledExecutorService(answer, profile.getMaxQueueSize());
        } else {
            return answer;
        }
    }

}
