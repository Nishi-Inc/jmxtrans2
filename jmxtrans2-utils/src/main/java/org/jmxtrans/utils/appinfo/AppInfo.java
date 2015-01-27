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
package org.jmxtrans.utils.appinfo;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

public class AppInfo<T> {

    @Nonnull private final GitRepositoryState repositoryState;
    @Nullable private final String groupId;
    @Nullable private final String artifactId;
    @Nullable @Getter private final String version;
    @Nullable @Getter private final String name;
    @Nullable private final String description;
    @Nullable private final String inceptionYear;
    @Nullable private final String issueManagementUrl;

    public AppInfo(
            @Nonnull Properties properties,
            @Nonnull GitRepositoryState repositoryState) {
        this.repositoryState = repositoryState;

        groupId = properties.getProperty("project.groupId");
        artifactId = properties.getProperty("project.artifactId");
        version = properties.getProperty("project.version");
        name = properties.getProperty("project.name");
        description = properties.getProperty("project.description");
        inceptionYear = properties.getProperty("project.inceptionYear");
        issueManagementUrl = properties.getProperty("project.issueManagement.url");
    }

    @Nonnull
    public String getFullVersion() {
        return getVersion() + " / " + repositoryState.getDescribe();
    }

    @Nullable
    public String getIssueManagementUrl() {
        return issueManagementUrl;
    }

    public void print(@Nonnull PrintStream out) {
        out.println(getName());
        out.println("version: " + getFullVersion());
        out.println("last modified: " + repositoryState.getCommitTime());
        out.println("build time: " + repositoryState.getBuildTime());
        out.println("license: MIT");
        out.println("in case of problem, open an issue on " + getIssueManagementUrl());
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> AppInfo<T> load(@Nonnull Class<T> clazz) throws IOException {
        try (InputStream in = clazz.getResourceAsStream("app-info.properties")) {
            Properties properties = new Properties();
            properties.load(in);
            return new AppInfo(
                    properties,
                    GitRepositoryState.load()
            );
        }
    }
}
