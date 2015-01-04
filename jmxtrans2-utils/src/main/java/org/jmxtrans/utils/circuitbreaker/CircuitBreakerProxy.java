/**
 * The MIT License
 * Copyright (c) 2014 JMXTrans Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jmxtrans.utils.circuitbreaker;

import org.jmxtrans.utils.time.Clock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@ThreadSafe
public class CircuitBreakerProxy implements InvocationHandler {

    @Nonnull
    private final Logger logger;

    @Nonnull
    public final Object target;
    private final int maxFailures;
    private final long disableDurationMillis;
    @Nonnull
    private final AtomicInteger failuresCounter = new AtomicInteger();
    @Nonnull
    private final Class<?> proxiedInterface;
    private volatile long disabledUntil = 0;
    @Nonnull
    private final Clock clock;

    private CircuitBreakerProxy(
            @Nonnull Clock clock,
            @Nonnull Class<?> proxiedInterface,
            @Nonnull Object target,
            int maxFailures,
            int disableDurationMillis) {
        this.clock = clock;
        this.proxiedInterface = proxiedInterface;
        this.target = target;
        this.maxFailures = maxFailures;
        this.disableDurationMillis = disableDurationMillis;
        logger = Logger.getLogger(target.getClass().getName() + "CircuitBreaker");
    }

    @Override
    public Object invoke(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
        // bypass circuit breaker for methods not directly implemented by the proxied interface
        if (!method.getDeclaringClass().equals(proxiedInterface)) {
            return method.invoke(target, args);
        }
        if (isDisabled()) {
            throw new CircuitBreakerOpenException(target, disabledUntil);
        }
        try {
            Object result = method.invoke(target, args);
            incrementSuccess();
            return result;
        } catch (InvocationTargetException e) {
            incrementFailures();
            throw e.getCause();
        }
    }

    private boolean isDisabled() {
        if (disabledUntil == 0) {
            logger.finer("OutputWriter is not temporarily disabled");
            return false;
        } else if (disabledUntil < clock.currentTimeMillis()) {
            logger.fine("re-enable OutputWriter");
            // reset counter
            disabledUntil = 0;
            return false;
        } else {
            if (logger.isLoggable(Level.FINE))
                logger.fine("OutputWriter is disabled until " + new Timestamp(disabledUntil));
            return true;
        }
    }

    private void incrementFailures() {
        int failuresCount = failuresCounter.incrementAndGet();
        if (failuresCount >= maxFailures) {
            disabledUntil = clock.currentTimeMillis() + disableDurationMillis;
            failuresCounter.set(0);
            logger.warning("Too many exceptions, disable writer until " + new Timestamp(disabledUntil));
        }
    }

    private void incrementSuccess() {
        if (failuresCounter.get() > 0) {
            logger.fine("Reset failures counter to 0");
            failuresCounter.set(0);
        }
    }

    @Nonnull
    public static <T> T create(
            @Nonnull Clock clock,
            @Nonnull Class<T> proxiedInterface,
            @Nonnull T target,
            int maxFailures,
            int disableDurationMillis) {
        return (T) Proxy.newProxyInstance(
                CircuitBreakerProxy.class.getClassLoader(),
                new Class[]{proxiedInterface},
                new CircuitBreakerProxy(clock, proxiedInterface, target, maxFailures, disableDurationMillis));
    }

}
