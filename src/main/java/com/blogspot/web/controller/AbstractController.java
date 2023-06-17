package com.blogspot.web.controller;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.blogspot.common.AbstractService;
import com.blogspot.markdown.Markdown;
import com.blogspot.redis.RedisService;
import com.blogspot.search.AbstractSearcher;
import com.blogspot.service.AdService;
import com.blogspot.service.AntiSpamService;
import com.blogspot.service.ArticleService;
import com.blogspot.service.AttachmentService;
import com.blogspot.service.BoardService;
import com.blogspot.service.EncryptService;
import com.blogspot.service.HeadlineService;
import com.blogspot.service.LinkService;
import com.blogspot.service.NavigationService;
import com.blogspot.service.SettingService;
import com.blogspot.service.SinglePageService;
import com.blogspot.service.TextService;
import com.blogspot.service.UserService;
import com.blogspot.service.WikiService;
import com.blogspot.web.view.i18n.Translators;

public abstract class AbstractController extends AbstractService {

    protected static final String ID = "{id:[0-9]{1,17}}";
    protected static final String ID2 = "{id2:[0-9]{1,17}}";

    @Value("${spring.profiles.active:native}")
    String activeProfile;

    @Value("${spring.application.name:blogspot")
    protected String name;

    protected boolean dev;

    @PostConstruct
    public void initEnv() {
        this.dev = "native".equals(this.activeProfile);
        if (this.dev) {
            logger.warn("application is set to dev mode.");
        }
    }

    @Autowired
    protected EncryptService encryptService;

    @Autowired
    protected RedisService redisService;

    @Autowired
    protected AdService adService;

    @Autowired
    protected ArticleService articleService;

    @Autowired
    protected AttachmentService attachmentService;

    @Autowired
    protected BoardService boardService;

    @Autowired
    protected HeadlineService headlineService;

    @Autowired
    protected LinkService linkService;

    @Autowired
    protected NavigationService navigationService;

    @Autowired
    protected SettingService settingService;

    @Autowired
    protected SinglePageService singlePageService;

    @Autowired
    protected TextService textService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected WikiService wikiService;

    @Autowired
    protected AntiSpamService antiSpamService;

    @Autowired
    protected AbstractSearcher searcher;

    @Autowired
    protected Translators translators;

    @Autowired
    protected Markdown markdown;

}
