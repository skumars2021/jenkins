def call(repo){
    MOP_LIBRARIES= "OSS|common-util-framework|tracing-framework|elasticsearch-client|ziti-api-client|dart-api-client|authorization"
    def library_list = MOP_LIBRARIES
    // if(REPO == "authorization") {      //unsure why authorization has this
    //     library_list = MOP_LIBRARIES.minus("|authorization")
    // }
    def dep_result = false
    def errors = sh ( script: "mvn dependency:tree -U -Dincludes=io.netfoundry | egrep '${library_list}' | grep SNAPSHOT | grep -v 'Downloading' | grep -v 'Downloaded' | wc -l ",label: "Check if snapshots are being referenced",returnStdout: true).trim()
    echo "errors: ${errors}"
    if("${errors.replaceAll(' ','')}" != "0"){
        dependency_list = sh (script: "mvn dependency:tree -U -Dincludes=io.netfoundry | egrep '${library_list}' | grep SNAPSHOT | grep -v 'Downloading' | grep -v 'Downloaded'",label: "Check if snapshots are being referenced", returnStdout: true).trim()
        echo "dependency_list: ${dependency_list}"
        for( String line: dependency_list.split("\n")){
            def count_lines = sh ( script: "grep ${line.split(":")[1]} pom.xml | wc -l", returnStdout: true ).trim()
            if ( "${count_lines}" != "0") {
                dep_result = true
            } 
        }
    }

    if(dep_result == true) {
        error "Service: ${svc_name} -   You are referencing MOP Snapshots in your POM files. Only Release versions of io.netfoundry can be referenced.\n${dependency_list}."+
            "\nView Latest version of releases from Artifactory - https://netfoundry.jfrog.io/ui/repos/tree/General/mop-local-releases."+
            "\nTo promote a snapshot to a release, use the Jenkins pipeline - https://jenkins.tools.netfoundry.io/blue/organizations/jenkins/MOP%20Snapshot%20Promotions/activity"+
            "\nOnce a snapshot is promoted, it cannot be updated again."
    }
}