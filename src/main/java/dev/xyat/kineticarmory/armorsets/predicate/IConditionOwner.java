package dev.xyat.kineticarmory.armorsets.predicate;

import java.util.List;

public interface IConditionOwner {
    List<ConditionData> getConditions();
    String getMatchMode();
    void setMatchMode(String mode);
    int getMinCount();
    void setMinCount(int count);
}