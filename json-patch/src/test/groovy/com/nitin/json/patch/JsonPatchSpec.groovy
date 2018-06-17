package com.nitin.json.patch

import java.io.File;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.node.ArrayNode;
import spock.lang.Specification
import com.nitin.json.patch.JsonPatch;
import com.nitin.json.patch.exception.JsonPatchException

class JsonPatchSpec extends Specification {
  private static String sampleJsonDir = "src"+File.separator+"test"+File.separator+"resources"+File.separator;
  JsonNode topologyDocument = null;
  
  def setup() {
    File sourceJson = new File(sampleJsonDir+"sample-topology.json");
    ObjectMapper mapper = new ObjectMapper();
    topologyDocument = mapper.readTree(sourceJson);
  }
  
  def "Json Patch Test"() {
    expect:
    applyJsonPatch("patch-pass.json", topologyDocument) == getPatchAsJsonString("patchOutput.json")
  }
  
  def getPatchAsJsonString(patchFile) {
    new File(sampleJsonDir+patchFile).text
  }
  
  def "Negative Json Patch Test"() {
    when:
    applyJsonPatch("patch-fail.json", topologyDocument)
    
    then:
    thrown(JsonPatchException)
  }
  
  def applyJsonPatch(patch, inputDocument) {
    JsonPatch jsonPatch = new JsonPatch(getPatchAsJsonString(patch))
    jsonPatch.apply(inputDocument).toString()
  }
}
