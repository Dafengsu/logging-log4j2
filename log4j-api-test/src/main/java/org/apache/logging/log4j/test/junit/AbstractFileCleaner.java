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

package org.apache.logging.log4j.test.junit;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

abstract class AbstractFileCleaner implements BeforeEachCallback {

    public static final String MAX_TRIES_PROPERTY = "log4j2.junit.fileCleanerMaxTries";
    public static final String SLEEP_PERIOD_MILLIS_PROPERTY = "log4j2.junit.fileCleanerSleepPeriodMillis";

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        clean(context);
    }

    private void clean(final ExtensionContext context) {
        final Collection<Path> paths = getPathsForTest(context);
        if (paths.isEmpty()) {
            return;
        }
        final Map<Path, IOException> failures = new LinkedHashMap<>();
        final int maxTries = context.getConfigurationParameter(MAX_TRIES_PROPERTY, Integer::valueOf).orElse(10);
        final int sleepPeriodMillis = context.getConfigurationParameter(SLEEP_PERIOD_MILLIS_PROPERTY, Integer::valueOf).orElse(200);
        final int sleepBasePeriodMillis = sleepPeriodMillis / maxTries;
        for (final Path path : paths) {
            if (Files.exists(path)) {
                for (int i = 0, sleepMillis = sleepBasePeriodMillis; i < maxTries; i++, sleepMillis <<= 1) {
                    try {
                        PathUtils.delete(path);
                        failures.remove(path);
                    } catch (final IOException e) {
                        failures.put(path, e);
                    }
                    LockSupport.parkNanos(path, TimeUnit.MILLISECONDS.toNanos(sleepMillis));
                    if (Thread.interrupted()) {
                        failures.put(path, new InterruptedIOException());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        if (!failures.isEmpty()) {
            final String message = failures.entrySet().stream()
                    .map(e -> e.getKey() + " failed with " + e.getValue())
                    .collect(Collectors.joining(", "));
            fail(message);
        }
    }

    abstract Collection<Path> getPathsForTest(final ExtensionContext context);

}
