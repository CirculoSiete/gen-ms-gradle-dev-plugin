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

import groovy.text.TemplateEngine
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
    //[javaPluginConvention.compileJava, javaPluginConvention.compileTestJava]*.options*.encoding = 'UTF-8'

    def k8sServiceName = project.name.split("(?=\\p{Upper})").join('-').toLowerCase()
    project.ext.k8sServiceName = k8sServiceName

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

    if (project.hasProperty('registryUrl')) {
      project.ext.registryUrl = project.property('registryUrl')
    } else {
      project.ext.registryUrl = 'https://hub.docker.com'
    }

    if (project.hasProperty('registryUsername')) {
      project.ext.registryUsername = project.property('registryUsername')
    } else {
      project.ext.registryUsername = ''
    }

    if (project.hasProperty('registryPassword')) {
      project.ext.registryPassword = project.property('registryPassword')
    } else {
      project.ext.registryPassword = ''
    }

    if (!project.ext.has('dockerTag')) {

      def tagData = []
      if (project.ext.registryUrl) {
        tagData << project.ext.registryUrl.split('//').toList().stream().filter {
          !it.startsWith('http')
        }.findFirst().orElse('')
        tagData << '/'
      }

      if (!project.ext.has('drHubProject')) {
        project.ext.drHubProject = 'microservices'
      }
      if (project.ext.drHubProject) {
        tagData << project.ext.drHubProject
        tagData << '/'
      }

      project.ext.dockerTag = "${tagData.join('')}${k8sServiceName}:${project.version}".toLowerCase()

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
      url = project.ext.registryUrl
      username = project.ext.registryUsername
      password = project.ext.registryPassword
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

      if (project.hasProperty('drFromImage')) {
        project.ext.drFromImage = project.property('drFromImage')
      } else {
        project.ext.drFromImage = 'openjdk:8u111-jre-alpine'
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

    def ideaExtension = project.extensions.getByName('idea')
    ideaExtension.project {
      ipr {
        withXml { provider ->
          // Get XML as groovy.util.Node to work with.
          def projectXml = provider.asNode()

          // Find compiler configuration component.
          def compilerConfiguration = projectXml.component.find { component ->
            component.'@name' == 'CompilerConfiguration'
          }

          // Replace current annotationProcessing
          // that is part of the compiler configuration.
          def currentAnnotationProcessing = compilerConfiguration.annotationProcessing
          currentAnnotationProcessing.replaceNode {
            annotationProcessing {
              profile(name: 'Default', default: true, enabled: true) {
                processorPath(useClasspath: true)
              }
            }
          }
        }
      }
    }

    project.task('k8s') {
      description = 'Create Kubernetes configuration.'
      group = 'Kubernetes'

      if (!project.ext.has('k8sBuildDirString')) {
        project.ext.k8sBuildDirString = "${project.buildDir}/k8s"
      }

      if (!project.ext.has('k8sBuildDir')) {
        project.ext.k8sBuildDir = project.mkdir(project.ext.k8sBuildDirString)
      }

      if (!project.ext.has('k8sReplicas')) {
        project.ext.k8sReplicas = 2
      }

      if (project.hasProperty('k8sBaseConfigPath')) {
        project.ext.k8sBaseConfigPath1 = project.property('k8sBaseConfigPath')
      } else {
        project.ext.k8sBaseConfigPath1 = "/config"
      }

      if (project.hasProperty('k8sRegistry')) {
        project.ext.k8sRegistry = project.property('k8sRegistry')
      } else {
        project.ext.k8sRegistry = "registry"
      }

      if (project.hasProperty('k8sNamespace')) {
        project.ext.k8sNamespace = project.property('k8sNamespace')
      } else {
        project.ext.k8sNamespace = "ns-default"
      }

      project.ext.k8sConfigPath = "${project.ext.k8sBaseConfigPath1}/${k8sServiceName}"

      Integer exposedAppPort = (project.ext.appPort - 7000) + 30000
      Integer exposedAdminPort = (project.ext.adminPort - 17000) + 31000
      String namespace = project.ext.k8sNamespace

      Map rcBinding = [
        name      : k8sServiceName,
        replicas  : project.ext.k8sReplicas,
        version   : project.version,
        tag       : project.ext.dockerTag,
        appPort   : project.ext.appPort,
        adminPort : project.ext.adminPort,
        configPath: project.ext.k8sConfigPath,
        configName: "${k8sServiceName}-config",
        registryId: project.ext.k8sRegistry,
        namespace : namespace,
      ]

      TemplateEngine engine = new groovy.text.SimpleTemplateEngine()
      String contentsRC = engine.createTemplate(K8sResources.rc)
        .make(rcBinding).toString()

      File rcFile = new File("${project.ext.k8sBuildDirString}/${k8sServiceName}-rc.yaml")
      rcFile.getParentFile().mkdirs()
      rcFile.text = contentsRC

      Map npBinding = [
        name            : k8sServiceName,
        version         : project.version,
        appPort         : project.ext.appPort,
        exposedAppPort  : exposedAppPort,
        adminPort       : project.ext.adminPort,
        exposedAdminPort: exposedAdminPort,
        namespace       : namespace,
      ]
      String contentsSvc = engine.createTemplate(K8sResources.np)
        .make(npBinding).toString()

      File svcFile = new File("${project.ext.k8sBuildDirString}/${k8sServiceName}-srv-np.yaml")
      svcFile.text = contentsSvc
    }

    project.task([type: org.gradle.api.tasks.Copy, dependsOn: 'k8s'], 'k8s-copy') {
      description = 'Copy Kubernetes configuration to desired location.'
      group = 'Kubernetes'

      if (project.hasProperty('k8sConfigLocation')) {
        project.ext.k8sConfigLocation = project.property('k8sConfigLocation')
      } else {
        project.ext.k8sConfigLocation = project.ext.dockerBuildDir
      }

      from project.ext.k8sBuildDirString
      into "${project.ext.k8sConfigLocation}/${project.ext.k8sServiceName}"
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