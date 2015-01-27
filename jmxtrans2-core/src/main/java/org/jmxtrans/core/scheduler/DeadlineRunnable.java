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
package org.jmxtrans.core.scheduler;

import lombok.Getter;
import org.jmxtrans.core.log.Logger;
import org.jmxtrans.core.log.LoggerFactory;
import org.jmxtrans.utils.time.Clock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@Getter
@ThreadSafe
public abstract class DeadlineRunnable implements Runnable {

    @Nonnull private final Logger logger = LoggerFactory.getLogger(getClass().getName());
    @Nonnull private final Clock clock;
    private final long deadline;

    public DeadlineRunnable(@Nonnull Clock clock, long deadline) {
        this.clock = clock;
        this.deadline = deadline;
    }

    @Override
    public final void run() {
        if (deadline < clock.currentTimeMillis()) {
            // TODO: log and count
            logger.warn("Deadline is passed, dropping job");
            return;
        }
        doRun();
    }

    protected abstract void doRun();

}
