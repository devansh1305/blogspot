package com.blogspot.bean.setting;

import com.blogspot.anno.SettingInput;
import com.blogspot.enums.InputType;

public class SettingDefinition {

    public final String name;
    public final String description;
    public final InputType type;

    public SettingDefinition(String name, SettingInput input) {
        this.name = name;
        this.description = input.description();
        this.type = input.value();
    }
}
