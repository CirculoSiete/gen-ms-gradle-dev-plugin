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

import groovy.text.TemplateEngine
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by domix on 2/10/17.
 */
class K8sTask extends DefaultTask {

  @TaskAction
  def run() {
    project.logger.info('Generating Kubernetes config files.')

    if (!project.hasProperty('k8sReplicas')) {
      project.ext.k8sReplicas = 1
    }

    if (!project.hasProperty('k8sMaxReplicas')) {
      project.ext.k8sMaxReplicas = 4
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
      project.ext.k8sNamespace = ""
    }

    def k8sServiceName = project.ext.k8sServiceName
    project.ext.k8sConfigPath = "${project.ext.k8sBaseConfigPath1}/${k8sServiceName}"

    Integer exposedAppPort = (project.ext.appPort - 7000) + 30000
    Integer exposedAdminPort = (project.ext.adminPort - 17000) + 31000
    String namespace = project.ext.k8sNamespace ? "namespace: ${project.ext.k8sNamespace}" : ''

    Map rcBinding = [
      name        : k8sServiceName,
      replicas    : project.ext.k8sReplicas,
      max_replicas: project.ext.k8sMaxReplicas,
      version     : project.version,
      tag         : project.ext.dockerTag,
      appPort     : project.ext.appPort,
      adminPort   : project.ext.adminPort,
      configPath  : project.ext.k8sConfigPath,
      configName  : "${k8sServiceName}-config",
      registryId  : project.ext.k8sRegistry,
      namespace   : namespace,
    ]

    TemplateEngine engine = new groovy.text.SimpleTemplateEngine()
    String contentsRC = engine.createTemplate(K8sResources.rc)
      .make(rcBinding).toString()

    File rcFile = new File("${project.ext.k8sBuildDirString}/${k8sServiceName}-rc.yaml")
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
}
