
def call(repo, namespace, environment, DESCRIPTION, service_changeset, database_changeset, killed_running_job, currentBuild) {
    // Check if there is another PR currently running, kill old job and setup new job
    currentBuildNumber = currentBuild.getNumber()
    def stopped_job = false
    try {
        Jenkins.instance.getView('Continous_Deployment').getBuilds().findAll() { 
            def jobResult = it.getResult()
            def jobDescription = it.description
            def jobNumber = it.getNumber()
            if(jobResult.equals(null) ) {
                if(jobDescription == DESCRIPTION && jobNumber != currentBuildNumber && jobNumber.toInteger() < currentBuildNumber.toInteger() ) {
                    echo "I am waiting for old job to close"
                    echo "${it}"
                    it.doKill()
                    stopped_job = true
                    killed_running_job = true
                }
            }
        }
    } catch(exmp) {
        echo "Job Killed, exception on next iteration. Exception ${exmp}"
    }

    // Cleanup
    service_rollback(namespace, environment, killed_running_job)
    
    // Check that job has stopped before proceeding.
    def stopped_job_check = false 
    while(stopped_job_check == false) {
        stopped_job_check = true 
        Jenkins.instance.getView('Continous_Deployment').getBuilds().findAll() {
            try {
                def jobResult = it.getResult()
                def jobDescription = it.description
                def jobNumber = it.getNumber()
                if(jobResult.equals(null) ) {
                    if(jobDescription == DESCRIPTION && jobNumber != currentBuildNumber) {
                        echo "I am waiting for old job to close"
                        stopped_job_check = false
                    }
                }
            } catch(exmp){
                echo "Cleanup not perfect, but moving on. Exception ${exmp}"
            }
            
        }
        if( stopped_job_check == false ) {
            sleep 5
        }
    }
                        
    // Service & Database Changeset

    def parameters_yaml = readYaml file: "pipelines/continous_deployment/conf/services.yaml"
    def yamlServices = parameters_yaml["Services"]
    smoke_test = []


    yamlServices.each { key, value ->
        if(key == repo){
            if(service_changeset[value.order] == null) { service_changeset[value.order] = [] }
            service_changeset[value.order].add(key)
            if(value["database"] != null) {
                database_changeset.add(value.database)
            }
            smoke_test = value.smoke_test
        }
    }

    

    if(repo == "api-gateway" ) {
        smoke_test = yamlServices["gateway"]["smoke_test"]
    }

    if(smoke_test.size() > 0 ) {
        if(smoke_test[0] == null ) {
            smoke_test = []
        } 
    }


    yamlServices.each { key, value ->
        if(((value.smoke_test.intersect(smoke_test)).size() > 0 && key != repo) || (value.order < 10 && key != repo)) {
            if(service_changeset[value.order] == null) { service_changeset[value.order] = [] }
            service_changeset[value.order].add(key)
            if(value["database"] != null) {
                database_changeset.add(value.database)
            }
        }
    }

   

    if( smoke_test.size() > 0 ) {
        if(smoke_test[0] == null) {
            smoke_test = []
        }
    }

    service_changeset.remove(1)
    if ( repo != "network" ) {
        def modifiedREPO = REPO
        if( REPO == "api-gateway"){ modifiedREPO = "gateway" }
        if(yamlServices[modifiedREPO]["migrated_tests"] == true) {
            service_changeset = [:]
            database_changeset = ["nfapidb", "identitydb", "authdb"]
            def index = 0
            service_changeset[0] = ["data"]
            service_changeset[1] = ["authorization"]
            service_changeset[2] = ["identity"]
            index = 3
            if(modifiedREPO != "authorization" && modifiedREPO != "identity"){
                service_changeset[index] = [modifiedREPO]
                index = index + 1
            }

            service_changeset[index] = []
            
            if(yamlServices[modifiedREPO]["database"] != null) {
                database_changeset.add(yamlServices[modifiedREPO]["database"])
            }


            for ( dependency in yamlServices[modifiedREPO]["dependencies"] ) {
                service_changeset[index].add(dependency)
                if(yamlServices[dependency]["database"] != null) {
                    database_changeset.add(yamlServices[dependency]["database"])
                }
            }
            smoke_test = ["repo"]
        }
    } else {
        service_changeset = [:]
        service_changeset[0] = ["data"]
        service_changeset[1] = ["authorization"]
        service_changeset[2] = ["identity"]
        service_changeset[3] = ["certificate", "gateway", "infra", "engine", "edge", "registration", "core-management", "billing"]
        database_changeset = ["nfapidb", "identitydb", "authdb", "core-managementdb", "enginedb", "billingdb"]
        smoke_test = ["repo"]
    }
    
    labelledShell label: "Click to view Services and Databases to be built", script: """
        echo 'Services to be built: ${service_changeset}'
        echo 'Databases to be built: ${database_changeset}'
    """

    return [service_changeset, database_changeset, killed_running_job]
}