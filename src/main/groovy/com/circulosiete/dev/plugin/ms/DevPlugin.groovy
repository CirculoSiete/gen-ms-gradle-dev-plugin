package com.circulosiete.dev.plugin.ms

import org.gradle.api.Plugin
import org.gradle.api.Project

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

    project.tasks.getByName('shadowJar').configure {
      mergeServiceFiles()
      exclude 'META-INF/*.DSA'
      exclude 'META-INF/*.RSA'

      baseName = 'service'
      classifier = null
      version = null
    }


    def jarManifestAttributes = [
      'Built-By'              : "Domingo Suarez Torres @ CirculoSiete.com (${System.properties['user.name']})",
      'Created-By'            : "${System.properties['java.version']} (${System.properties['java.vendor']} ${System.getProperty('java.vm.version')})",
      'Build-Date'            : project.ext.buildDate,
      'Build-Time'            : project.ext.buildTime,
      'Implementation-Version': project.ext.theVersion,
      'Main-Class'            : '______project.ext.mainClassName',
    ]
/*
    task myRun(type: JavaExec) {
  classpath sourceSets.main.runtimeClasspath
  main = mainClassName
  args 'server', 'src/main/resources/config.yaml'
}

//Borramos el archivo empacado
clean.doFirst {
  delete "../microservicios/FormularioFrecuente/service.jar"
}

//Copiamos el archivo empacado al directorio de Dockerfile
task prepareApp(type: Copy) {
  from 'build/libs/service.jar'
  into '../microservicios/FormularioFrecuente'
}

prepareApp.dependsOn shadowJar
build.dependsOn prepareApp
     */
    project.tasks.getByName('jar').configure {
      manifest {
        attributes(jarManifestAttributes)
      }
    }

    project.tasks.getByName('run').configure {
      args 'server', 'src/main/resources/config.yaml'
    }


  }

  Closure checkRequiredPlugins = {
    def checkPlugin = checkPluginName.curry(plugins)
    //println '\n==> checking required plugins'
    [
      'java', 'eclipse', 'idea', 'application',
      'com.github.johnrengelman.shadow', 'maven'
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