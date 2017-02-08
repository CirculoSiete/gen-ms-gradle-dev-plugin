/*
 *
 * Copyright (C) 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.circulosiete.dev.plugin.ms.codequality

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.FindBugs
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Created by domix on 2/8/17.
 */
@Slf4j
class QualityConfigHelper {

  static setup(Project project, String buildDir) {

    project.checkstyle {
      ignoreFailures = true
      showViolations = false
      toolVersion = '7.4'

      configFile = "$buildDir/checkstyle.xml" as File
    }

    project.tasks.withType(Checkstyle) { task ->
      task.configure {
        reports {
          boolean enabledXml = project.ext.runningInJenkins
          xml.enabled enabledXml
          html.enabled !enabledXml
        }
      }
    }

    project.findbugs {
      effort = 'max'
      reportLevel = 'low'
      ignoreFailures = true
    }

    project.tasks.withType(FindBugs) { task ->
      task.configure {
        reports {
          boolean enabledXml = project.ext.runningInJenkins
          xml.enabled enabledXml
          html.enabled !enabledXml
        }
      }
    }

    project.jacoco {
      toolVersion = '0.7.8'
    }

    project.tasks.withType(JacocoReport) { task ->
      task.configure {
        reports {
          boolean enabledXml = project.ext.runningInJenkins
          xml.enabled enabledXml
          html.enabled !enabledXml
        }
      }
    }

    project.jdepend {
      ignoreFailures = true
    }

    project.jdependMain {
      reports {
        boolean enabledXml = project.ext.runningInJenkins
        xml.enabled enabledXml
        text.enabled !enabledXml
      }
    }

    project.jdependTest {
      reports {
        boolean enabledXml = project.ext.runningInJenkins
        xml.enabled enabledXml
        text.enabled !enabledXml
      }
    }

    project.pmd {
      consoleOutput = project.ext.runningInJenkins
      ignoreFailures = true
      targetJdk = 1.7
      toolVersion = '5.5.2'
    }

    project.pmdMain {
      reports {
        boolean enabledXml = project.ext.runningInJenkins
        xml.enabled enabledXml
        html.enabled !enabledXml
      }
    }

    project.pmdTest {
      reports {
        boolean enabledXml = project.ext.runningInJenkins
        xml.enabled enabledXml
        html.enabled !enabledXml
      }
    }

    /*
    project.release {
      git {
        requireBranch = 'release'
      }
    }
    */
  }
}
