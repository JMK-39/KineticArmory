package dev.xyat.kineticarmory.armorsets.predicate;

import java.util.HashMap;
import java.util.Map;

public class ConditionData {
    public String type = "";
    public boolean invert = false;
    public Map<String, String> params = new HashMap<>();

    public ConditionData() {
    }

    public ConditionData(String type) {
        this.type = type;
    }
}