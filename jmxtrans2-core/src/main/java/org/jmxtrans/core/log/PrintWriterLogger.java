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
package org.jmxtrans.core.log;

import lombok.Getter;
import org.jmxtrans.utils.time.Clock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.jmxtrans.core.log.Level.*;

public class PrintWriterLogger implements Logger {

    @Nonnull @Getter
    private final String name;
    @Nonnull private final Level level;
    @Nonnull private final Clock clock;
    @Nonnull private final PrintStream out;

    private final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
        }
    };

    public PrintWriterLogger(@Nonnull String name, @Nonnull Level level, @Nonnull Clock clock, @Nonnull PrintStream out) {
        this.level = level;
        this.out = out;
        this.name = name;
        this.clock = clock;
    }

    @Override
    public boolean isDebugEnabled() {
        return level.isEnabled(DEBUG);
    }

    @Override
    public void debug(@Nullable String msg) {
        if (isDebugEnabled()) {
            write(DEBUG, msg);
        }
    }

    @Override
    public void debug(@Nullable String msg, @Nullable Throwable throwable) {
        if (isDebugEnabled()) {
            write(DEBUG, msg, throwable);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return level.isEnabled(INFO);
    }

    @Override
    public void info(@Nullable String msg) {
        if (isInfoEnabled()) {
            write(INFO, msg);
        }
    }

    @Override
    public void info(@Nullable String msg, @Nullable Throwable throwable) {
        if (isInfoEnabled()) {
            write(INFO, msg, throwable);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return level.isEnabled(WARN);
    }

    @Override
    public void warn(@Nullable String msg) {
        if (isWarnEnabled()) {
            write(WARN, msg);
        }
    }

    @Override
    public void warn(@Nullable String msg, @Nullable Throwable throwable) {
        if (isWarnEnabled()) {
            write(WARN, msg, throwable);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return level.isEnabled(ERROR);
    }

    @Override
    public void error(@Nullable String msg) {
        if (isErrorEnabled()) {
            write(ERROR, msg);
        }
    }

    @Override
    public void error(@Nullable String msg, @Nullable Throwable throwable) {
        if (isErrorEnabled()) {
            write(ERROR, msg, throwable);
        }
    }

    private void write(Level logLevel, @Nullable String msg) {
        out.print(dateFormat.get().format(new Date(clock.currentTimeMillis())));
        out.print(" - ");
        out.print(logLevel);
        out.print(" - ");
        out.print(name);
        out.print(" - ");
        out.println(msg);
        out.flush();
    }

    private void write(Level logLevel, @Nullable String msg, @Nullable Throwable throwable) {
        write(logLevel, msg);
        if (throwable != null) throwable.printStackTrace(out);
        out.flush();
    }
}
