package com.nitin.json.pointer
import java.io.File
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import com.nitin.json.pointer.JsonPointer
import com.nitin.json.pointer.exception.JsonPointerException
import spock.lang.*

class JsonPointerSpec extends Specification {
  private static String sampleJsonDir = "src"+File.separator+"test"+File.separator+"resources"+File.separator;
  JsonNode topologyDocument = null;
  
  def setup() {
    File sourceJson = new File(sampleJsonDir+"sample-topology.json");
    ObjectMapper mapper = new ObjectMapper();
    topologyDocument = mapper.readTree(sourceJson);
    //println "Input Json document is: $topologyDocument"
  }
  
  def "Invalid Pointer test"() {
    String invalidPtr = "/topology/5";
    when:
    getJsonPointerValue(invalidPtr)
    
    then:
    JsonPointerException e = thrown()
    println e.getMessage()
    e.getMessage() == "Invalid Json pointer $invalidPtr for given JSON $topologyDocument"
  }
  
  def "Json Pointer test"() {
    expect:
    getJsonPointerValue(jsonPointer1) == getExpectedResult(expectedFile);
    
    where:
    jsonPointer1                        |   expectedFile
    ""                                  |   "sample-topology.json"
    "/topology"                         |   "topology.json"
    "/topology/webservers"              |   "webservers.json"
    "/topology/appservers/1"            |   "appserver1.json"
    "/topology/appservers/1/protocols/2"|   "t3protocol.json"
  }
  
  def getJsonPointerValue(jsonPointer) {
    JsonNode ptrOutput = new JsonPointer(jsonPointer).getValue(topologyDocument);
    println "JsonPointer $jsonPointer has been resolved to $ptrOutput"
    ptrOutput.toString()
  }
  
  def getExpectedResult(expectedFile) {
    new File(sampleJsonDir+expectedFile).text;
  }
}
