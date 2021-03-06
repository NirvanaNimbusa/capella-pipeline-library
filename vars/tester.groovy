private def downloadJacocoIfNeeded() {
    def jacoURL = 'https://repo1.maven.org/maven2/org/jacoco/jacoco/0.8.6/jacoco-0.8.6.zip'
    def jacoZip = 'jacoco.zip'
    sh """
		if [ ! -d ${WORKSPACE}/jacoco ]; then
			curl -k -o ${jacoZip} ${jacoURL}
			unzip ${jacoZip} -d ${WORKSPACE}/jacoco
			echo "Jacoco downloaded at ${WORKSPACE}/jacoco" 
		fi
    """
	sh "ls ${WORKSPACE}/jacoco/lib"
}

def generateJacocoReport() {
    sh "mvn -Djacoco.dataFile=${WORKSPACE}/jacoco.exec org.jacoco:jacoco-maven-plugin:0.8.6:report"
}

def publishTests() {
	junit '*.xml'
	generateJacocoReport()
}
	        		
private def getRunnerCmd(String capellaProductPath) {
  return "${capellaProductPath} " +
      "-port 8081 " +
      "-application org.polarsys.capella.test.run.application " +
      "-data ${WORKSPACE}/runner >> ${WORKSPACE}/runner.log"
}

private def getJunitCmdTemplate(String capellaProductPath, String applicationParam) {
  
  // extract the capella path, without the executable name
  def capellaPath = capellaProductPath.substring(0, capellaProductPath.lastIndexOf("/"))
  def jacocoParameters = "\"-javaagent:${WORKSPACE}/jacoco/lib/jacocoagent.jar=includes=*,excludes=,exclclassloader=sun.reflect.DelegatingClassLoader,destfile=${WORKSPACE}/jacoco.exec,output=file,append=true\" "
  
  return "sleep 10 && " +
    "java " +
      "${jacocoParameters}" +
      "-Xms1024m -Xmx3500m -XX:+CMSClassUnloadingEnabled -ea " +
      "-Declipse.p2.data.area=@config.dir/../p2 " +
      "-Dfile.encoding=Cp1252 " +
      "-classpath ${capellaPath}/plugins/org.eclipse.equinox.launcher_*.jar org.eclipse.equinox.launcher.Main " +
      "-os linux " +
      "-ws gtk " +
      "-arch x86_64 " +
      "-version 3 " +
      "-port 8081 " +
      "-testLoaderClass org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader " +
      "-loaderpluginname org.eclipse.jdt.junit4.runtime " +
      "-application ${applicationParam} " +
      "-product org.polarsys.capella.rcp.product " +
      "-testApplication org.polarsys.capella.core.platform.sirius.ui.perspective.id " +
      "-configuration file:${capellaPath}/configuration " +
      "-buildKey ${BUILD_KEY}"
}


private def getUIJunitCmd(String capellaProductPath) {
	return getJunitCmdTemplate(capellaProductPath, 'org.eclipse.pde.junit.runtime.uitestapplication')
}

private def getNONUIJunitCmd(String capellaProductPath) {
  return getJunitCmdTemplate(capellaProductPath, 'org.eclipse.pde.junit.runtime.nonuithreadtestapplication')
}

def runUITests(String capellaProductPath, String suiteTitle, String testPluginName, List<String> testClassNames) {
    downloadJacocoIfNeeded()
    def runnerCmd = getRunnerCmd(capellaProductPath)
    def junitCmd = getUIJunitCmd(capellaProductPath)
    def testClassNamesParam = testClassNames.join(' ')
    
    sh "${runnerCmd} -title ${suiteTitle} & ${junitCmd} -data ${WORKSPACE}/${suiteTitle} -testpluginname ${testPluginName} -classNames ${testClassNamesParam}"
}

def runNONUITests(String capellaProductPath, String suiteTitle, String testPluginName, List<String> testClassNames) {
    downloadJacocoIfNeeded()
    def runnerCmd = getRunnerCmd(capellaProductPath)
    def junitCmd = getNONUIJunitCmd(capellaProductPath)
    def testClassNamesParam = testClassNames.join(' ')
    
    sh "${runnerCmd} -title ${suiteTitle} & ${junitCmd} -data ${WORKSPACE}/${suiteTitle} -testpluginname ${testPluginName} -classNames ${testClassNamesParam}"
}