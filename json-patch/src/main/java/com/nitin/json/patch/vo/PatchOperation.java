package com.nitin.json.patch.vo;

import java.io.IOException;
import java.util.logging.Level;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import static com.nitin.json.util.LoggerUtils.*;
import com.nitin.json.pointer.JsonPointer;
import com.nitin.json.pointer.exception.JsonPointerException;
import com.nitin.json.patch.exception.InvalidJsonPatchException;
import com.nitin.json.patch.exception.JsonPatchException;
import static com.nitin.json.patch.vo.PatchOperationType.*;

/*
 * This is part of implementation of RFC 6902:
 * https://tools.ietf.org/html/rfc6902
 * 
 * Implementation is based on Jackson library
 */
/**
 * @author Nitin Patel
 *
 */
public class PatchOperation {
	/*
	 * TODO: handle cases where from is the entire object. Eg: /xmlConfig/discoveryConfig/bIPublisherURL
	 * TODO: The "from" location MUST NOT be a proper prefix of the "path" location; i.e., a location cannot be moved into one of its children.
	 */

	public PatchOperation(JsonNode operationObj) throws InvalidJsonPatchException {
		this.opJson = operationObj.toString();
		//getLogger().fine(operationObj.path(PatchOperationType.Member.OP.getName()));
		
		this.op = operationObj.path(PatchOperationType.Member.OP.getName()).asText();
		this.path = operationObj.path(PatchOperationType.Member.PATH.getName()).asText();
		this.from = operationObj.path(PatchOperationType.Member.FROM.getName()).asText();
		this.valueText = operationObj.path(PatchOperationType.Member.VALUE.getName()).asText();
		this.value = operationObj.path(PatchOperationType.Member.VALUE.getName());
		//getLogger().fine("Value as text: "+valueText+" & value.toString: "+this.value.toString());

		//this is exception case where the value has escape chars, but valueText doesn't
		//we would need to parse valueText w/o escape chars back to value
		if (this.valueText != null && !this.valueText.equals(this.value.toString()) &&
				this.value.toString().contains("\\")) {
			try {
				this.value = mapper.readTree(this.valueText);
			} catch (IOException e) {
			    String errorMsg = "Error occurred while trying to execute remove patch operation.";
	            getLogger().log(Level.SEVERE, errorMsg, e);
	            throw new InvalidJsonPatchException(
						"Error occurred while trying to parse value member: "+this.valueText, e);
			}
		}

		validateOperationType();
		this.opType = PatchOperationType.valueOf(this.op);

		validateOperationMembers(operationObj);
	}
	
	private void validateOperationMembers(JsonNode operationObj) throws InvalidJsonPatchException {
		for (PatchOperationType.Member requiredMember : this.opType.getMembers()) {
			if (!operationObj.path(requiredMember.OP.getName()).isValueNode()) {
			    throwInvalidJsonPatchException(operationObj+ " has invalid value "+
						operationObj.path(requiredMember.OP.getName())+
						" for required member "+requiredMember.OP.getName());
			}
		}
	}

	private void throwInvalidJsonPatchException(String errorMsg) throws InvalidJsonPatchException {
	    getLogger().log(Level.SEVERE, errorMsg);
	    throw new InvalidJsonPatchException(errorMsg);
    }

    private void validateOperationType() throws InvalidJsonPatchException {
		if (this.op == null || this.op.isEmpty() || 
				!PatchOperationType.isValidOperationType(this.op)) {
		    throwInvalidJsonPatchException("Invalid Json Operation "+this.opJson+" with op set as: "+this.op);
		}
	}

	public PatchOperation(String operationJson) throws 
		JsonProcessingException, IOException, InvalidJsonPatchException {
		this(new ObjectMapper().readTree(operationJson));
	}
	
	private String op, path, from, valueText;
	JsonNode value;
	private PatchOperationType opType;
	private static ObjectMapper mapper = new ObjectMapper();
	private String opJson;
	
	/**
	 * @param sourceNode
	 * @return
	 * @throws JsonPatchException
	 */
	public JsonNode apply(JsonNode sourceNode) throws JsonPatchException {
		//operate on clone so that original node is not modified
		JsonNode result = cloneJsonNode(sourceNode);
		getLogger().fine("Applying patch operation "+this.opJson+" on node: "+sourceNode);
		switch(this.opType){
			case add:
				return executeAddOperation(result);
			case replace:
				return executeReplaceOperation(result);
			case remove:
				return executeRemoveOperation(result);
			case move:
				return executeMoveOperation(result);
			case copy:
				return executeCopyOperation(result);
			case test:
				return executeTestOperation(result);
		}
		return result;
	}

