/*
 * Copyright 2011 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.maven;

import java.io.File;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.tooling.AnalysisResult;
import org.pitest.mutationtest.tooling.CombinedStatistics;
import org.pitest.mutationtest.tooling.EntryPoint;

public class RunPitStrategy implements GoalStrategy {

  @Override
  public CombinedStatistics execute(File baseDir, ReportOptions data,
      PluginServices plugins, Map<String, String> environmentVariables)
          throws MojoExecutionException {

    EntryPoint e = new EntryPoint();
    AnalysisResult result = e.execute(baseDir, data, plugins,
        environmentVariables);
    if (result.getError().isPresent()) {
      throw new MojoExecutionException("fail", result.getError().get());
    }
    return result.getStatistics().get();
  }

}
