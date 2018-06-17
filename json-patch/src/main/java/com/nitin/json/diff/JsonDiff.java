package com.nitin.json.diff;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;

import static com.nitin.json.util.LoggerUtils.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

public class JsonDiff {
    private static ObjectMapper mapper = new ObjectMapper();
	private ArrayNode diff = JsonNodeFactory.instance.arrayNode();

	public JsonDiff() {
		// TODO Auto-generated constructor stub
	}
	
	public JsonNode getDiff(String sourceJson, String otherJson) {
        try {
            getDiff(mapper.readTree(sourceJson), mapper.readTree(otherJson));
        } catch (IOException e) {
            String errorMsg = "Error while parsing the input json: "+e.getMessage();
            getLogger().log(Level.SEVERE, errorMsg, e);
        }
        return diff;
    }
	
	public JsonNode getDiff(JsonNode sourceJson, JsonNode otherJson) {
		compare(sourceJson, otherJson, "");
		return diff;
	}

	private void compare(JsonNode source, JsonNode target, String sourcePtr) {
		if (source == null && target == null)
			return;
		if (source == null && target != null) {
			//generate a diff which will be to replace entire source entry with target
			getLogger().fine("Source is null\n source: "+source+"\n target: "+target+"\n");
			ObjectNode addOp = JsonNodeFactory.instance.objectNode();
			addOp.put("op", "add");
			addOp.put("path", sourcePtr);
			addOp.put("value", target);

			diff.add(addOp);
			return;
		}
		if (target == null && source != null) {
			//generate a diff which will be to replace entire source entry with target
			getLogger().fine("Target is null\n source: "+source+"\n target: "+target+"\n");
			ObjectNode removeOp = JsonNodeFactory.instance.objectNode();
			removeOp.put("op", "remove");
			removeOp.put("path", sourcePtr);
			
			diff.add(removeOp);
			return;
		}
		
		if (source.equals(target)) {
			getLogger().fine("Source and target are EQUAL: "+source);
			return;
		} else if (source.isArray() && target.isArray()) {
			ArrayNode sourceArray = (ArrayNode)source;
			ArrayNode targetArray = (ArrayNode)target;

			int idx = 0;
			while (sourceArray.has(idx)  || targetArray.has(idx)) {
				compare(sourceArray.get(idx), targetArray.get(idx), sourcePtr+"/"+idx);
				idx++;
			}
		} else if (source.isObject() && target.isObject()) {
			ObjectNode sourceObj = (ObjectNode)source;
			ObjectNode targetObj = (ObjectNode)target;
			
			Iterator<String> fields = sourceObj.getFieldNames();
			while (fields.hasNext()) {
				String fieldName = fields.next();
				JsonNode sourceElem = sourceObj.get(fieldName);
				JsonNode targetElem = targetObj.get(fieldName);
				getLogger().fine("Checking field "+fieldName+". Recurse with s: "+sourceElem+" & t:"+targetElem);
				compare(sourceElem, targetElem, sourcePtr+"/"+fieldName);
			}
		} else {
			//generate a diff which will be to replace entire source entry with target
			getLogger().fine("Source and target are different types:\n source: "+source+"\n target: "+target+"\n");
			ObjectNode replaceOp = JsonNodeFactory.instance.objectNode();
			replaceOp.put("op", "replace");
			replaceOp.put("path", sourcePtr);
			replaceOp.put("value", target);
			
			diff.add(replaceOp);
			return;
		}
	}

	private static boolean isSourceTypeDifferentThanTarget(JsonNode source, JsonNode target) {
		boolean different = (source.isObject() && target.isArray()) || (source.isArray() && target.isObject());		
		return different;
	}

}