	/*
	 * Execute Add operation as follows:
	 * The "add" operation performs one of the following functions,
   depending upon what the target location references:

   o  If the target location specifies an array index, a new value is
      inserted into the array at the specified index.

   o  If the target location specifies an object member that does not
      already exist, a new member is added to the object.
      
   o  If the target location specifies an object member that does exist,
      that member's value is replaced.
	 */
	private JsonNode executeAddOperation(JsonNode result) throws JsonPatchException {
		try {
			getLogger().fine("Adding "+this.value+" at "+this.path);
			JsonNode nodeToBeAdded = this.value;
			
			add(result, new JsonPointer(this.path), nodeToBeAdded);
		} catch (JsonPointerException e) {
		    throwJsonPatchException("Error occurred while trying to execute add patch operation: "+this.opJson, e);
		}
		getLogger().fine("Add operation successful. Resulting JSON:\n"+result);
		return result;
	}

	/*
	 * The "replace" operation replaces the value at the target location
   with a new value.  The operation object MUST contain a "value" member
   whose content specifies the replacement value.

   The target location MUST exist for the operation to be successful.

   For example:

   { "op": "replace", "path": "/a/b/c", "value": 42 }

   This operation is functionally identical to a "remove" operation for
   a value, followed immediately by an "add" operation at the same
   location with the replacement value.
	 */
	private JsonNode executeReplaceOperation(JsonNode result) throws JsonPatchException {
		JsonPointer jsonPtr = new JsonPointer(getPath());
		try {
			result = jsonPtr.replace(result, this.value);
		} catch (JsonPointerException | IOException e) {
		    throwJsonPatchException("Error occurred while trying to execute replace patch operation: "+this.opJson, e);
		}
		getLogger().fine("Replace operation successful. Resulting JSON:\n"+result);
		return result;
	}

	/*
	 * The "remove" operation removes the value at the target location.

   The target location MUST exist for the operation to be successful.

   For example:

   { "op": "remove", "path": "/a/b/c" }

   If removing an element from an array, any elements above the
   specified index are shifted one position to the left.
	 */
	private JsonNode executeRemoveOperation(JsonNode result) throws JsonPatchException {
		try {
			//get the node to be removed represented by path member
			JsonPointer ptrToBeRemoved = new JsonPointer(this.path);
			
			result = remove(result, ptrToBeRemoved);
		} catch (JsonPointerException e) {
		    throwJsonPatchException("Error occurred while trying to execute remove patch operation.", e);
		}
		getLogger().fine("Remove operation successful. Resulting JSON:\n"+result);
		return result;
	}

	private void throwJsonPatchException(String errorMsg, Exception e) throws JsonPatchException {
	    getLogger().log(Level.SEVERE, errorMsg, e); 
        throw new JsonPatchException(errorMsg, e);
    }

  private JsonNode remove(JsonNode result, JsonPointer ptrToBeRemoved) throws JsonPointerException {
		JsonNode parentNode = ptrToBeRemoved.getParentNode(result);
		
		String lastReferenceToken = ptrToBeRemoved.getJsonPtr().
				substring(ptrToBeRemoved.getJsonPtr().lastIndexOf(JsonPointer.FORWARD_SLASH)+1);
		if(lastReferenceToken.matches(JsonPointer.REGEX_NUMERIC_VALUE)) {
			int idx = Integer.parseInt(lastReferenceToken);
			ArrayNode parentArray = (ArrayNode)parentNode;
			if (idx > parentArray.size()-1) {
	            throwJsonPointerException(idx, parentArray);
			}
			parentArray.remove(idx);
		} else {
			((ObjectNode)parentNode).remove(lastReferenceToken);
		}
		return result;
	}

    private void throwJsonPointerException(int idx, ArrayNode parentArray) throws JsonPointerException {
        String errorMsg = "Invalid Json Pointer: Index "+idx+
            " is not valid, given the parent Array "+parentArray+" with size: "+parentArray.size();
        getLogger().log(Level.SEVERE, errorMsg);
        throw new JsonPointerException(errorMsg);
    }

