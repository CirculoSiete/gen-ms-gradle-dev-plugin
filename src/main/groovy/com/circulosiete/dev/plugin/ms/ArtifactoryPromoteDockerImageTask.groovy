package com.circulosiete.dev.plugin.ms

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import wslite.http.auth.HTTPBasicAuthorization
import wslite.rest.RESTClient

class ArtifactoryPromoteDockerImageTask extends DefaultTask {

  ArtifactoryPromoteDockerImageTask() {
    group = "Artifactory"
  }


  @Input
  def sourceRepo

  @Input
  def targetRepo

  @Input
  def username

  @Input
  def dockerRepository

  @Input
  def password

  @Input
  def contextUrl

  @Input
  Boolean copy = true

  @TaskAction
  void promote() {
    def url = "${contextUrl}api/docker/${sourceRepo}/v2/promote"
    RESTClient client = new RESTClient(url)
    client.authorization = new HTTPBasicAuthorization(username, password)

    def result = client.post() {
      json targetRepo: targetRepo, copy: copy, dockerRepository: dockerRepository
    }

    println '=' * 80
    println result.response.contentAsString
    println '=' * 80
  }
}
