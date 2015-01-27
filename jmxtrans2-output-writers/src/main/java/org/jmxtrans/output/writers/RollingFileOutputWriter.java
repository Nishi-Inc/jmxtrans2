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
import org.jmxtrans.utils.io.IoUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import static org.jmxtrans.utils.ConfigurationUtils.*;

@NotThreadSafe
public class RollingFileOutputWriter implements OutputWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingFileOutputWriter.class.getName());

    private final DateFormat dateFormat;
    
    protected Writer temporaryFileWriter;
    protected File temporaryFile;
    protected final File file;
    protected final long maxFileSize;
    protected final int maxBackupIndex;

    protected RollingFileOutputWriter(
            @Nonnull File file,
            long maxFileSize,
            int maxBackupIndex,
            @Nonnull DateFormat dateFormat) {
        this.dateFormat = dateFormat;
        this.file = file;
        this.maxFileSize = maxFileSize;
        this.maxBackupIndex = maxBackupIndex;
        LOGGER.info("RollingFileOutputWriter configured with file " + file.getAbsolutePath());
    }

    public static void appendToFile(@Nonnull File source, @Nonnull File destination, long maxFileSize, int maxBackupIndex) throws IOException {
        boolean destinationExists = validateDestinationFile(source, destination, maxFileSize, maxBackupIndex);
        if (destinationExists) {
            IoUtils.doCopySmallFile(source, destination, true);
        } else {
            boolean renamed = source.renameTo(destination);
            if (!renamed) {
                IoUtils.doCopySmallFile(source, destination, false);
            }
        }
    }

    // visible for testing
    static boolean validateDestinationFile(@Nonnull File source, @Nonnull File destination, long maxFileSize, int maxBackupIndex) throws IOException {
        if (!destination.exists() || destination.isDirectory()) return false;
        long totalLengthAfterAppending = destination.length() + source.length();
        if (totalLengthAfterAppending > maxFileSize) {
            rollFiles(destination, maxBackupIndex);
            return false; // File no longer exists because it was move to filename.1
        }

        return true;
    }

    // visible for testing
    static void rollFiles(@Nonnull File destination, int maxBackupIndex) throws IOException {

        // if maxBackup index == 10 then we will have file
        // outputFile, outpuFile.1 outputFile.2 ... outputFile.10
        // we only care if 9 and lower exists to move them up a number
        for (int i = maxBackupIndex - 1; i >= 0; i--) {
            String path = destination.getAbsolutePath();
            path=(i==0)?path:path + "." + i;
            File f = new File(path);
            if (!f.exists()) continue;

            File fNext = new File(destination + "." + (i + 1));
            IoUtils.doCopySmallFile(f, fNext, false);
        }

        if (!destination.delete()) {
            LOGGER.warn("Could not delete file [" + destination.getAbsolutePath() + "].");
        }
    }

    /**
     * Simple implementation without chunking if the source file is big.
     *
     * @param source
     * @param destination
     * @throws java.io.IOException
     */
    // visible for testing
    public static void doCopySmallFile(@Nonnull File source, @Nonnull File destination) throws IOException {
        if (destination.exists() && destination.isDirectory()) {
            throw new IOException("Can not copy file, destination is a directory: " + destination.getAbsolutePath());
        }

        try (
                FileInputStream fis = new FileInputStream(source);
                FileOutputStream fos = new FileOutputStream(destination, false);
                FileChannel input = fis.getChannel();
                FileChannel output = fos.getChannel();
        ) {
            output.transferFrom(input, 0, input.size());
        }

        if (destination.length() != source.length()) {
            throw new IOException("Failed to copy content from '" +
                    source + "' (" + source.length() + "bytes) to '" + destination + "' (" + destination.length() + ")");
        }
    }

    public static void replaceFile(@Nonnull File source, @Nonnull File destination) throws IOException {
        if (destination.exists()) {
            // try to delete destination, it might fail, but doCopySmallFile() can deal with it
            if (!destination.delete()) {
                LOGGER.info("Could not delete " + destination.getAbsolutePath() + ", replacing only the content");
            }
        }
        doCopySmallFile(source, destination);
    }

    @Nonnull
    protected Writer getTemporaryFileWriter() throws IOException {
        if (temporaryFile == null) {
            temporaryFile = File.createTempFile("jmxtrans-agent-", ".data");
            temporaryFile.deleteOnExit();
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Created temporary file " + temporaryFile.getAbsolutePath());

            temporaryFileWriter = null;
        }
        if (temporaryFileWriter == null) {
            temporaryFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temporaryFile, false), Charset.forName("UTF-8")));
        }

        return temporaryFileWriter;
    }

    @Override
    public synchronized void write(@Nonnull QueryResult result) throws IOException {
        try {
            getTemporaryFileWriter().write("["+ dateFormat.format(Calendar.getInstance().getTime()) +"] "+ result.getName() + " " + result.getValue() + "\n");
        } catch (IOException e) {
            releaseTemporaryWriter();
            throw e;
        }
        postCollect();
    }

    protected void releaseTemporaryWriter() {
        try (Writer writer = getTemporaryFileWriter()) {
        } catch (IOException ignore) {
        }
        if (temporaryFile != null) {
            if (!temporaryFile.delete()) {
                LOGGER.warn("Could not delete temporary file [" + temporaryFile.getAbsolutePath() + "].");
            }
        }
        temporaryFile = null;

    }

    private synchronized void postCollect() throws IOException {
        try {
            getTemporaryFileWriter().close();
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Overwrite " + file.getAbsolutePath() + " by " + temporaryFile.getAbsolutePath());
            appendToFile(temporaryFile, file, maxFileSize, maxBackupIndex);
        } finally {
            temporaryFileWriter = null;
        }
    }

    public static final class Factory implements OutputWriterFactory<RollingFileOutputWriter> {
        public final static String SETTING_FILE_NAME = "fileName";
        public final static String SETTING_FILE_NAME_DEFAULT_VALUE = "jmxtrans-agent.data";
        public final static String SETTING_MAX_FILE_SIZE = "maxFileSize";
        public final static long SETTING_MAX_FILE_SIZE_DEFAULT_VALUE=10;
        public final static String SETTING_MAX_BACKUP_INDEX = "maxBackupIndex";
        public final static int SETTING_MAX_BACKUP_INDEX_DEFAULT_VALUE = 5;

        @Nonnull
        @Override
        public RollingFileOutputWriter create(@Nonnull Map<String, String> settings) {
            File file = new File(getString(settings, SETTING_FILE_NAME, SETTING_FILE_NAME_DEFAULT_VALUE));
            long maxFileSize = getLong(settings, SETTING_MAX_FILE_SIZE, SETTING_MAX_FILE_SIZE_DEFAULT_VALUE);
            int maxBackupIndex = getInt(settings, SETTING_MAX_BACKUP_INDEX, SETTING_MAX_BACKUP_INDEX_DEFAULT_VALUE);
            // Performance related after 10mb the Filechanel output.transferTo() performs badly
            if (maxFileSize > 10 || maxFileSize < 0) maxFileSize = 10;
            maxFileSize = maxFileSize * 1000000; //converts to bytes.

            // FIXME: not thread safe !
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            return new RollingFileOutputWriter(file, maxFileSize, maxBackupIndex, dateFormat);
        }
    }
}

