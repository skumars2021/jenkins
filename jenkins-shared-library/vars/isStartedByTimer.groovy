import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def call() {
    def buildCauses = currentBuild.rawBuild.getCauses()
    // echo buildCauses
    def stringBuildCauses = String.valueOf(buildCauses)
    echo "BuildCauses:"
    echo stringBuildCauses
    boolean isStartedByTimer = false
    if (stringBuildCauses.contains("hudson.triggers.TimerTrigger\$TimerTriggerCause")) {
        isStartedByTimer = true
    }

    return isStartedByTimer
}
