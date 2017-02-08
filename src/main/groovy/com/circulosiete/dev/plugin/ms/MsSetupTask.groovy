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
package com.circulosiete.dev.plugin.ms

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by domix on 2/8/17.
 */
class MsSetupTask extends DefaultTask {
  static String workDir = '/work/'

  static String getWorkDir() {
    "build/${workDir}"
  }

  @TaskAction
  def run() {
    def resource = DevPlugin.class.getResource('/c7/quality/checkstyle.xml')
    if (resource) {
      def file = new File(getWorkDir())
      if (!file.exists()) {
        file.mkdirs()
      }
      FileUtils.copyURLToFile(resource, new File("${getWorkDir()}/checkstyle.xml"))
    } else {
      throw new RuntimeException("No se pudo cargar el archivo")
    }
  }
}
