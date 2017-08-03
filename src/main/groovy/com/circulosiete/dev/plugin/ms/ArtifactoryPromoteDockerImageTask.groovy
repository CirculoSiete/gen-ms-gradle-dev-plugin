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
