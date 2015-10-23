/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
/**
 * This script takes the pom.xml in the current directory, reads the from it dependencies
 * and outputs Gradle formatted dependencies.
 *
 * It currently is quite simplistic and does not perform any checks on inherited
 * dependencies or retreive version numbers from variables deplared in the pom.
 *
 */
 
// debug - print out our current working directory
println "Working path:" + new File(".").getAbsolutePath()
 
// read in the pom.xml file and get it�s text into a string
pomContent = new File("pom.xml").text
 
// use the Groovy XmlSlurper library to parse the text string and get a reference to the outer project element.
def project = new XmlSlurper().parseText(pomContent)
 
// use GPath to navigate the object hierarchy and retrieve the collection of dependency nodes.
def dependencies = project.dependencies.dependency
 
def compileTimeScope = []
def runTimeScope = []
def testScope = []
def providedScope = []
def systemScope = []
 
// using Groovy Looping and mapping a Groovy Closure to each element, we collect together all
// the dependency nodes into corresponding collections depending on their scope value.
dependencies.each() {
    def scope = (elementHasText(it.scope)) ? it.scope : "compile"
    switch (scope) {
        case "compile":
            compileTimeScope.add(it)
            break
        case "test":
            testScope.add(it)
            break
        case "provided":
            providedScope.add(it)
            break
        case "runtime":
            runTimeScope.add(it)
            break
        case "system":
            systemScope.add(it)
            break
    }
}
 
/**
 * print function then checks the exclusions node to see if it exists, if
 * so it branches off, otherwise we call our simple print function
 */
def printGradleDep = {String scope, it ->
    def exclusions = it.exclusions.exclusion
    if (exclusions.size() > 0) {
        printComplexDependency(it, scope)
    } else {
        printBasicDependency(it, scope)
    }
}
 
/**
 * complex print statement does one extra task which is iterate over each
 * �exclusion� node and print out the artifact id.
 */
private def printComplexDependency(it, scope) {
    println "addDependency(['${scope}'], \"${contructSignature(it)}\" ) {"
    it.exclusions.exclusion.each() {
        println "   exclude(module: '${it.artifactId}')"
    }
    println "}"
}
 
/**
 * Print out the basic form og gradle dependency
 */
private def printBasicDependency(it, String scope) {
    def classifier = contructSignature(it)
    println "${scope}: \"${classifier}\""
}
 
/**
 * Construct and return the signature of a dependency, including it's version and
 * classifier if it exists
 */
private def contructSignature(it) {
    def gradelDep = "${it.groupId.text()}:${it.artifactId.text()}:${it?.version?.text()}"
    def classifier = elementHasText(it.classifier) ? gradelDep + ":" + it.classifier.text().trim() : gradelDep
    return classifier
}
 
/**
 * Check to see if the selected node has content
 */
private boolean elementHasText(it) {
    return it.text().length() != 0
}
 
// for each collection, one at a time, we take each element and call our print function
compileTimeScope.each() { printGradleDep("compile", it) }
runTimeScope.each() { printGradleDep("runtime", it) }
testScope.each() { printGradleDep("testCompile", it) }
providedScope.each() { printGradleDep("provided", it) }
systemScope.each() { printGradleDep("system", it) }