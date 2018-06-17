package com.nitin.json.pointer;

import java.io.IOException;
import static com.nitin.json.util.LoggerUtils.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ArrayNode;
import com.nitin.json.pointer.exception.JsonPointerException;

/**
 * @author Nitin Patel
 * 
 * This class is an implementation of "JavaScript Object Notation (JSON) Pointer" RFC 6901:
 * https://tools.ietf.org/html/rfc6901
 * 
 * The implementation is based on jackson library, and takes jackson's org.codehaus.jackson.JsonNode
 * as input and output.
 */
public class JsonPointer {

	private static final String TILDA = "~";
	public static final String REGEX_NUMERIC_VALUE = "[0-9]+";
	private static final String ENCODED_CHAR_TILDA = "~0";
	private static final String ENCODED_CHAR_FORWARD_SLASH = "~1";
	public static final String FORWARD_SLASH = "/";
	public static final String PATH_HYPHEN = "-";
	private final String jsonPtr;
	
	public String getJsonPtr() {
		return jsonPtr;
	}

	public static final JsonPointer EMPTY = new JsonPointer(""); 

	/**
	 * Construct a JsonPointer using the pointer represented as a string. 
	 * @param pointer
	 */
	public JsonPointer(String pointer) {
		this.jsonPtr = pointer;
	}
	
	/**
	 * Evaluates the current jsonPointer against the Json document which is provided as an input,
	 * and returns the Json which refers to the current pointer.
	 * 
	 * Usage:
	 * JsonPointer jsonPointer = new JsonPointer("/foo/0");
	 * JsonNode node = jsonPointer.getValue(inputJson));//say inputJson represents {"foo": ["bar", "baz"]}
	 * 
	 * Note: With {"foo": ["bar", "baz"]} as input Json document, and "/foo/0" as json pointer,
	 * this method would return "bar" as the output.
	 * 
	 * @param node - Represents the Json document against which the json pointer needs to be evaluated.
	 * @return Json which refers to the current json pointer.
	 * @throws JsonPointerException - If the json pointer is invalid or if the evaluation fails on the
	 * json document provided as input. 
	 */
	public JsonNode getValue(JsonNode node) 
			throws JsonPointerException {
		validate(node);
		
		if (EMPTY.equals(this))
			return node;
		
		JsonNode currentNode = node; 
		for (String ptrRef : getJsonPtr().substring(1).split(FORWARD_SLASH)) {
			if(ptrRef.matches(REGEX_NUMERIC_VALUE)) {
  			    //handle array
  			    if (!(currentNode instanceof ArrayNode))
  			        throwJsonPointerException(node);
  			  
  			    currentNode = ((ArrayNode)currentNode).get(Integer.parseInt(ptrRef));
			} else {
				//handle object
				ptrRef = decodeEscapedCharSeqs(ptrRef);
				if (!(currentNode instanceof ObjectNode))
                    throwJsonPointerException(node);
				currentNode = ((ObjectNode)currentNode).get(ptrRef);
			}
			if (currentNode == null) {
				throwJsonPointerException(node);
			}
		}
		return currentNode;
	}

  private void throwJsonPointerException(JsonNode node) throws JsonPointerException {
    String errorMsg = "Invalid Json pointer "+getJsonPtr()+" for given JSON "+node;
    getLogger().severe(errorMsg);
    throw new JsonPointerException(errorMsg);
  }

	private void validate(JsonNode node) throws JsonPointerException {
		if (getJsonPtr() == null) {
			throwJsonPointerException(node);
		}
	}
	
	/**
	 * Replaces the json node represented by the current json pointer with
	 * new json node in the given node.
	 * 
	 * @param rootNode
	 * @param newValue
	 * @return
	 * @throws JsonPointerException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public JsonNode replace(JsonNode rootNode, JsonNode newValue) 
			throws JsonPointerException, JsonProcessingException, IOException {
		validate(rootNode);
		if (EMPTY.equals(this)) {
			return newValue;
		}
		
		rootNode = cloneJsonNode(rootNode);
		
		JsonNode parentNode = parent().getValue(rootNode);
		
		String lastReferenceToken = getJsonPtr().substring(getJsonPtr().lastIndexOf(FORWARD_SLASH)+1);
		if(lastReferenceToken.matches(REGEX_NUMERIC_VALUE)) {
			((ArrayNode)parentNode).set(Integer.parseInt(lastReferenceToken), newValue);
		} else {
			((ObjectNode)parentNode).put(lastReferenceToken, newValue);
		}
		return rootNode;
	}

	private JsonNode cloneJsonNode(JsonNode node) throws JsonProcessingException, IOException {
		return new ObjectMapper().readTree(node.toString());
	}

	public JsonPointer parent() {
		return new JsonPointer(getJsonPtr().substring(0,
				getJsonPtr().lastIndexOf(FORWARD_SLASH)));
	}
	
	public JsonNode getParentNode(JsonNode node) throws JsonPointerException {
		JsonNode parentNode = parent().getValue(node);
		if (parentNode == null) {
		    String errorMsg = "Invalid Json pointer - Parent node not found: "+parent();
		    getLogger().severe(errorMsg);
			throw new JsonPointerException(errorMsg);
		}
		return parentNode;
	}
	
	private String decodeEscapedCharSeqs(String jsonPointer) {
		return jsonPointer.
				replace(ENCODED_CHAR_FORWARD_SLASH, FORWARD_SLASH).
				replace(ENCODED_CHAR_TILDA, TILDA);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other != null && other instanceof JsonPointer &&
				getJsonPtr().equals(((JsonPointer)other).jsonPtr)) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getJsonPtr();
	}
	
	@Override
	public int hashCode() {
		return getJsonPtr().hashCode();
	}
}