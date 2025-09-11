package nf_core.nf.test.utils;

import java.lang.RuntimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OutputSanitizer {
  public static TreeMap<String,Object> sanitizeOutput(HashMap<String,Object> options, TreeMap<String,Object> channel) {
    String className = channel.getClass().getName();
    // Can't do valid type checking here because the Channels type is not exposed from nf-test
    if (!className.equals("com.askimed.nf.test.lang.channels.Channels")) {
      throw new java.lang.RuntimeException("sanitizeOutput only supports channels as input, pass it either `process.out` or `workflow.out`");
    }

    // Fetch options
    ArrayList<String> unstableKeys = (ArrayList<String>) options.getOrDefault("unstableKeys", new java.util.ArrayList<String>());

    TreeMap<String,Object> output = new TreeMap<String,Object>();
    Integer channelSize = (Integer) channel.size();
    for (Map.Entry<String,Object> entry : channel.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if(key.matches("^\\d+$") && channelSize > 1) {
        // Skip numeric keys if there is more than one entry in the channel
        continue;
      }
      if(unstableKeys.contains(key)) {
        output.put(key, fixUnstable(value));
      } else {
        output.put(key, value);
      }
    }
    return output;
  }

  private static Object fixUnstable(Object value) {
    if (value instanceof String) {
      String strValue = (String) value;
      if(Files.exists(Paths.get(strValue))) {
        return strValue.substring(strValue.lastIndexOf('/') + 1);
      } else {
        return strValue;
      }
    } else if (value instanceof ArrayList || value instanceof Vector) {
      List listValue = (List) value;
      ArrayList fixedList = new ArrayList();
      for (Object item : listValue) {
        fixedList.add(fixUnstable(item));
      }
      return fixedList;
    } else if (value instanceof Map) {
      Map mapValue = (Map) value;
      Map fixedMap = new TreeMap();
      for (Object entryObj : mapValue.entrySet()) {
        Map.Entry entry = (Map.Entry) entryObj;
        fixedMap.put(entry.getKey(), fixUnstable(entry.getValue()));
      }
      return fixedMap;
    } else {
      return value;
    }
  }
}
