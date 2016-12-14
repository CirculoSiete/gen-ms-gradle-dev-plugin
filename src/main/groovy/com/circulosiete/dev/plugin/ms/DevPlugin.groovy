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

import static com.bmuschko.gradle.docker.DockerRemoteApiPlugin.DOCKER_JAVA_CONFIGURATION_NAME

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

import java.text.SimpleDateFormat

class DevPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.allprojects checkRequiredPlugins

    Date buildTimeAndDate = new Date()

    if (!project.ext.has('buildDate')) {
      project.ext.buildDate = new SimpleDateFormat('dd-MMM-yyyy').format(buildTimeAndDate)
    }

    if (!project.ext.has('buildTime')) {
      project.ext.buildTime = new SimpleDateFormat('hh:mm aa').format(buildTimeAndDate)
    }

    if (!project.ext.has('theVersion')) {
      project.ext.theVersion = project.version
    }

    if (!project.ext.has('dockerTag')) {
      project.ext.dockerTag = "vhub.cosapidata.com.pe/gdg-lima-talk:${project.version}".toLowerCase()
    }

    if (!project.ext.has('dockerBuildDirString')) {
      project.ext.dockerBuildDirString = "${project.buildDir}/docker"
    }

    if (!project.ext.has('dockerBuildDir')) {
      project.ext.dockerBuildDir = project.mkdir(project.ext.dockerBuildDirString)
    }

    if (!project.ext.has('dockerBuildGroup')) {
      project.ext.dockerBuildGroup = 'Docker'
    }

    Configuration config = project.configurations[DOCKER_JAVA_CONFIGURATION_NAME]
    config.resolutionStrategy {
      force 'de.gesellix:unix-socket-factory:2016-04-06T22-21-19'
    }

    project.tasks.getByName('shadowJar').configure {
      mergeServiceFiles()
      exclude 'META-INF/*.DSA'
      exclude 'META-INF/*.RSA'

      baseName = 'service'
      classifier = null
      version = null
      project.ext.finalJarFilename = 'service.jar'
    }

    //project.extensions.getByName()

    def dockerExtension = project.extensions.getByName('docker')
    dockerExtension.url = System.env.DOCKER_HOST ?
      System.env.DOCKER_HOST.replace("tcp", "https") :
      'unix:///var/run/docker.sock'

    dockerExtension.registryCredentials {
      url = project.hasProperty('drSunatUrl') ? drSunatUrl : ''

      username = project.hasProperty('drSunatUsername') ? drSunatUsername : ''
      password = project.hasProperty('drSunatPassword') ? drSunatPassword : ''
    }

    def jarManifestAttributes = [
      'Built-By'              : "Domingo Suarez Torres @ CirculoSiete.com (${System.properties['user.name']})",
      'Created-By'            : "${System.properties['java.version']} (${System.properties['java.vendor']} ${System.getProperty('java.vm.version')})",
      'Build-Date'            : project.ext.buildDate,
      'Build-Time'            : project.ext.buildTime,
      'Implementation-Version': project.ext.theVersion
    ]

    project.tasks.getByName('jar').configure {
      manifest {
        attributes(jarManifestAttributes)
      }
    }

    project.tasks.getByName('run').configure {
      args 'server', 'src/main/resources/config.yaml'
    }

    project.tasks.getByName('build').dependsOn('shadowJar')

    project.task([type: org.gradle.api.tasks.Copy, dependsOn: 'build'], 'dockerRepackage') {
      description = 'Repackage application JAR to make it runnable.'
      group = project.ext.dockerBuildGroup

      project.ext {
        dockerJar = project.file("build/libs/${project.jar.archiveName}")
      }

      from "build/libs/${project.ext.finalJarFilename}"
      into project.ext.dockerBuildDir
    }

    project.task([type: com.bmuschko.gradle.docker.tasks.image.Dockerfile, dependsOn: 'dockerRepackage'], 'createDockerfile') {
      description = 'Create Dockerfile to build image.'
      group = project.ext.dockerBuildGroup

      destFile = project.file("${project.ext.dockerBuildDir}/Dockerfile")

      if (!project.ext.has('drFromImage')) {
        project.ext.drFromImage = 'openjdk:8-jre-alpine'
      }

      if (!project.ext.has('drMantainer')) {
        project.ext.drMantainer = 'Domingo Suarez Torres <domingo.suarez@gmail.com>'
      }


      from project.ext.drFromImage
      instruction "# Se usara ${project.ext.dockerTag}"
      maintainer project.ext.drMantainer

      exposePort 8080

      copyFile project.ext.finalJarFilename, '/app/application.jar'

      entryPoint 'java', '-jar', '/app/application.jar'

    }

  }


  Closure checkRequiredPlugins = {
    def checkPlugin = checkPluginName.curry(plugins)
    //println '\n==> checking required plugins'
    [
      'java', 'eclipse', 'idea', 'application',
      'com.github.johnrengelman.shadow', 'maven',
      'com.bmuschko.docker-remote-api'
    ].each {
      checkPlugin it
    }
  }

  Closure checkPluginName = { plugins, pluginName ->
    if (!plugins.hasPlugin(pluginName)) {
      println "Adding plugin: $pluginName"
      plugins.apply(pluginName)
    }
  }

}