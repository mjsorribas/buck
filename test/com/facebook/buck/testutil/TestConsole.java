/*
 * Copyright 2012-present Facebook, Inc.
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

package com.facebook.buck.testutil;

import com.facebook.buck.util.Ansi;
import com.facebook.buck.util.CapturingPrintStream;
import com.facebook.buck.util.Console;
import com.facebook.buck.util.Verbosity;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * Implementation of {@link Console} to use in unit tests. Avoids the side-effect of writing to
 * actual {@link System#out} or {@link System#err}, and makes it possible to inspect what was
 * written to either stream after the fact.
 */
public class TestConsole extends Console {

  @Nullable
  private Verbosity verbosityOverride;

  public TestConsole() {
    super(Verbosity.STANDARD_INFORMATION,
          /* stdOut */ new CapturingPrintStream(),
          /* stdErr */ new CapturingPrintStream(),
          /* ansi */ Ansi.withoutTty());
  }

  public void setVerbosity(Verbosity verbosity) {
    this.verbosityOverride = Preconditions.checkNotNull(verbosity);
  }

  @Override
  public Verbosity getVerbosity() {
    if (verbosityOverride == null) {
      return super.getVerbosity();
    } else {
      return verbosityOverride;
    }
  }

  public String getTextWrittenToStdOut() {
    CapturingPrintStream stream = (CapturingPrintStream)getStdOut();
    return stream.getContentsAsString(Charsets.UTF_8);
  }

  public String getTextWrittenToStdErr() {
    CapturingPrintStream stream = (CapturingPrintStream)getStdErr();
    return stream.getContentsAsString(Charsets.UTF_8);
  }
}
