//def folder_name = "cartridge_folder"



freeStyleJob("act1") {

    // general
    properties {
        copyArtifactPermissionProperty {
        projectNames('act3')
        }
    }  

    // source code management  
    scm {
        git {
            remote {
                url('git@gitlab:Fermin/CurrencyConverterDTS.git')
                credentials('efe11ee5-d565-4868-8f10-fc61574bc868')
            }
        }
    }

    // build triggers
     triggers {
        gitlabPush {
            buildOnMergeRequestEvents(true)
            buildOnPushEvents(true)
            enableCiSkip(false)
            setBuildDescription(false)
            rebuildOpenMergeRequest('never')
        }
    }
    wrappers {
        preBuildCleanup()
    }
  
    // build
    steps {
        maven{
            mavenInstallation('ADOP Maven')
            goals('package')
        }
    }

    // post build actions
     publishers {
        archiveArtifacts {
            pattern('**/*.war')
            onlyIfSuccessful()
        }
        downstream('act2', 'UNSTABLE')
    }
}

freeStyleJob("act2") {
    // source code management  
    scm {
        git {
            remote {
                url('git@gitlab:Fermin/CurrencyConverterDTS.git')
                credentials('efe11ee5-d565-4868-8f10-fc61574bc868')
            }
        }
    }

    // build
    configure { project ->
        project / 'builders' / 'hudson.plugins.sonar.SonarRunnerBuilder' {
        properties('''
            sonar.projectKey=SonarActivityTest
            sonar.projectName=simulationActivity
            sonar.projectVersion=1.0
            sonar.sources=.
        ''')
        javaOpts()
        jdk('(Inherit From Job)')
        task()
        }
    }    

  //post build actions
  publishers {
        downstream('act3', 'UNSTABLE')
    }

}

freeStyleJob("act3") {

  // build copy artifacts

steps {
      copyArtifacts('act1') {
            includePatterns('target/*.war')
          
            fingerprintArtifacts()
        }
      nexusArtifactUploader {
        nexusVersion('NEXUS2')
        protocol('http')
        nexusUrl('nexus:8081/nexus')
        groupId('DTSActivity')
        version('1')
        repository('snapshots')
        credentialsId('efe11ee5-d565-4868-8f10-fc61574bc868')
        artifact {
            artifactId('CurrencyConverter')
            type('war')
            file('/var/jenkins_home/jobs/act3/workspace/target/CurrencyConverter.war')
        }
      }
    }
  
  // post build actions
     publishers {
        archiveArtifacts {
            pattern('**/*.war')
            onlyIfSuccessful()
        }
        downstream('act4', 'UNSTABLE')
    }
}

freeStyleJob("act4") {

  // label
  label('ansible')

  // source code management  
    scm {
        git {
            remote {
                url('http://13.57.75.40/gitlab/Fermin/Ansible.git')
                credentials('efe11ee5-d565-4868-8f10-fc61574bc868')
            }
        }
    }

// build environment + bindings
wrappers {
        sshAgent('adop-jenkins-master')
        credentialsBinding {
            usernamePassword('username', 'password', 'efe11ee5-d565-4868-8f10-fc61574bc868')
        }
    }

// build- execute shell

steps {
        shell('''ls -la
                ansible-playbook -i hosts master.yml -u ec2-user -e "image_version=$BUILD_NUMBER username=$username password=$password"''')
    }

// post build actions
publishers {
        downstream('act5', 'UNSTABLE')
    }
}

freeStyleJob("act5") {

  // source code management  
    scm {
        git {
            remote {
                url('http://13.57.75.40/gitlab/Fermin/SeleniumDTS.git')
                credentials('efe11ee5-d565-4868-8f10-fc61574bc868')
            }
        }
    }

    // build
    steps {
        maven{
            mavenInstallation('ADOP Maven')
            goals('test')
        }
    }
    // post build actions
     publishers {
        downstream('act6', 'UNSTABLE')
    }
}

freeStyleJob("act6") {

   // general
    properties {
        copyArtifactPermissionProperty {
        projectNames('act3')
        }
    }  

    // build
    steps {
        copyArtifacts('act3') {
            includePatterns('**/*.war')
            fingerprintArtifacts()
            buildSelector {
                latestSuccessful(true)
            }
        }
            
        nexusArtifactUploader {
            nexusVersion('NEXUS2')
            protocol('http')
            nexusUrl('nexus:8081/nexus')
            groupId('DTSActivity')
            version('${BUILD_NUMBER}')
            repository('releases')
            credentialsId('efe11ee5-d565-4868-8f10-fc61574bc868')
            artifact {
                artifactId('CurrencyConverter')
                type('war')
                file('target/CurrencyConverter.war')
        }
      }
    }

}

buildPipelineView("Activity_Cartridge") {
    filterBuildQueue()
    filterExecutors()
    title('Cartridge Pipeline')
    displayedBuilds(3)
    selectedJob('act1')
    alwaysAllowManualTrigger()
    showPipelineParameters()
    refreshFrequency(60)
}
