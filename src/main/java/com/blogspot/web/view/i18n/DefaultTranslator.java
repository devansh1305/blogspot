package com.blogspot.web.view.i18n;

import java.util.Locale;

/**
 * Default English translator. Do nothing.
 * 
 * @author devanshpanirwala
 */
class DefaultTranslator implements Translator {

    @Override
    public String getDisplayName() {
        return "English";
    }

    @Override
    public String getLocaleName() {
        return Locale.ENGLISH.toString();
    }

    @Override
    public String translate(String text, Object... args) {
        return args.length == 0 ? text : String.format(text, args);
    }

}
