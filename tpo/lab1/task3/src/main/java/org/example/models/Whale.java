package org.example.models;

import java.util.Objects;

public class Whale extends BaseModel {
    private WhaleCondition condition;
    private boolean stillWhale;

    public Whale(String name, WhaleCondition condition) {
        super(name);
        this.condition = Objects.requireNonNull(condition, "Состояние не может быть null");
        this.stillWhale = true;
    }

    public WhaleCondition getCondition() {
        return condition;
    }

    public void setCondition(WhaleCondition condition) {
        this.condition = Objects.requireNonNull(condition, "Состояние не может быть null");
    }

    public boolean isStillWhale() {
        return stillWhale;
    }

    public void setStillWhale(boolean stillWhale) {
        this.stillWhale = stillWhale;
    }
}