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
package org.jmxtrans.output.writers.utils;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class EvictingQueue<E> extends ForwardingQueue<E> implements Queue<E> {

    @Getter
    private LinkedBlockingQueue delegate;
    private int maxRetry = 10;

    public EvictingQueue(int capacity) {
        delegate = new LinkedBlockingQueue(capacity);
    }

    public static <E> EvictingQueue<E> create(int maxCapacity) {
        return new EvictingQueue<E>(maxCapacity);
    }

    @Nonnull
    @Override
    protected Queue<E> delegate() {
        return delegate;
    }

    @Override
    public boolean add(E e) {
        return addEvictingIfNeeded(e);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        return standardAddAll(c);
    }

    @Override
    public boolean offer(E e) {
        return addEvictingIfNeeded(e);
    }

    protected boolean addEvictingIfNeeded(E e) {
        for (int i = 0; i < maxRetry; i++) {
            boolean offered = delegate().offer(e);
            if (offered) {
                return true;
            }
            delegate().poll();
        }

        return false;
    }
}
