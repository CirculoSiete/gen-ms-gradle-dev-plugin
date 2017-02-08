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

    FileUtils.copyURLToFile(this.getClass().getResource('/c7/quality/checkstyle.xml'), new File("$buildDir/checkstyle.xml"))

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
