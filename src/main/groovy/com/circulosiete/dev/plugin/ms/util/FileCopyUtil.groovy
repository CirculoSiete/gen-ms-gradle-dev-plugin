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
package com.circulosiete.dev.plugin.ms.util

import org.apache.commons.io.IOUtils

/**
 * Created by domix on 2/8/17.
 */
class FileCopyUtil {
  static copyStringToFile(String sourceString, String destination) {
    def destinationFile = destination as File
    def parentFile = destinationFile.parentFile
    if (!parentFile.exists() && !parentFile.mkdirs()) {
      throw new IllegalStateException("unable to create directory $parentFile")
    }
    if (destinationFile.exists()) {
      destinationFile.delete()
    }
    if (!destinationFile.createNewFile()) {
      throw new IllegalStateException("unable to create file $destination")
    }
    def fileOutputStream = new FileOutputStream(destinationFile)
    try {
      IOUtils.copy(new ByteArrayInputStream(sourceString.bytes), fileOutputStream)
      fileOutputStream.close()
    } finally {
      IOUtils.closeQuietly(fileOutputStream)
    }
  }
}
