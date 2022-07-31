/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@LoggerContextSource(value= "log4j-rolling-random-direct-switch-director.xml")
public class RollingRandomAppenderDirectWriteAndSwitchDirectorTest {

    private final static String DIR = "target/rolling-random-direct-switch-director";
    private final Logger logger;

    public RollingRandomAppenderDirectWriteAndSwitchDirectorTest(final LoggerContext context) {
        this.logger = context.getLogger(RollingRandomAppenderDirectWriteAndSwitchDirectorTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        String uuid = UUID.randomUUID().toString();
        ThreadContext.put("uuid", uuid);
        logger.info("RollingRandomAppenderDirectWriteAndSwitchDirectorTest uuid:" + uuid);
        LocalTime start = LocalTime.now();
        LocalTime end;
        do {
            end = LocalTime.now();
            logger.info("test log");
            Thread.sleep(100);
        } while (start.getSecond() == end.getSecond());

        File nextLogFile = new File(String.format("%s/%s/%d/%d.log", DIR, uuid, end.getSecond(), end.getSecond()));
        assertTrue(nextLogFile.exists(), "nextLogFile not created");
        String subdir = String.format("%s/%s", DIR, uuid);
        Thread.sleep(1000);
        assertTrue(cleanPath(Paths.get(subdir)), "clean sub director failed");
    }

    private boolean cleanPath(final Path path) throws IOException {
        if (Files.exists(path) && Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return true;
    }
}
