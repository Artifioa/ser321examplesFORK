package taskone;

import java.util.List;
import java.util.ArrayList;

class StringList {
    
    List<String> strings = new ArrayList<String>();

    public void add(String str) {
        int pos = strings.indexOf(str);
        if (pos < 0) {
            strings.add(str);
        }
    }

    public boolean contains(String str) {
        return strings.indexOf(str) >= 0;
    }

    public int size() {
        return strings.size();
    }

    public void clear() {
        strings.clear();
    }

    public void set(int index, String str) {
        if (index >= 0 && index < strings.size()) {
            strings.set(index, str);
        }
    }
    
    public String toString() {
        return strings.toString();
    }
}