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
package org.jmxtrans.output.writers;

import org.jmxtrans.core.log.Logger;
import org.jmxtrans.core.log.LoggerFactory;
import org.jmxtrans.core.output.OutputWriter;
import org.jmxtrans.core.output.OutputWriterFactory;
import org.jmxtrans.core.results.QueryResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import static org.jmxtrans.utils.ConfigurationUtils.getBoolean;
import static org.jmxtrans.utils.ConfigurationUtils.getString;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
@NotThreadSafe
public class FileOverwriterOutputWriter implements OutputWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    protected Writer temporaryFileWriter;
    protected File temporaryFile;
    protected final File file;
    protected final Boolean showTimeStamp;
    private final DateFormat dateFormat;

    protected FileOverwriterOutputWriter(
            @Nonnull File file,
            boolean showTimeStamp,
            @Nonnull DateFormat dateFormat) {
        this.dateFormat = dateFormat;
        this.file = file;
        this.showTimeStamp = showTimeStamp;
        logger.info("FileOverwriterOutputWriter configured with file " + file.getAbsolutePath());
    }

    @Nonnull
    protected Writer getTemporaryFileWriter() throws IOException {
        if (temporaryFile == null) {
            temporaryFile = File.createTempFile("jmxtrans-agent-", ".data");
            temporaryFile.deleteOnExit();
            if (logger.isDebugEnabled())
                logger.debug("Created temporary file " + temporaryFile.getAbsolutePath());

            temporaryFileWriter = null;
        }
        if (temporaryFileWriter == null) {
            temporaryFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temporaryFile, false), Charset.forName("UTF-8")));
        }

        return temporaryFileWriter;
    }

    public synchronized void writeQueryResult(@Nonnull String name, @Nullable String type, @Nullable Object value) throws IOException {
        try {
            if (showTimeStamp){
                getTemporaryFileWriter().write("["+ dateFormat.format(Calendar.getInstance().getTime()) +"] "+name + " " + value + "\n");
            } else {
                getTemporaryFileWriter().write(name + " " + value + "\n");
            }
            
        } catch (IOException e) {
            releaseTemporaryWriter();
            throw e;
        }
    }

    @Override
    public void write(@Nonnull QueryResult result) throws IOException {
        String name = result.getName();
        Object value = result.getValue();
        synchronized (this) {
            try {
                if (showTimeStamp){
                    getTemporaryFileWriter().write("["+ dateFormat.format(Calendar.getInstance().getTime()) +"] "+name + " " + value + "\n");
                } else {
                    getTemporaryFileWriter().write(name + " " + value + "\n");
                }

            } catch (IOException e) {
                releaseTemporaryWriter();
                throw e;
            }
        }
        postCollect();
    }

    protected void releaseTemporaryWriter() {
        try (Writer writer = getTemporaryFileWriter()) {
        } catch (IOException e) {
            logger.warn("Could not get temporary file to delete", e);
        }
        if (temporaryFile != null && !temporaryFile.delete()) {
            logger.warn("Could not delete temporary file [" + temporaryFile.getAbsolutePath() + "].");
        }
        temporaryFile = null;

    }

    private synchronized void postCollect() throws IOException {
        try {
            getTemporaryFileWriter().close();
            if (logger.isDebugEnabled())
                logger.debug("Overwrite " + file.getAbsolutePath() + " by " + temporaryFile.getAbsolutePath());
            RollingFileOutputWriter.replaceFile(temporaryFile, file);
        } finally {
            temporaryFileWriter = null;
        }
    }

    public static final class Factory implements OutputWriterFactory<FileOverwriterOutputWriter> {
        public final static String SETTING_FILE_NAME = "fileName";
        public final static String SETTING_FILE_NAME_DEFAULT_VALUE = "jmxtrans-agent.data";
        public final static String SETTING_SHOW_TIMESTAMP = "showTimeStamp";
        public final static Boolean SETTING_SHOW_TIMESTAMP_DEFAULT = false;

        @Override
        @Nonnull
        public FileOverwriterOutputWriter create(@Nonnull Map<String, String> settings) {
            File file = new File(getString(settings, SETTING_FILE_NAME, SETTING_FILE_NAME_DEFAULT_VALUE));
            boolean showTimeStamp = getBoolean(settings, SETTING_SHOW_TIMESTAMP, SETTING_SHOW_TIMESTAMP_DEFAULT);

            // FIXME: not thread safe !
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            return new FileOverwriterOutputWriter(file, showTimeStamp, dateFormat);
        }
    }
}
