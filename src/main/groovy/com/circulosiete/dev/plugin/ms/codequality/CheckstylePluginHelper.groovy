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
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle

/**
 * Created by domix on 2/8/17.
 */
@Slf4j
class CheckstylePluginHelper {
  static setupCheckstyle(Project project, String buildDir) {
    project.apply plugin: 'checkstyle'

    def resource = this.getClass().getResource('/c7/quality/checkstyle.xml')
    if(resource) {
      FileUtils.copyURLToFile(resource, new File("$buildDir/checkstyle.xml"))
    } else {
      throw new RuntimeException("No se pudo cargar el archivo")
    }


    project.tasks.withType(Checkstyle) { task ->
    }

    project.checkstyle {
      ignoreFailures = true
      showViolations = true
      toolVersion = '7.4'

      configFile = "$buildDir/checkstyle.xml" as File
    }
    /*
    CheckstyleExtension checkstyleExt = project.extensions.getByName('checkstyle')

    project.task([type: org.gradle.api.plugins.quality.Checkstyle]).configure {
      reports {
        boolean enabledXml = runningInJenkins
        xml.enabled enabledXml
        html.enabled !enabledXml
      }
    }*/
  }
}
