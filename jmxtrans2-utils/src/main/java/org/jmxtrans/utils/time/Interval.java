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
package org.jmxtrans.utils.time;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

@Getter
@Immutable
@ThreadSafe
public class Interval {

    private final int value;
    @Nonnull
    private final TimeUnit timeUnit;

    public Interval(int value, @Nonnull TimeUnit timeUnit) {
        this.value = value;
        this.timeUnit = requireNonNull(timeUnit, "timeUnit cannot be null");
    }

    public long getDuration(TimeUnit timeUnit) {
        return timeUnit.convert(value, this.timeUnit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Interval interval = (Interval) o;

        return Objects.equals(value, interval.value)
                && Objects.equals(timeUnit, interval.timeUnit);
    }

    @Override
    public int hashCode() {
        return hash(value, timeUnit);
    }

    @Override
    @Nonnull
    public String toString() {
        return "Interval{" +
                "value=" + value +
                ", timeUnit=" + timeUnit +
                "}";
    }
}
