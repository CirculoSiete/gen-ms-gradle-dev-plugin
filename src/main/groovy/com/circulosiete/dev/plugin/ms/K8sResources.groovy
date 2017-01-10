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
        ports:
        - containerPort: ${appPort}
          protocol: TCP
        - containerPort: ${adminPort}
          protocol: TCP
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
