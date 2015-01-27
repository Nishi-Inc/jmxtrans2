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
package org.jmxtrans.standalone.cli;

import com.beust.jcommander.Parameter;
import lombok.Getter;

import java.io.File;
import java.util.List;

@Getter
public class JmxTransParameters {

    @Parameter(
            names = { "--configFiles", "-c"},
            description = "List of configuration files.",
            validateValueWith = ExistingFileValidator.class)
    private List<File> configFiles;

    @Parameter(
            names = { "--configDir", "-d"},
            description = "List of configuration directories, all files in those directories will be loaded.",
            validateValueWith = ExistingDirectoryValidator.class)
    private List<File> configDirs;

    @Parameter(
            names = { "--ignoreParsingErrors", "-i"},
            description = "Start JmxTrans even if errors are found in configuration files.")
    private boolean ignoreParsingErrors = false;

}
