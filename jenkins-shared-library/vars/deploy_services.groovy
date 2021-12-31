#!/usr/bin/env groovy
/*
* Name: deploy_services
* Author: Vinay Lakshmaiah
* Date: 11-30-2021
*/

import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils;
import hudson.model.*

def call(environment, commit_hash, REPO, NAMESPACE, service_changeset, currentBuild, alb_security, cert_arns, HELM_TIMEOUT, helm_verbose_level, set_kibana_url, set_affected_service, affected_service, kibana_url, deploy_gateway) {
    currentBuildNumber = currentBuild.getNumber()
    def er = null
    def account_id = nfAWSAccounts account_name: "${environment}"
    def svcChangeSet = service_changeset.sort()
    def serviceArrary = []
    def parameters_yaml = readYaml file: "pipelines/continous_deployment/conf/services.yaml"
    def db_env_replacements = parameters_yaml["db_env_replacements"]
    withAWS(credentials: 'jenkins-central-mgt', region: 'us-east-1', role:'jenkins-central-mgt',  roleAccount: "${account_id}") {                      
        svcChangeSet.each{
            def buildServices = [:]
            def arrayValue = it.value
            arrayValue.each { arrVal ->
                buildServices["${arrVal}"] = { ->
                    def serviceName =  "${arrVal}"
                    def ecs_service_name= "${arrVal}"

                    if(serviceName == "authorization") {
                        serviceName  = "auth"
                        ecs_service_name = "auth"
                    }
                    if(serviceName == "gateway"){
                        ecs_service_name = "fargate-gateway"
                        deploy_gateway = true
                    }
                    if(serviceName == "registration"){
                        ecs_service_name = "fargate-registration"
                    }
                    if(serviceName == "salt-data-service") {
                        ecs_service_name = "sds"
                        serviceName = "sds"
                    }
                    def ecs_service = "arn:aws:ecs:us-east-1:${account_id}:service/ecs-${ecs_service_name}-service"
                    def CLUSTER = "ecs-cluster-nfn-internal-env-private"
                    def taskDefinition = sh (script: "aws ecs describe-services --region us-east-1 --services ${ecs_service} --cluster ${CLUSTER} --query 'services[*].taskDefinition' --output text",label: "Read Task Definition for ${ecs_service_name} service from ${params.environment}",returnStdout: true).trim()
                    def taskEnvironments = sh (script: "aws ecs describe-task-definition --task-definition ${taskDefinition}  --region us-east-1 --query 'taskDefinition.containerDefinitions[*].environment'",label: "Read Environment variables for ${ecs_service_name} service from ${params.environment}",returnStdout: true).trim()
                    def taskSecrets = sh (script: "aws ecs describe-task-definition --task-definition ${taskDefinition}  --region us-east-1 --query 'taskDefinition.containerDefinitions[*].secrets'",label: "Read Secrets for ${ecs_service_name} service from ${params.environment}",returnStdout: true).trim()
                    def imageString = sh (script: "aws ecs describe-task-definition --task-definition ${taskDefinition}  --region us-east-1 --query 'taskDefinition.containerDefinitions[*].image' --output text",label: "Read Image for ${ecs_service_name} service from ${params.environment}",returnStdout: true).trim()
                    def imageStr = imageString.split(":")[1]
                    def finalEKSTasks = [], finalEKSSecrets = []

                    def taskVariablesJSON = readJSON text: taskEnvironments
                    def taskSecretsJSON = readJSON text: taskSecrets
                    for( int i=0; i < taskVariablesJSON[0].size(); i++) {
                        def res = [:]
                        def name = taskVariablesJSON[0][i]["name"]
                        def value = taskVariablesJSON[0][i]["value"]
                        res["name"] = name
                        if(name.contains("AUDIENCE")) {
                            res["value"] = value
                        } else {
                            if(serviceName == "auth"){
                                res["value"] = transformations(parameters_yaml, "authorization",name,value, NAMESPACE, environment)
                            } else {
                                res["value"] = transformations(parameters_yaml, serviceName,name,value, NAMESPACE, environment)
                            }
                        }

                        if(name == "IO_NETFOUNDRY_AWSRESOURCE_WORKFLOWTASKS_SQSQUEUENAME"){
                            res["value"] = "${NAMESPACE}"
                        }

                        if(name == "REG_SERVICE_API" || name == "IO_NETFOUNDRY_REGISTRATION_SERVICEURL") {
                            res["value"] = "https://registration-${NAMESPACE}.${environment}.netfoundry.io:18443/registration"
                        }


                        if(res["value"] != "null") {
                            finalEKSTasks.add(res)
                        }
                    }
                    // Secrets
                    def resx = [:]
                    
                    resx = [:]
                    resx["name"]="RANDFILE"
                    resx["value"]=".rnd"
                    finalEKSTasks.add(resx)

                    for( String str: db_env_replacements) {
                        resx = [:]
                        resx["name"]="${str}"
                        resx["value"]="DockerRoot9876"
                        finalEKSTasks.add(resx)
                    }

                    if( taskSecretsJSON.size() > 0) {
                        for( int i=0; i < taskSecretsJSON[0].size(); i++) {
                            def res1 = [:]
                            def name1 = taskSecretsJSON[0][i]["name"]
                            def value1 = taskSecretsJSON[0][i]["valueFrom"]

                            //rabbitmq hack
                            value1 = value1.replaceAll('arn:aws:secretsmanager:us-east-1:745435643501:secret:central_rabbitmq_password-30qxFs','arn:aws:secretsmanager:us-east-1:745435643501:secret:eks_rabbitmq_password-Uhmm4K')
                            value1 = value1.replaceAll('arn:aws:secretsmanager:us-east-1:560269805515:secret:central_rabbitmq_password-GqA18B','arn:aws:secretsmanager:us-east-1:560269805515:secret:eks_rabbitmq_password-6C7nXx')

                            if( ! db_env_replacements.contains(name1)) {
                                def secret2 = getAWSSecretString "${value1}"
                                String[] value1Arr = value1.split(':')
                                String secretNameStr
                                if (value1Arr.size() > 7) {
                                    secretNameStr = value1Arr[6] + '-' + value1Arr[7]
                                } else {
                                    secretNameStr = value1Arr[6]
                                }
                                String secretName = secretNameStr.toLowerCase().replaceAll("_","-")
                                // Save Secret
                                def CREATE_K8_SECRET= "kubectl create secret generic ${serviceName}-${secretName} --from-literal=secret='${secret2}' --namespace ${NAMESPACE}"
                                writeFile file: "create_secret_${serviceName}-${secretName}_${currentBuildNumber}.sh", text: "${CREATE_K8_SECRET}"
                                try {
                                    labelledShell label: "Creating secret ${serviceName}-${secretName} in EKS", script: """
                                        bash create_secret_${serviceName}-${secretName}_${currentBuildNumber}.sh
                                    """
                                } catch(e) {
                                    echo "${e}"
                                }
                                res1["name"] = name1
                                res1["value"] = "${serviceName}-${secretName}"
                                finalEKSSecrets.add(res1)
                                labelledShell label: "Cleanup. Removing file create_secret_${serviceName}-${secretName}_${currentBuildNumber}.sh", script: """
                                    rm -f create_secret_${serviceName}-${secretName}_${currentBuildNumber}.sh
                                """
                            }                   
                        }
                    }

                    


                    def ports = sh (script: "aws ecs describe-task-definition --task-definition ${taskDefinition} --region us-east-1 --query 'taskDefinition.containerDefinitions[*].portMappings'",returnStdout: true).trim()
                    def portsJSON = readJSON text: ports

                    // Health Check
                    def healthCheck = [:]
                    healthCheck["HealthCheckPath"] = parameters_yaml["Services"]["${arrVal}"]["healthcheckpath"]
                    healthCheck["HealthCheckProtocol"] = "HTTP"
                    healthCheck["Port"] = portsJSON[0][0]["containerPort"]

                    

                    //ImageName
                    def image = "netfoundry/${serviceName}:${imageStr}"
                    
                    // Add Flag to check if a failure can lead to a retry
                    def retriable_service = true

                    if(REPO == serviceName) {
                        image = "netfoundry/${serviceName}:${commit_hash}"
                        retriable_service = false
                    }

                    if(REPO == "network") {
                        if(serviceName == "edge" || serviceName == "engine" || serviceName == "infra" || serviceName == "data") {
                            image = "netfoundry/${serviceName}:${commit_hash}"
                            retriable_service = false
                        }
                    }

                    def data = [:]

                    if(serviceName == "gateway") {
                        data["alb"] = [:]
                        data["alb"]["hostname"] = "gateway-${NAMESPACE}.${environment}.netfoundry.io"
                        image = "netfoundry/api-gateway:${imageStr}"
                    }

                    if(serviceName == "registration") {
                        data["alb"] = [:]
                        data["alb"]["hostname"] = "registration-${NAMESPACE}.${environment}.netfoundry.io"
                    }

                    if(REPO == "api-gateway" && serviceName == "gateway") {
                        image = "netfoundry/api-gateway:${commit_hash}"
                        retriable_service = false
                    }

                    if(REPO == "authorization" && serviceName == "auth") {
                        image = "netfoundry/auth:${commit_hash}"
                        retriable_service = false
                    }


                    data["service"] = "${serviceName}-service"
                    data["namespace"] =  "${NAMESPACE}"
                    data["limit"] = [:]
                    data["limit"]["memory"] =  "1536Mi"
                    data["limit"]["cpu"] =  "512m"
                    data["image"] =  "${image}"
                    data["ports"] = portsJSON[0]
                    data["healthcheck"] = healthCheck
                    data["loadBalanceType"] = "ClusterIP"
                    data["alb_security"] = alb_security["${environment}"]
                    data["cert_arns"] = cert_arns["${environment}"]
                    if(serviceName == "registration" || serviceName == "gateway") {
                        data["loadBalanceType"] = "NodePort"
                    }
                    data["environment"] = finalEKSTasks
                    data["secrets"] = finalEKSSecrets
                    dir("nf-mop-kubernetes") {
                        def filename = "${serviceName}-${NAMESPACE}.yaml"
                        sh script: "rm -f ${filename}", label: "Cleanout any previous running versions of ${filename}"
                        writeYaml file: filename, data: data
                        labelledShell label: "Helm template file with contents", script: """
                            cat ${filename}
                        """
                        def helm_release = "${serviceName}-${NAMESPACE}"
                        if(helm_release.size() > 50) {
                            helm_release = "${serviceName}-${NAMESPACE}".substring(0,50)+"a"
                        }

                        try {
                            labelledShell label: "Delete any previous running versions of ${ecs_service_name} container", script: """
                                helm delete ${helm_release} --namespace=${NAMESPACE}|| true
                            """
                        } catch (e) {
                            echo "${serviceName}-${NAMESPACE} release doesnt exist"
                        }

                        try {
                            labelledShell label: "Create ${ecs_service_name} container", script: """
                                helm install ${helm_release} generic-service --namespace ${NAMESPACE} --wait --values ${filename} --set namespace=${NAMESPACE} --timeout ${HELM_TIMEOUT} -v=${helm_verbose_level}
                            """
                        } catch(e) {
                            def datacontainer=sh (script: "kubectl get pods --namespace=${NAMESPACE} | grep ${serviceName}-service | cut -d' ' -f1",returnStdout: true).trim()
                            try {
                                labelledShell label: "Container ${ecs_service_name} deployment failed. Expand to view logs", script: """
                                    kubectl logs ${datacontainer} --namespace=${NAMESPACE} | tail -5 
                                    exit 1
                                """
                            }catch(ex_logs) {}
                            set_kibana_url = true
                            set_affected_service = true
                            affected_service = serviceName
                            def startDate = dateMath minutes: 30
                            def start_date = startDate.format( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" )
                            def end_date = new Date().format( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" )
                            affected_service = "${serviceName}"
                            def elastic_index = es_index(environment)
                            def svcDescription = "kubernetes.pod.name:%20%22${serviceName}-service%22%20"
                            kibana_url = "https://bf915c25292eaced6cd2cca6ab8cd77f.us-east-1.aws.found.io:9243/app/kibana#/discover?_g=(refreshInterval:(pause:!t,value:0),time:(from:'${start_date}',mode:absolute,to:'${end_date}'))&_a=(columns:!(kubernetes.pod.name,message,stack_trace),filters:!(),index:'${elastic_index}',interval:auto,query:(language:lucene,query:'${svcDescription}AND%20kubernetes.namespace:%20%22${NAMESPACE}%22%20AND%20ERROR'),sort:!('@timestamp',desc))" 
                            er = e
                            return [set_affected_service, set_kibana_url, affected_service, kibana_url, deploy_gateway, er]
                        }
                    }
                }
            }
            parallel buildServices
        }
    }
    return [set_affected_service, set_kibana_url, affected_service, kibana_url, deploy_gateway, er]
}