	/*
	 * This operation is functionally identical to a "remove" operation on
	 * the "from" location, followed immediately by an "add" operation at
	 * the target location with the value that was just removed.
	 */
	private JsonNode executeMoveOperation(JsonNode result) throws JsonPatchException {
		try {
			//get the object/value represented by from member
			JsonPointer ptrToBeMoved = new JsonPointer(this.from);
			JsonNode nodeToBeMoved = ptrToBeMoved.getValue(result);
			
			//remove the object/value represented by ptrToBeMoved
			result = remove(result, ptrToBeMoved);
			
			//Add from value at the target location represented by path member
			JsonPointer targetPtr = new JsonPointer(this.path);
			result = add(result, targetPtr, nodeToBeMoved);
		} catch (JsonPointerException e) {
		    throwJsonPatchException("Error occurred while trying to execute move patch operation: "+this.opJson, e);
        }
		getLogger().fine("Move operation successful. Resulting JSON:\n"+result);
		return result;
	}

	/*
	 * This operation is functionally identical to an "add" operation at the
	 * target location using the value specified in the "from" member.
	 */
	private JsonNode executeCopyOperation(JsonNode result) throws JsonPatchException {
		try {
			//get the object/value the from member
			JsonPointer fromPtr = new JsonPointer(this.from);
			JsonNode nodeToBeAdded = fromPtr.getValue(result);
			
			//Add from value at the target location represented by path member
			JsonPointer targetPtr = new JsonPointer(this.path);
			result = add(result, targetPtr, nodeToBeAdded);
		} catch (JsonPointerException e) {
		    throwJsonPatchException("Error occurred while trying to execute copy patch operation: "+this.opJson, e);
		}
		getLogger().fine("Copy operation successful. Resulting JSON:\n"+result);
		return result;
	}

	private JsonNode add(JsonNode result, JsonPointer targetPtr, JsonNode nodeToBeAdded) throws JsonPointerException {
		JsonNode parentNode = targetPtr.getParentNode(result);
		
		String lastReferenceToken = targetPtr.getJsonPtr().
				substring(targetPtr.getJsonPtr().lastIndexOf(JsonPointer.FORWARD_SLASH)+1);
		if(lastReferenceToken.matches(JsonPointer.REGEX_NUMERIC_VALUE)) {
			//check if given index is valid, should be <= size of parent array
			int idx = Integer.parseInt(lastReferenceToken);
			ArrayNode parentArray = (ArrayNode)parentNode;
			if (idx > parentArray.size()) {
	             throwJsonPointerException(idx, parentArray);
			}
			//insert node at specified index, which ensures that:
			//Any elements at or above the index are shifted one position to the right
			parentArray.insert(idx, nodeToBeAdded);
		} else if (JsonPointer.PATH_HYPHEN.equals(lastReferenceToken)) {
			//if "-" is used as the index, then add it to the end of the array
			((ArrayNode)parentNode).add(nodeToBeAdded);
		} else {
			((ObjectNode)parentNode).put(lastReferenceToken, nodeToBeAdded);
		}
		return result;
	}

	/*
	 * The "test" operation tests that a value at the target location is
	 * equal to a specified value.
	 */
	private JsonNode executeTestOperation(JsonNode result) throws JsonPatchException {
		try {
			//get the value at target location represented by path
			JsonNode targetNode = new JsonPointer(this.path).getValue(result);
			getLogger().fine("Testing "+targetNode+" against "+this.value);
			if (!targetNode.equals(this.value))
                throwJsonPatchException("\"test\" patch operation "+this.opJson
                    + " failed as the target location is not equal to the specified value.");
			else
			    getLogger().fine("Test operation successful!");
		} catch (JsonPointerException e) {
		    throwJsonPatchException("Error occurred while applying \"test\" patch operation: "+this.opJson, e);
		}
		return result;
	}

    private void throwJsonPatchException(String errorMsg) throws JsonPatchException {
        getLogger().severe(errorMsg);
        throw new JsonPatchException(errorMsg);
    }

	/*
	 * Creates a deep copy of the given jsonnode.
	 * This is required before performing any patch operation on source. 
	 */
	private JsonNode cloneJsonNode(JsonNode node) throws JsonPatchException {
		JsonNode clone = null;
		try {
			clone = mapper.readTree(node.toString());
		} catch (IOException e) {
		    throwJsonPatchException("Failed clone the source JSON "
					+ "while trying to apply patch operation: "+this.opJson, e);
		}
		return clone;
	}

	public PatchOperationType getOpType() {
		return opType;
	}

	public void setOpType(PatchOperationType opType) {
		this.opType = opType;
	}

	public String getOp() {
		return op;
	}

	public void setOp(String op) {
		this.op = op;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setValue(JsonNode value) {
		this.value = value;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}
	
	
}
