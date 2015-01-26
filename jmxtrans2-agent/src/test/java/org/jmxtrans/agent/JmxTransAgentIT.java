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
package org.jmxtrans.agent;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxTransAgentIT extends AbstractAgentTest {

    @Test
    public void agentIsStarting() throws IOException, InterruptedException {
        startDummyApplication();

        Thread.sleep(3000);

        System.out.println(getOut().toString("UTF-8"));
        System.err.println(getErr().toString("UTF-8"));

        assertThat(getOut().toString("UTF-8"))
                .contains("counter.Value 0")
                .contains("counter.Value 1");
    }

    @Test
    public void applicationInfoAreDisplayedAtStartup() throws IOException, InterruptedException {
        startDummyApplication();

        Thread.sleep(1000);

        assertThat(getOut().toString("UTF-8"))
                .contains("JMXTrans - agent")
                .contains("version:")
                .contains("last modified:")
                .contains("build time:");
    }
}