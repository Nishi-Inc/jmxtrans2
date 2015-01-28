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

import com.google.common.io.ByteStreams;
import org.jmxtrans.core.log.ConsoleLogProvider;
import org.junit.After;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractAgentTest {
    private static final int PAUSE = 2000;

    private Process process;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();


    public void startDummyApplication() throws IOException,
            InterruptedException {
        List<String> args = new ArrayList<>();
        args.add("java");
        args.add("-javaagent:" + getAgent().getAbsolutePath() + "=" + getConfig().getAbsolutePath());
        args.add("-D" + ConsoleLogProvider.JMXTRANS_LOG_LEVEL_PROP + "=DEBUG");
        args.add("-cp");
        args.add(getBuildDirectory() + "/test-classes/");

        args.add(DummyApp.class.getName());
        ProcessBuilder pBuilder = new ProcessBuilder(args);
        pBuilder.redirectErrorStream(true);

        process = pBuilder.start();
        redirectProcessIO(process);
        Thread.sleep(PAUSE);
    }

    @After
    public void stopDummyApplication() throws InterruptedException {
        if (process != null) {
            process.destroy();
        }
    }

    private void redirectProcessIO(final Process process) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    ByteStreams.copy(process.getInputStream(), out);
                } catch (IOException e) {
                }
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                try {
                    ByteStreams.copy(process.getErrorStream(), err);
                } catch (IOException e) {
                }
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                try {
                    ByteStreams.copy(System.in, process.getOutputStream());
                } catch (IOException e) {
                }
            }
        }).start();
    }

    private File getAgent() throws IOException {
        File agent = new File(getBuildDirectory(), getFinalName() + "-all.jar");
        assertThat(agent).exists();
        assertThat(agent).isFile();
        return agent;
    }

    private File getConfig() throws IOException {
        File config = new File(getBaseDir() + "/src/test/resources/config.xml");
        assertThat(config).exists();
        assertThat(config).isFile();
        return config;
    }

    private String getFinalName() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("project-info.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("project.build.finalName");
        }
    }

    private String getVersion() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("project-info.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("project.version");
        }
    }

    private String getBuildDirectory() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("project-info.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("project.build.directory");
        }
    }
    
    private String getBaseDir() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("project-info.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("project.basedir");
        }
    }

    protected Callable<Boolean> stdOutContains(final String content) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return out.toString("UTF-8").contains(content);
            }
        };
    }
    
    protected ByteArrayOutputStream getOut() {
        return out;
    }

    protected ByteArrayOutputStream getErr() {
        return err;
    }
}
