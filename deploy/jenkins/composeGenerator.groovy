def generateCompose(composeVars, pathToComposeTemplate, pathToTargetComposeFile){
    def resultMap = [:]
    def composeTemplate = readFile pathToComposeTemplate
    resultMap.putAll(composeVars)
    def targetComposeFile = readFile pathToTargetComposeFile
    def compose = dotemplate(composeTemplate, resultMap)
    writeFile(file: pathToTargetComposeFile, text: compose)

}
@NonCPS
String dotemplate(text, bind) {
    def engine = new groovy.text.GStringTemplateEngine()
    def template = engine.createTemplate(text).make(bind)
    //return template.toString()
}

def checkCompose(pathComposeTemplate) { 
    if !pathComposeTemplateDev3 = null { 
        
    }
}