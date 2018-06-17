package com.nitin.json.patch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import com.nitin.json.patch.exception.InvalidJsonPatchException;
import com.nitin.json.patch.exception.JsonPatchException;
import com.nitin.json.patch.vo.PatchOperation;
import static com.nitin.json.util.LoggerUtils.*;
/*
 * This is an implementation of RFC 6902:
 * https://tools.ietf.org/html/rfc6902
 * 
 * Implementation is based on Jackson library
 */
public class JsonPatch {
	List<PatchOperation> patchOperations = new ArrayList<PatchOperation>();
	private static ObjectMapper mapper = new ObjectMapper();

	public JsonPatch(ArrayNode patchArray) throws InvalidJsonPatchException {
		Iterator<JsonNode> patchElems = patchArray.getElements();
		while (patchElems.hasNext()) {
			patchOperations.add(new PatchOperation(patchElems.next()));
		}
	}
	
	public JsonPatch(String patch) throws InvalidJsonPatchException, JsonProcessingException, IOException {
		this((ArrayNode)mapper.readTree(patch));
	}

	public JsonNode apply(JsonNode sourceNode) throws JsonPatchException {
		JsonNode resultNode = sourceNode;
		for (PatchOperation patchOp : patchOperations) {
			resultNode = patchOp.apply(resultNode);
		}
		return resultNode;
	}
	
	public JsonNode apply(String sourceJson) throws JsonPatchException {
	    JsonNode patchOutcome = null;
	    try {
	      patchOutcome = apply(mapper.readTree(sourceJson));
        } catch (IOException e) {
            String errorMsg = "Error occurred while trying to parse the source json."
                + " Corresponding error message is: "+e.getMessage();
            getLogger().log(Level.SEVERE, errorMsg, e);
            throw new JsonPatchException(errorMsg, e);
        }
	    return patchOutcome; 
	}
}
