pipeline {
    agent none
    options {
        skipDefaultCheckout()
    }
    environment {
        // jdeps can't see these: jdk.crypto.ec is needed for HTTPS rate fetches (TLS handshakes fail without it),
        // jdk.localedata for number formats in non-English locales.
        EXTRA_MODULES = 'jdk.crypto.ec,jdk.localedata'
        GATE_DIR = '/home/sounak/jenkins/release-gate/indiangold'
    }
    stages {
        stage('Build jar') {
            agent { label 'lin' }
            steps {
                cleanWs()
                script {
                    def scmVars = checkout scm
                    // Release gate: 'indiangold dev' must have passed on a commit with exactly these files (same git tree).
                    def tree = sh(returnStdout: true, script: 'git rev-parse "HEAD^{tree}"').trim()
                    if (sh(returnStatus: true, script: "test -f '${env.GATE_DIR}/${tree}'") != 0) {
                        error("Release blocked: commit ${scmVars.GIT_COMMIT.substring(0, 8)} (tree ${tree.substring(0, 12)}) has no passing 'indiangold dev' run. " +
                              "Check out this commit with no uncommitted changes, build it in the IDE so 'indiangold dev' passes, then run the release again.")
                    }
                    echo "Release gate passed:"
                    sh "cat '${env.GATE_DIR}/${tree}'"
                    env.APP_VERSION = sh(returnStdout: true, script: 'mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout').trim().replace('-SNAPSHOT', '')
                    if (!(env.APP_VERSION ==~ /\d+(\.\d+){0,2}/)) {
                        error("pom.xml version '${env.APP_VERSION}' is not a valid installer version; use e.g. 1.2 or 1.2.3")
                    }
                    currentBuild.description = "v${env.APP_VERSION} @ ${scmVars.GIT_COMMIT.substring(0, 8)}"
                }
                sh 'mvn -B -ntp clean verify'
                sh '''
                    mkdir -p app
                    cp target/indiangold.jar app/
                    cp LICENSE.md app/LICENSE.txt
                '''
                stash name: 'app', includes: 'app/**'
                stash name: 'icons', includes: 'extras/IndianGold.ico,extras/IndianGold.png,extras/IndianGold.icns,extras/linux/**'
                archiveArtifacts artifacts: 'app/indiangold.jar', fingerprint: true
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Package') {
            parallel {
                stage('Windows') {
                    agent { label 'win' }
                    stages {
                        stage('Windows: Creating minimal JRE') {
                            steps {
                                cleanWs()
                                unstash 'app'
                                unstash 'icons'
                                script {
                                    def deps = bat(returnStdout: true, script: '@echo off && "%JAVA_HOME%\\bin\\jdeps" --multi-release 21 --ignore-missing-deps --print-module-deps app\\indiangold.jar').trim()
                                    echo "Dependencies: '${deps}' + ${env.EXTRA_MODULES}"
                                    withEnv(["DEPENDS=${deps},${env.EXTRA_MODULES}"]) {
                                        bat '@echo off && "%JAVA_HOME%\\bin\\jlink" --compress=zip-6 --strip-debug --no-header-files --no-man-pages --add-modules "%DEPENDS%" --output jre'
                                    }
                                }
                            }
                        }
                        stage('Pack Windows Installer') {
                            steps {
                                script {
                                    fileOperations([
                                        fileDownloadOperation(password: '', proxyHost: '', proxyPort: '', targetFileName: 'wix314-binaries.zip', targetLocation: '', url: 'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip', userName: ''),
                                        fileUnZipOperation(filePath: 'wix314-binaries.zip', targetLocation: 'wix')
                                    ])
                                    def wix = pwd() + '\\wix'
                                    withEnv(["PATH+WIX=${wix}"]) {
                                        bat '@echo off && "%JAVA_HOME%\\bin\\jpackage" --input app --name IndianGold --description "Weight and price calculator for Indian units (ratti, tola, bhori) with metal market rates" --vendor "Sounak Choudhury" --copyright "Copyright (C) 2012-2026 Sounak Choudhury" --app-version %APP_VERSION% --main-jar indiangold.jar --runtime-image jre --type msi --license-file app\\LICENSE.txt --icon extras\\IndianGold.ico --win-dir-chooser --win-menu --win-menu-group IndianGold --win-shortcut'
                                    }
                                }
                            }
                        }
                        stage('Export MSI') {
                            steps {
                                archiveArtifacts artifacts: '*.msi', followSymlinks: false
                            }
                        }
                    }
                }
                stage('Ubuntu') {
                    agent { label 'lin' }
                    stages {
                        stage('Ubuntu: Creating minimal JRE') {
                            steps {
                                cleanWs()
                                unstash 'app'
                                unstash 'icons'
                                sh 'jlink --compress=zip-6 --strip-debug --no-header-files --no-man-pages --add-modules "$(jdeps --multi-release 21 --ignore-missing-deps --print-module-deps app/indiangold.jar),$EXTRA_MODULES" --output jre'
                            }
                        }
                        stage('Pack Debian Package') {
                            steps {
                                sh 'jpackage --input app --name IndianGold --description "Weight and price calculator for Indian units (ratti, tola, bhori) with metal market rates" --vendor "Sounak Choudhury" --copyright "Copyright (C) 2012-2026 Sounak Choudhury" --app-version "$APP_VERSION" --main-jar indiangold.jar --runtime-image jre --type deb --license-file app/LICENSE.txt --icon extras/IndianGold.png --linux-app-category utils --linux-app-release release --linux-menu-group "Utility;Calculator" --linux-shortcut --resource-dir extras/linux --java-options "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"'
                            }
                        }
                        stage('Export DEB') {
                            steps {
                                archiveArtifacts artifacts: '*.deb', followSymlinks: false
                            }
                        }
                    }
                }
                stage('Mac OS') {
                    agent { label 'mac' }
                    stages {
                        stage('MacOS: Creating minimal JRE') {
                            steps {
                                cleanWs()
                                unstash 'app'
                                unstash 'icons'
                                sh 'jlink --compress=zip-6 --strip-debug --no-header-files --no-man-pages --add-modules "$(jdeps --multi-release 21 --ignore-missing-deps --print-module-deps app/indiangold.jar),$EXTRA_MODULES" --output jre'
                            }
                        }
                        stage('Pack DMG Image') {
                            steps {
                                sh 'jpackage --input app --name IndianGold --description "Weight and price calculator for Indian units (ratti, tola, bhori) with metal market rates" --vendor "Sounak Choudhury" --copyright "Copyright (C) 2012-2026 Sounak Choudhury" --app-version "$APP_VERSION" --main-jar indiangold.jar --runtime-image jre --type dmg --license-file app/LICENSE.txt --icon extras/IndianGold.icns --mac-app-category finance --mac-package-name IndianGold'
                            }
                        }
                        stage('Export DMG Image') {
                            steps {
                                archiveArtifacts artifacts: '*.dmg', followSymlinks: false
                            }
                        }
                    }
                }
            }
        }
    }
}
