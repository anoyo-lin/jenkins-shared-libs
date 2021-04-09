## implementing Guide

# gitlab
for gitlab we can extract the gitlab.rb from the running container in /etc/gitlab/gitlab.rb
then we can remove unwanted services by set the Omnibus of the gitlab
- remove the promethus and conterpart services.
- decrease the performance of postgreSQL and puma (web server) of the gitlab
- tweak the nginx configuration, and decrease the worker to 1
and we need to add the public key to the jar of the gitlab when finished the initialization

# jenkins

for jenkins we need to install sudo and add the devops account firstly, then
and adding the docker binary and chmod the docker.sock to let the jenkins has capability of
Docker in Docker
- changing the MAVEN_CONFIG = /app/maven/.m2 in executor's envVars
- changing the git repo url to the docker internal IP ones
- downloading the devops-ci-image from the global artifactory, update  known_hosts 
- update the JENKINS_OPTS, to support the url prefix, and awt headless, or anyother JAVA options and Jenkins options
- in jenkins please change the shared libs & project repo's url to gitlab internal IP's.

# jenkins optimization

- GC troubleshooting, upload the GC file by set special JAVA_OPTS, and upload them to **gceasy.io**, [jenkins memory tweak](https://cloudbees.com/blog/joining-big-leagues-tuning-jenkins-gc-responsiveness-and-stability)
- threaddump by jstack,  use nid & hex (0xhex) of PID as match pattern to search the problemic thread in java process. [threaddump troubleshooting](identify-java-code-consuming-high-cpu-in-linux-linking-jvm-thread-and-linux-pid)
- understanding the queue mechanism in jenkins, [queue](https://javadoc.jenkins-ci.org/hudson/model/Queue.html), [item](https://javadoc.jenkins-ci.org/hudson/model/Queue.item.html)
- javaMelody, please install the jenkins monitor plugin to do the further investigation
- debugging JVM application
- java performance: the definitive guide
- split the jenkins master from static one, to the serverless jenkins in the jenkins X, by prow or any other cloud native structure.
- for branch indexing, if your branch has new revisions in remote. when you scan the multibranch project , it will fetch the latest change and do the automatical build across all filtered branches in your multibranch job
* we can disable this by set the skip inital build when branch indexing
or inject the code before the pipeline{} block 
```
if (configuration.skipBranchIndexing == null || configuration.skipBranchIndexing == true) {
    print currentBuild.rawBuild.getCause().toString()
    if ( currentBuild.rawBuild.getCause().toString().contains('BranchIndexingCause')) {
        print "INFO: Build skipped due to trigger being Branch Indexing"
        currentBuild.result = "ABORTED"
        return
    }
}
```