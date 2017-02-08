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
      throw new IllegalStateException("unable to create fille $destination")
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
