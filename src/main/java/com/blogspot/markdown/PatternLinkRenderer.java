package com.blogspot.markdown;

import org.commonmark.renderer.html.HtmlWriter;

/**
 * Convert any pattern link to any html. Example: convert link
 * "https://www.youtube.com/watch?v=UF8uR6Z6KLc" to a HTML5 player.
 * 
 * @author devanshpanirwala
 */
public interface PatternLinkRenderer {

    /**
     * Try render a link. Return false to skip.
     * 
     * @param html  HtmlWriter object.
     * @param url   Link url, not empty.
     * @param title Link title, maybe null.
     * @return True if rendered ok, or false to skip.
     */
    boolean render(HtmlWriter html, String url, String title);

}
