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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class GitRepositoryState {
    private final String branch;
    private final String tags;
    private final String describe;
    private final String describeShort;
    private final String commitId;
    private final String buildUserName;
    private final String buildUserEmail;
    private final String buildTime;
    private final String commitUserName;
    private final String commitUserEmail;
    private final String commitMessageShort;
    private final String commitMessageFull;
    private final String commitTime;

    private GitRepositoryState(Properties properties) {
        this.branch = properties.get("git.branch").toString();
        this.tags = properties.get("git.tags").toString();
        this.describe = properties.get("git.commit.id.describe").toString();
        this.describeShort = properties.get("git.commit.id.describe-short").toString();
        this.commitId = properties.get("git.commit.id").toString();
        this.buildUserName = properties.get("git.build.user.name").toString();
        this.buildUserEmail = properties.get("git.build.user.email").toString();
        this.buildTime = properties.get("git.build.time").toString();
        this.commitUserName = properties.get("git.commit.user.name").toString();
        this.commitUserEmail = properties.get("git.commit.user.email").toString();
        this.commitMessageShort = properties.get("git.commit.message.short").toString();
        this.commitMessageFull = properties.get("git.commit.message.full").toString();
        this.commitTime = properties.get("git.commit.time").toString();
    }

    public static GitRepositoryState load() throws IOException {
        try (InputStream in = GitRepositoryState.class.getClassLoader().getResourceAsStream("git.properties")) {
            Properties properties = new Properties();
            properties.load(in);
            return new GitRepositoryState(properties);
        }
    }

}
