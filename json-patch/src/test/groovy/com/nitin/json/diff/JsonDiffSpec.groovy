package com.nitin.json.diff

import java.io.File
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import spock.lang.Specification
import com.nitin.json.diff.JsonDiff
import com.nitin.json.patch.JsonPatch

class JsonDiffSpec extends Specification {
  private static String sampleJsonDir = "src"+File.separator+"test"+File.separator+"resources"+File.separator;
  
  def "Json Diff Test"() {
    expect:
    getJsonDiff(source, other) == getExpectedResult(expectedFile);
    
    where:
    source                  |   other                   |   expectedFile
    "diff-source.json"      |   "diff-other.json"       |   "diff-output.json"
    "sample-topology.json"  |   "other-topology.json"   |   "topology-diff-output.json"
  }
  
  def "Patch Json Diff Test"() {
    expect:
    applyJsonPatch("topology-diff-output.json", "sample-topology.json") == getExpectedResult("other-topology.json");
  }
  
  def getJsonDiff(source, other) {
    def diff = new JsonDiff().getDiff(new File(sampleJsonDir+source).text, new File(sampleJsonDir+other).text);
    println "Json Diff is: $diff"
    diff.toString()
  }
  
  def getExpectedResult(file) {
    new File(sampleJsonDir+file).text;
  }
  
  def applyJsonPatch(patch, inputDocument) {
    patch = getExpectedResult(patch)
    JsonPatch jsonPatch = new JsonPatch(patch)
    def patchOutput = jsonPatch.apply(getExpectedResult(inputDocument))
    println "Output after applying JsonDiff $patch as Patch is:\n $patchOutput"
    patchOutput.toString()
  }
}
