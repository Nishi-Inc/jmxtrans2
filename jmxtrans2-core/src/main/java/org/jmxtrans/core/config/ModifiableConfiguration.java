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
package org.jmxtrans.core.config;

import lombok.Getter;
import lombok.Setter;
import org.jmxtrans.core.output.OutputWriter;
import org.jmxtrans.core.query.Invocation;
import org.jmxtrans.core.query.embedded.Server;
import org.jmxtrans.utils.time.Interval;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Collection;

@NotThreadSafe
final class ModifiableConfiguration implements Configuration {

    @Setter
    private Interval period;
    @Nonnull @Getter
    private final Collection<OutputWriter> outputWriters = new ArrayList<>();
    @Nonnull @Getter
    private final Collection<Invocation> invocations = new ArrayList<>();
    private final Collection<Server> servers = new ArrayList<>();

    @Nonnull
    @Override
    public Collection<Server> getServers() {
        return servers;
    }

    @Nonnull
    @Override
    public Interval getPeriod() {
        if (period == null) {
            return DefaultConfiguration.getInstance().getPeriod();
        }
        return period;
    }

    public void addServer(@Nonnull Server server) {
        servers.add(server);
    }
}
