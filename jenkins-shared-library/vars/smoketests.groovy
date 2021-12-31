/* Author: Albert Mugisha
* Used for Smoketests
*/



def call( params ) {
    def traffic_tests = params.traffic_tests ?: "true"
    def api_tests = params.traffic_tests ?: "true"
    def environment = params.environment ?: "sandbox"

    def tests = [:]
    if( traffic_tests == "true") {
        tests["Traffic Test"] = { ->
            build job: 'Smoke Test - On Demand', parameters: [
                [$class: 'StringParameterValue', name: 'network', value: "TestNW-Depot-${new Date().format( 'yyyyMMddHHmmSSS' )}"],
                [$class: 'StringParameterValue', name: 'dep_environment', value: environment],
                [$class: 'StringParameterValue', name: 'location', value: 'us-east-1'],
                [$class: 'StringParameterValue', name: 'autodelete', value: 'Yes']
            ] 
        }
    }
    if(api_tests == "true" ) {
        tests["API Test"] = { ->
            build job: 'Taurus_SmokeTest', parameters: [
                            [$class: 'StringParameterValue', name: 'network_name', value: "Dep-Tau-${new Date().format( 'yyyyMMddHHmmSSS' )}"],
                            [$class: 'StringParameterValue', name: 'environment', value: environment],
                            [$class: 'StringParameterValue', name: 'location', value: 'ca-central-1'],
                            [$class: 'StringParameterValue', name: 'timeout', value: '1500']
                        ], propagate: true, wait: true
        }
    }
    
    parallel tests
}