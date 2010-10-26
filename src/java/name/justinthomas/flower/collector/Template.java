/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.collector;

import java.util.LinkedHashMap;

/**
 *
 * @author justin
 */
public class Template {

    public LinkedHashMap<Integer, Integer> fields = new LinkedHashMap<Integer, Integer>();

    public Integer length() {
        Integer length = 0;

        for (Integer value : fields.values()) {
            length += value;
        }

        return length;
    }
}
