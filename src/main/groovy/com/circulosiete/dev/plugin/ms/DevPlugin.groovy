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
import org.gradle.api.plugins.JavaPluginConvention
import org.yaml.snakeyaml.Yaml

import java.text.SimpleDateFormat

class DevPlugin implements Plugin<Project> {

  public static final String DEFAULT_CONFIG_FILE = 'src/main/resources/config.yaml'

  @Override
  void apply(Project project) {
    project.allprojects checkRequiredPlugins

    JavaPluginConvention javaPluginConvention = (JavaPluginConvention) project.convention.plugins.java

    javaPluginConvention.sourceCompatibility = 1.8
    javaPluginConvention.targetCompatibility = 1.8

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
      if (!project.ext.has('drHub')) {
        project.ext.drHub = 'vhub.cosapidata.com.pe'
      }

      def tagData = []
      if (project.ext.drHub) {
        tagData << project.ext.drHub
        tagData << '/'
      }

      if (!project.ext.has('drHubProject')) {
        project.ext.drHubProject = 'microservices'
      }
      if (project.ext.drHubProject) {
        tagData << project.ext.drHubProject
        tagData << '/'
      }

      project.ext.dockerTag = "${tagData.join('')}${project.name}:${project.version}".toLowerCase()

    }

    println "Docker Tag '${project.ext.dockerTag}'"

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

    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win")
    if (!isWindows) {
      config.resolutionStrategy {
        force 'de.gesellix:unix-socket-factory:2016-04-06T22-21-19'
      }
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

    def dockerExtension = project.extensions.getByName('docker')

    dockerExtension.registryCredentials {
      url = project.hasProperty('drSunatUrl') ? project.property('drSunatUrl') : ''

      username = project.hasProperty('drSunatUsername') ? project.property('drSunatUsername') : ''
      password = project.hasProperty('drSunatPassword') ? project.property('drSunatPassword') : ''
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
      args 'server', DEFAULT_CONFIG_FILE
    }

    project.tasks.getByName('build').dependsOn('shadowJar')

    portApp(project)

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
        project.ext.drMantainer = 'Domingo Suarez Torres <domingo@circulosiete.com>'
      }

      from project.ext.drFromImage
      maintainer project.ext.drMantainer

      exposePort project.ext.appPort, project.ext.adminPort

      copyFile project.ext.finalJarFilename, '/app/application.jar'

      volume '/config'

      entryPoint 'java', '-jar', '/app/application.jar', 'server', '/config/config.yaml'

    }

    project.task([type: com.bmuschko.gradle.docker.tasks.image.DockerBuildImage, dependsOn: 'createDockerfile'], 'buildImage') {
      description = 'Create Docker image with application.'
      group = project.ext.dockerBuildGroup

      inputDir = project.file(project.ext.dockerBuildDir)
      tag = project.ext.dockerTag
    }

    project.task([type: com.bmuschko.gradle.docker.tasks.image.DockerPushImage, dependsOn: 'buildImage'], 'pushImage') {
      conventionMapping.imageName = { project.ext.dockerTag }
    }

  }

  Closure checkRequiredPlugins = {
    def checkPlugin = checkPluginName.curry(plugins)
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
      println "Applying plugin: $pluginName"
      plugins.apply(pluginName)
    }
  }

  void portApp(Project project) {
    try {
      Yaml yaml = new Yaml()
      Object load = yaml.load(new File(DEFAULT_CONFIG_FILE).text)

      project.ext.appPort = new Integer(load.server.applicationConnectors[0].port)
      project.ext.adminPort = new Integer(load.server.adminConnectors[0].port)
    } catch (Throwable t) {
      project.logger.error(t.message)
      project.ext.appPort = 0
      project.ext.adminPort = 0
    }
  }

}