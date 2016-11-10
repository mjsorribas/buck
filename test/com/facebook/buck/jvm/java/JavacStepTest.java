/*
 * Copyright 2015-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.empty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.event.BuckEventBusFactory;
import com.facebook.buck.model.BuildTargetFactory;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.DefaultTargetNodeToBuildRuleTransformer;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.TargetGraph;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.StepExecutionResult;
import com.facebook.buck.step.TestExecutionContext;
import com.facebook.buck.testutil.FakeProjectFilesystem;
import com.facebook.buck.testutil.TestConsole;
import com.facebook.buck.util.FakeProcessExecutor;
import com.facebook.buck.util.FakeProcess;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import java.nio.file.Paths;

import java.util.Optional;

import org.junit.Test;

public class JavacStepTest {

  @Test
  public void findFailedImports() throws Exception {
    String lineSeperator = System.getProperty("line.separator");

    String stderrOutput = Joiner.on(lineSeperator).join(
        ImmutableList.of(
            "java/com/foo/bar.java:5: package javax.annotation.concurrent does not exist",
            "java/com/foo/bar.java:99: error: cannot access com.facebook.Raz",
            "java/com/foo/bar.java:142: cannot find symbol: class ImmutableSet",
            "java/com/foo/bar.java:999: you are a clown"));

    ImmutableSet<String> missingImports =
        JavacStep.findFailedImports(stderrOutput);
    assertEquals(
        ImmutableSet.of("javax.annotation.concurrent", "com.facebook.Raz", "ImmutableSet"),
        missingImports);
  }

  @Test
  public void successfulCompileDoesNotSendStdoutAndStderrToConsole() throws Exception {
    FakeJavac fakeJavac = new FakeJavac();
    BuildRuleResolver buildRuleResolver =
        new BuildRuleResolver(TargetGraph.EMPTY, new DefaultTargetNodeToBuildRuleTransformer());
    SourcePathResolver sourcePathResolver = new SourcePathResolver(buildRuleResolver);
    ProjectFilesystem fakeFilesystem = FakeProjectFilesystem.createJavaOnlyFilesystem();
    JavacOptions javacOptions = JavacOptions.builder()
        .setSourceLevel("8.0")
        .setTargetLevel("8.0")
        .build();

    JavacStep step = new JavacStep(
        Paths.get("output"),
        NoOpClassUsageFileWriter.instance(),
        Optional.empty(),
        Optional.empty(),
        ImmutableSortedSet.of(),
        Paths.get("pathToSrcsList"),
        ImmutableSortedSet.of(),
        fakeJavac,
        javacOptions,
        BuildTargetFactory.newInstance("//foo:bar"),
        Optional.empty(),
        sourcePathResolver,
        fakeFilesystem);

    FakeProcess fakeJavacProcess = new FakeProcess(0, "javac stdout\n", "javac stderr\n");

    ExecutionContext executionContext = TestExecutionContext.newBuilder()
        .setProcessExecutor(
            new FakeProcessExecutor(
                Functions.constant(fakeJavacProcess),
                new TestConsole()))
        .build();
    BuckEventBusFactory.CapturingConsoleEventListener listener =
        new BuckEventBusFactory.CapturingConsoleEventListener();
    executionContext.getBuckEventBus().register(listener);
    StepExecutionResult result = step.execute(executionContext);

    // Note that we don't include stderr in the step result on success.
    assertThat(result, equalTo(StepExecutionResult.SUCCESS));
    assertThat(listener.getLogMessages(), empty());
  }

  @Test
  public void failedCompileSendsStdoutAndStderrToConsole() throws Exception {
    FakeJavac fakeJavac = new FakeJavac();
    BuildRuleResolver buildRuleResolver =
        new BuildRuleResolver(TargetGraph.EMPTY, new DefaultTargetNodeToBuildRuleTransformer());
    SourcePathResolver sourcePathResolver = new SourcePathResolver(buildRuleResolver);
    ProjectFilesystem fakeFilesystem = FakeProjectFilesystem.createJavaOnlyFilesystem();
    JavacOptions javacOptions = JavacOptions.builder()
        .setSourceLevel("8.0")
        .setTargetLevel("8.0")
        .build();

    JavacStep step = new JavacStep(
        Paths.get("output"),
        NoOpClassUsageFileWriter.instance(),
        Optional.empty(),
        Optional.empty(),
        ImmutableSortedSet.of(),
        Paths.get("pathToSrcsList"),
        ImmutableSortedSet.of(),
        fakeJavac,
        javacOptions,
        BuildTargetFactory.newInstance("//foo:bar"),
        Optional.empty(),
        sourcePathResolver,
        fakeFilesystem);

    FakeProcess fakeJavacProcess = new FakeProcess(1, "javac stdout\n", "javac stderr\n");

    ExecutionContext executionContext = TestExecutionContext.newBuilder()
        .setProcessExecutor(
            new FakeProcessExecutor(
                Functions.constant(fakeJavacProcess),
                new TestConsole()))
        .build();
    BuckEventBusFactory.CapturingConsoleEventListener listener =
        new BuckEventBusFactory.CapturingConsoleEventListener();
    executionContext.getBuckEventBus().register(listener);
    StepExecutionResult result = step.execute(executionContext);

    assertThat(result, equalTo(StepExecutionResult.of(1, Optional.of("javac stderr\n"))));
    assertThat(
        listener.getLogMessages(),
        equalTo(ImmutableList.of("javac stdout\n", "javac stderr\n")));
  }
}
