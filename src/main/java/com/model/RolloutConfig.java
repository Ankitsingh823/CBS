package com.model;

import java.util.List;

public class RolloutConfig {
    private Boolean enableAll;
    private Integer enableAllRollout;
    private List<EntityRollout> entities;
    private List<String> disableAny;

    public Boolean getEnableAll() { return enableAll; }
    public void setEnableAll(Boolean enableAll) { this.enableAll = enableAll; }

    public Integer getEnableAllRollout() { return enableAllRollout; }
    public void setEnableAllRollout(Integer enableAllRollout) { this.enableAllRollout = enableAllRollout; }

    public List<EntityRollout> getEntities() { return entities; }
    public void setEntities(List<EntityRollout> entities) { this.entities = entities; }

    public List<String> getDisableAny() { return disableAny; }
    public void setDisableAny(List<String> disableAny) { this.disableAny = disableAny; }
}