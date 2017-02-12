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
  static final rc = '''apiVersion: v1
kind: ReplicationController
metadata:
  name: ${name}-rc
  ${namespace}
spec:
  replicas: ${replicas}
  selector:
    name: ${name}-rc
    version: ${version}
  template:
    metadata:
      labels:
        name: ${name}-rc
        version: ${version}
    spec:
      imagePullSecrets:
        - name: ${registryId ?: ''}
      containers:
        - image: ${tag}
          name: ${name}-img
          imagePullPolicy: Always
          resources:
            requests:
              memory: "128Mi"
              cpu: "250m"
            limits:
              memory: "256Mi"
              cpu: "500m"
          volumeMounts:
            - mountPath: /config
              name: ${configName}
          ports:
            - containerPort: ${appPort}
              protocol: TCP
            - containerPort: ${adminPort}
              protocol: TCP
          livenessProbe:
            httpGet:
              path: /healthcheck
              port: ${adminPort}
            initialDelaySeconds: 180
            periodSeconds: 180
            timeoutSeconds: 5
      volumes:
        - name: ${configName}
          hostPath:
            # directory location on host
            path: ${configPath}
#---
# apiVersion: autoscaling/v1
# kind: HorizontalPodAutoscaler
# metadata:
#   name: ${name}-hpas
#   ${namespace}
# spec:
#   maxReplicas: ${max_replicas}
#   minReplicas: ${replicas}
#   scaleTargetRef:
#     apiVersion: v1
#     kind: ReplicationController
#     name: ${name}-rc
'''
  static final np = '''apiVersion: v1
kind: Service
metadata:
  name: ${name}-srv-np
  labels:
    name: ${name}-srv-np
  ${namespace}
spec:
  type: NodePort
  ports:
    - port: ${appPort}
      targetPort: ${appPort}
      nodePort: ${exposedAppPort}
      protocol: TCP
      name: app
    - port: ${adminPort}
      targetPort: ${adminPort}
      nodePort: ${exposedAdminPort}
      protocol: TCP
      name: admin
  selector:
    name: ${name}-rc
    version: ${version}
'''
}
