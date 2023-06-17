package com.blogspot.bean;

public class ReplyBean extends AbstractRequestBean {

    public String content;

    @Override
    public void validate(boolean createMode) {
        this.content = checkContent(this.content);
    }

}
