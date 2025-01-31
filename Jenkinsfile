/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

node(label: 'ubuntu') {
    if (env.CHANGE_ID) {
        properties([
            pipelineTriggers([
                issueCommentTrigger('.*test this please.*')
            ])
        ])
    }

    catchError {
        def environmentDockerImage

        def dockerTag = env.BUILD_TAG.replace('%2F', '-')

        withEnv(["DOCKER_TAG=${dockerTag}"]) {
            stage('Clone repository') {
                checkout scm
            }

            stage('Prepare environment') {
                echo 'Creating maven cache ...'
                sh 'mkdir -p ${WORKSPACE}/.m2'
                sh 'docker logout'   // needed because sometimes cached credentials break the following step (creds aren't needed)
                echo 'Building docker image for test environment ...'
                environmentDockerImage = docker.build('brooklyn:${DOCKER_TAG}')
            }

            stage('Run tests') {
                environmentDockerImage.inside('-i --name brooklyn-${DOCKER_TAG} -v ${WORKSPACE}/.m2:/var/maven/.m2 --mount type=bind,source="${HOME}/.m2/settings.xml",target=/var/maven/.m2/settings.xml,readonly -v ${WORKSPACE}:/usr/build -w /usr/build') {
                    sh 'mvn clean install -Duser.home=/var/maven -Duser.name=jenkins'
                }
            }
        }
    }

    // ---- Post actions steps, to always perform ----

    stage('Publish test results') {
        // Publish JUnit results
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/junitreports/*.xml'

        // Publish TestNG results
        step([
            $class: 'Publisher',
            reportFilenamePattern: '**/testng-results.xml'
        ])
    }

    // Conditional stage, when not building a PR
    if (env.CHANGE_ID == null) {
        stage('Send notifications') {
            // Send email notifications
            step([
                $class: 'Mailer',
                notifyEveryUnstableBuild: true,
                recipients: 'dev@brooklyn.apache.org',
                sendToIndividuals: false
            ])
        }
    }
}
