def call(MAVEN_DIRECTORY, JAVA_HOME){
        // START OF SHARED FUNCTION
        MOP_LIBRARIES= "OSS|common-util-framework|tracing-framework|elasticsearch-client|ziti-api-client|dart-api-client|authorization"
        // checkAllowedServices()
        //Used by Sonar, donot delete artifact dir
        dir("${MAVEN_DIRECTORY}"){
            // check_maven_dependencies("${REPO}")
            def dep_result = false
            def errors = sh ( script: "mvn dependency:tree -U -Dincludes=io.netfoundry | egrep '${MOP_LIBRARIES}' | grep SNAPSHOT | grep -v 'Downloading' | grep -v 'Downloaded' | wc -l ",label: "Check if snapshots are being referenced",returnStdout: true).trim()
            echo "errors: ${errors}"
            if("${errors.replaceAll(' ','')}" != "0"){
                dependency_list = sh (script: "mvn dependency:tree -U -Dincludes=io.netfoundry | egrep '${MOP_LIBRARIES}' | grep SNAPSHOT | grep -v 'Downloading' | grep -v 'Downloaded'",label: "Check if snapshots are being referenced", returnStdout: true).trim()
                echo "dependency_list: ${dependency_list}"
                for( String line: dependency_list.split("\n")){
                    def count_lines = sh ( script: "grep ${line.split(":")[1]} pom.xml | wc -l", returnStdout: true ).trim()
                    if ( "${count_lines}" != "0") {
                        dep_result = true
                    } 
                }
            }

            if(dep_result == true) {
                error "You are referencing MOP Snapshots in your POM files. Only Release versions of io.netfoundry can be referenced.\n${dependency_list}."+
                    "\nView Latest version of releases from Artifactory - https://netfoundry.jfrog.io/ui/repos/tree/General/mop-local-releases."+
                    "\nTo promote a snapshot to a release, use the Jenkins pipeline - https://jenkins.tools.netfoundry.io/blue/organizations/jenkins/MOP%20Snapshot%20Promotions/activity"+
                    "\nOnce a snapshot is promoted, it cannot be updated again."
            }
        // }

        }


        // build_maven("${MAVEN_DIRECTORY}", "${JAVA_HOME}", "${HOME}")
            labelledShell label: 'Creating Maven Folder', script: """
                mkdir -p ${MAVEN_DIRECTORY}/maven_local
            """
            dir("${MAVEN_DIRECTORY}/maven_local"){
                labelledShell label: 'Copying over .m2 repository folders', script: """
                    for dir in \$(ls \$HOME/.m2/repository/ | grep -v 'io'); do ln -s \$HOME/.m2/repository/\$dir \$dir; done
                """
            }
        labelledShell label: 'maven build', script: """
            export JAVA_HOME=${JAVA_HOME}; mvn -U -Dmaven.repo.local=${MAVEN_DIRECTORY}/maven_local  clean install
            rm -rf ${MAVEN_DIRECTORY}
        """
}