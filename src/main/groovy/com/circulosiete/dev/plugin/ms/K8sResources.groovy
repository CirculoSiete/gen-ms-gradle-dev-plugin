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

/**
 * Created by domix on 1/10/17.
 */
class K8sResources {
  static final rc = '''
apiVersion: v1
kind: ReplicationController
metadata:
  name: ${name}
spec:
  replicas: ${replicas}
  selector:
    name: ${name}
    version: ${version}
  template:
    metadata:
      labels:
        name: ${name}
        version: ${version}
    spec:
      containers:
      - image: ${tag}
        name: ${name}
        volumeMounts:
          - mountPath: /config
            name: config-${name}
        ports:
        - containerPort: ${appPort}
          protocol: TCP
        - containerPort: ${adminPort}
          protocol: TCP
      volumes:
        - name: config-${name}
          hostPath:
            # directory location on host
            path: ${configPath}
'''
  static final np = '''
apiVersion: v1
kind: Service
metadata:
  name: ${name}
  labels:
    name: ${name}
spec:
  type: NodePort
  ports:
  - port: ${appPort}
    targetPort: ${appPort}
    nodePort: ${exposedAppPort}
    protocol: TCP
  - port: ${adminPort}
    targetPort: ${adminPort}
    nodePort: ${exposedAdminPort}
    protocol: TCP
  selector:
    name: ${name}
'''
}
