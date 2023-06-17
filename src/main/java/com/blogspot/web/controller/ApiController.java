package com.blogspot.web.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blogspot.bean.AdMaterialBean;
import com.blogspot.bean.AdPeriodBean;
import com.blogspot.bean.AdSlotBean;
import com.blogspot.bean.ArticleBean;
import com.blogspot.bean.AttachmentBean;
import com.blogspot.bean.BoardBean;
import com.blogspot.bean.CategoryBean;
import com.blogspot.bean.HeadlineBean;
import com.blogspot.bean.LinkBean;
import com.blogspot.bean.NavigationBean;
import com.blogspot.bean.ReplyBean;
import com.blogspot.bean.SinglePageBean;
import com.blogspot.bean.SortBean;
import com.blogspot.bean.TopicBean;
import com.blogspot.bean.WikiBean;
import com.blogspot.bean.WikiPageBean;
import com.blogspot.bean.WikiPageMoveBean;
import com.blogspot.bean.setting.Follow;
import com.blogspot.bean.setting.Security;
import com.blogspot.bean.setting.Snippet;
import com.blogspot.bean.setting.Website;
import com.blogspot.common.ApiException;
import com.blogspot.enums.ApiError;
import com.blogspot.enums.RefType;
import com.blogspot.enums.Role;
import com.blogspot.model.AdMaterial;
import com.blogspot.model.AdPeriod;
import com.blogspot.model.AdSlot;
import com.blogspot.model.Article;
import com.blogspot.model.Attachment;
import com.blogspot.model.Board;
import com.blogspot.model.Category;
import com.blogspot.model.Headline;
import com.blogspot.model.Link;
import com.blogspot.model.Navigation;
import com.blogspot.model.Reply;
import com.blogspot.model.SinglePage;
import com.blogspot.model.Topic;
import com.blogspot.model.User;
import com.blogspot.model.Wiki;
import com.blogspot.model.WikiPage;
import com.blogspot.service.BoardService.TopicWithReplies;
import com.blogspot.web.filter.HttpContext;
import com.blogspot.web.support.RoleWith;

@RestController
@RequestMapping("/api")
public class ApiController extends AbstractController {

    @Value("${spring.security.anti-spam.lock-days:3650}")
    int lockDaysForSpam = 3650;

    @Value("${spring.security.anti-spam.register-at-least:P7D}")
    Duration registerAtLeast = Duration.ofDays(7);

    @Value("${spring.security.anti-spam.frequency-per-minute:3}")
    int freqPerMin = 3;

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ad
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/adSlots")
    @RoleWith(Role.ADMIN)
    public Map<String, List<AdSlot>> adSlotList() {
        return Map.of(RESULTS, this.adService.getAdSlots());
    }

    @PostMapping("/adSlots")
    @RoleWith(Role.ADMIN)
    public AdSlot adSlotCreate(@RequestBody AdSlotBean bean) {
        AdSlot as = this.adService.createAdSlot(bean);
        this.adService.deleteAdInfoFromCache();
        return as;
    }

    @GetMapping("/adSlots/" + ID)
    @RoleWith(Role.ADMIN)
    public AdSlot adSlot(@PathVariable("id") long id) {
        return this.adService.getById(id);
    }

    @PostMapping("/adSlots/" + ID)
    @RoleWith(Role.ADMIN)
    public AdSlot adSlotUpdate(@PathVariable("id") long id, @RequestBody AdSlotBean bean) {
        AdSlot as = this.adService.updateAdSlot(id, bean);
        this.adService.deleteAdInfoFromCache();
        return as;
    }

    @PostMapping("/adSlots/" + ID + "/delete")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> adSlotDelete(@PathVariable("id") long id) {
        this.adService.deleteAdSlot(id);
        this.adService.deleteAdInfoFromCache();
        return API_RESULT_TRUE;
    }

    @GetMapping("/adPeriods")
    @RoleWith(Role.ADMIN)
    public Map<String, List<AdPeriod>> adPeriodList() {
        return Map.of(RESULTS, this.adService.getAdPeriods());
    }

    @GetMapping("/adPeriods/" + ID)
    @RoleWith(Role.ADMIN)
    public AdPeriod adPeriodGet(@PathVariable("id") long id) {
        return this.adService.getAdPeriodById(id);
    }

    @PostMapping("/adPeriods")
    @RoleWith(Role.ADMIN)
    public AdPeriod adPeriodCreate(@RequestBody AdPeriodBean bean) {
        AdPeriod ap = this.adService.createAdPeriod(bean);
        this.adService.deleteAdInfoFromCache();
        return ap;
    }

    @PostMapping("/adPeriods/" + ID)
    @RoleWith(Role.ADMIN)
    public AdPeriod adPeriodUpdate(@PathVariable("id") long id, @RequestBody AdPeriodBean bean) {
        AdPeriod ap = this.adService.updateAdPeriod(id, bean);
        this.adService.deleteAdInfoFromCache();
        return ap;
    }

    @PostMapping("/adPeriods/" + ID + "/delete")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> adPeriodDelete(@PathVariable("id") long id) {
        this.adService.deleteAdPeriod(id);
        this.adService.deleteAdInfoFromCache();
        return API_RESULT_TRUE;
    }

    @GetMapping("/adMaterials")
    @RoleWith(Role.ADMIN)
    public PagedResults<AdMaterial> adMaterialList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        return this.adService.getAdMaterials(pageIndex);
    }

    @PostMapping("/adPeriods/" + ID + "/adMaterials")
    @RoleWith(Role.SPONSOR)
    public AdMaterial adMaterialCreate(@PathVariable("id") long id, @RequestBody AdMaterialBean bean) {
        User user = HttpContext.getRequiredCurrentUser();
        AdPeriod adPeriod = this.adService.getAdPeriodById(id);
        if (adPeriod.userId != user.id) {
            throw new ApiException(ApiError.PERMISSION_DENIED, null, "Permission denied.");
        }
        AdMaterial am = this.adService.createAdMaterial(user, adPeriod, bean);
        this.adService.deleteAdInfoFromCache();
        return am;
    }

    @PostMapping("/adMaterials/" + ID + "/delete")
    @RoleWith(Role.SPONSOR)
    public Map<String, Boolean> adMaterialDelete(@PathVariable("id") long id) {
        User user = HttpContext.getRequiredCurrentUser();
        this.adService.deleteAdMaterial(user, id);
        this.adService.deleteAdInfoFromCache();
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // headline
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/headlines")
    @RoleWith(Role.CONTRIBUTOR)
    public PagedResults<Headline> headline(@RequestParam(value = "published", defaultValue = "false") boolean published,
            @RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        return published ? this.headlineService.getPublishedHeadlines(pageIndex) : this.headlineService.getUnpublishedHeadlines(pageIndex);
    }

    @PostMapping("/headlines")
    @RoleWith(Role.SUBSCRIBER)
    public Map<String, Boolean> headlineCreate(@RequestBody HeadlineBean bean) {
        User user = HttpContext.getRequiredCurrentUser();
        if (user.role == Role.SUBSCRIBER && user.createdAt > (System.currentTimeMillis() - registerAtLeast.toMillis())) {
            throw new ApiException(ApiError.USER_FORBIDDEN, null, "No permission to submit headline.");
        }
        this.headlineService.createHeadline(user.id, bean);
        return API_RESULT_TRUE;
    }

    @GetMapping("/headlines/" + ID)
    @RoleWith(Role.SUBSCRIBER)
    public Headline headline(@PathVariable("id") long id) {
        Headline h = this.headlineService.getHeadline(id);
        if (!h.published) {
            User user = HttpContext.getRequiredCurrentUser();
            if (h.userId != user.id && user.role == Role.SUBSCRIBER) {
                throw new ApiException(ApiError.USER_FORBIDDEN, null, "No permission to get headline.");
            }
        }
        return h;
    }

    @PostMapping("/headlines/" + ID)
    @RoleWith(Role.EDITOR)
    public Headline headlineUpdate(@PathVariable("id") long id, @RequestBody HeadlineBean bean) {
        Headline h = this.headlineService.updateHeadline(id, bean);
        this.headlineService.deleteHeadlinesFromCache();
        return h;
    }

    @PostMapping("/headlines/" + ID + "/delete")
    @RoleWith(Role.EDITOR)
    public Map<String, Boolean> headlineDelete(@PathVariable("id") long id) {
        this.headlineService.deleteHeadline(id);
        this.headlineService.deleteHeadlinesFromCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/headlines/" + ID + "/publish")
    @RoleWith(Role.EDITOR)
    public Map<String, Boolean> headlinePublish(@PathVariable("id") long id) {
        this.headlineService.publishHeadline(id);
        this.headlineService.deleteHeadlinesFromCache();
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // article
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/articles/" + ID)
    @RoleWith(Role.CONTRIBUTOR)
    public ArticleWithContent article(@PathVariable("id") long id) {
        Article article = this.articleService.getById(id);
        String content = this.textService.getById(article.textId).content;
        return new ArticleWithContent(article, content);
    }

    @GetMapping("/articles")
    @RoleWith(Role.CONTRIBUTOR)
    public PagedResults<Article> articles(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        return this.articleService.getArticles(pageIndex);
    }

    @PostMapping("/articles")
    @RoleWith(Role.CONTRIBUTOR)
    public Article articleCreate(@RequestBody ArticleBean bean) {
        Article article = this.articleService.createArticle(HttpContext.getRequiredCurrentUser(), bean);
        this.articleService.deleteArticlesFromCache(article.categoryId);
        asyncIndex(article);
        return article;
    }

    @PostMapping("/articles/" + ID)
    @RoleWith(Role.CONTRIBUTOR)
    public Article articleUpdate(@PathVariable("id") long id, @RequestBody ArticleBean bean) {
        Article article = this.articleService.updateArticle(HttpContext.getRequiredCurrentUser(), id, bean);
        this.articleService.deleteArticlesFromCache(article.categoryId);
        asyncIndex(article);
        return article;
    }

    @PostMapping("/articles/" + ID + "/delete")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<String, Boolean> articleDelete(@PathVariable("id") long id) {
        Article article = this.articleService.deleteArticle(HttpContext.getRequiredCurrentUser(), id);
        this.articleService.deleteArticlesFromCache(article.categoryId);
        asyncUnindex(id);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // attachment
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/attachments")
    @RoleWith(Role.CONTRIBUTOR)
    public PagedResults<Attachment> attachments(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        return this.attachmentService.getAttachments(pageIndex);
    }

    @GetMapping("/attachments/" + ID)
    @RoleWith(Role.SPONSOR)
    public Attachment attachment(@PathVariable("id") long id) {
        return this.attachmentService.getById(id);
    }

    @PostMapping("/attachments")
    @RoleWith(Role.SPONSOR)
    public Attachment attachmentCreate(@RequestBody AttachmentBean bean) {
        return this.attachmentService.createAttachment(HttpContext.getRequiredCurrentUser(), bean);
    }

    @PostMapping("/attachments/" + ID + "/delete")
    @RoleWith(Role.SPONSOR)
    public Map<String, Boolean> attachmentDelete(@PathVariable("id") long id) {
        this.attachmentService.deleteAttachment(HttpContext.getRequiredCurrentUser(), id);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // board
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/boards")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<String, List<Board>> boards() {
        return Map.of(RESULTS, this.boardService.getBoards());
    }

    @GetMapping("/boards/" + ID)
    @RoleWith(Role.CONTRIBUTOR)
    public Board board(@PathVariable("id") long id) {
        return this.boardService.getById(id);
    }

    @PostMapping("/boards/sort")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> boardsSort(@RequestBody SortBean bean) {
        this.boardService.sortBoards(bean.ids);
        this.boardService.deleteBoardsFromCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/boards")
    @RoleWith(Role.ADMIN)
    public Board boardCreate(@RequestBody BoardBean bean) {
        Board board = this.boardService.createBoard(bean);
        this.boardService.deleteBoardsFromCache();
        return board;
    }

    @PostMapping("/boards/" + ID)
    @RoleWith(Role.ADMIN)
    public Board boardUpdate(@PathVariable("id") long id, @RequestBody BoardBean bean) {
        Board board = this.boardService.updateBoard(id, bean);
        this.boardService.deleteBoardsFromCache();
        return board;
    }

    @PostMapping("/boards/" + ID + "/delete")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> boardDelete(@PathVariable("id") long id) {
        this.boardService.deleteBoard(id);
        this.boardService.deleteBoardFromCache(id);
        this.boardService.deleteTopicsFromCache(id);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // category
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/categories")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<String, List<Category>> categories() {
        return Map.of(RESULTS, this.articleService.getCategories());
    }

    @GetMapping("/categories/" + ID)
    @RoleWith(Role.CONTRIBUTOR)
    public Category category(@PathVariable("id") long id) {
        return this.articleService.getCategoryById(id);
    }

    @PostMapping("/categories/sort")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> categoriesSort(@RequestBody SortBean bean) {
        this.articleService.sortCategories(bean.ids);
        this.articleService.deleteCategoriesFromCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/categories")
    @RoleWith(Role.ADMIN)
    public Category categoryCreate(@RequestBody CategoryBean bean) {
        return this.articleService.createCategory(bean);
    }

    @PostMapping("/categories/" + ID)
    @RoleWith(Role.ADMIN)
    public Category categoryUpdate(@PathVariable("id") long id, @RequestBody CategoryBean bean) {
        Category category = this.articleService.updateCategory(id, bean);
        this.articleService.deleteCategoryFromCache(id);
        return category;
    }

    @PostMapping("/categories/" + ID + "/delete")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> categoryDelete(@PathVariable("id") long id) {
        this.articleService.deleteCategory(id);
        this.articleService.deleteCategoryFromCache(id);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // link
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/links")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<String, List<Link>> links() {
        return Map.of(RESULTS, this.linkService.getLinks());
    }

    @GetMapping("/links/" + ID)
    @RoleWith(Role.CONTRIBUTOR)
    public Link linkGet(@PathVariable("id") long id) {
        return this.linkService.getById(id);
    }

    @PostMapping("/links")
    @RoleWith(Role.ADMIN)
    public Link linkCreate(@RequestBody LinkBean bean) {
        Link link = this.linkService.createLink(bean);
        this.linkService.updateLinksCache();
        return link;
    }

    @PostMapping("/links/" + ID + "/delete")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> linkDelete(@PathVariable("id") long id) {
        this.linkService.deleteLink(id);
        this.linkService.updateLinksCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/links/" + ID)
    @RoleWith(Role.ADMIN)
    public Link linkUpdate(@PathVariable("id") long id, @RequestBody LinkBean bean) {
        Link link = this.linkService.updateLink(id, bean);
        this.linkService.updateLinksCache();
        return link;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // navigation
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/navigations")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<String, List<Navigation>> navigations() {
        return Map.of(RESULTS, this.navigationService.getNavigations());
    }

    @GetMapping("/navigations/" + ID)
    @RoleWith(Role.CONTRIBUTOR)
    public Navigation navigationGet(@PathVariable("id") long id) {
        return this.navigationService.getById(id);
    }

    @GetMapping("/navigations/urls")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<String, List<NavigationMenu>> navigationUrls() {
        List<NavigationMenu> list = new ArrayList<>();
        list.add(new NavigationMenu("Headline", "/headline"));
        this.articleService.getCategories().forEach(c -> {
            list.add(new NavigationMenu(c.name, "/category/" + c.id));
        });
        this.wikiService.getWikis().forEach(w -> {
            list.add(new NavigationMenu(w.name, "/wiki/" + w.id));
        });
        this.singlePageService.getAll().forEach(p -> {
            list.add(new NavigationMenu(p.name, "/single/" + p.id));
        });
        list.add(new NavigationMenu("Discuss", "/discuss"));
        list.add(new NavigationMenu("Custom", "http://"));
        return Map.of(RESULTS, list);
    }

    @PostMapping("/navigations")
    @RoleWith(Role.ADMIN)
    public Navigation navigationCreate(@RequestBody NavigationBean bean) {
        Navigation nav = this.navigationService.createNavigation(bean);
        this.navigationService.removeNavigationsFromCache();
        return nav;
    }

    @PostMapping("/navigations/" + ID + "/delete")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> navigationDelete(@PathVariable("id") long id) {
        this.navigationService.deleteNavigation(id);
        this.navigationService.removeNavigationsFromCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/navigations/sort")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> navigationsSort(@RequestBody SortBean bean) {
        this.navigationService.sortNavigations(bean.ids);
        this.navigationService.removeNavigationsFromCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/navigations/" + ID)
    @RoleWith(Role.ADMIN)
    public Navigation navigationUpdate(@PathVariable("id") long id, @RequestBody NavigationBean bean) {
        Navigation navigation = this.navigationService.updateNavigation(id, bean);
        this.navigationService.removeNavigationsFromCache();
        return navigation;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // search
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @PostMapping("/search/reindex")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> searchReindex() throws Exception {
        searcher.removeIndex();
        searcher.createIndex();
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // reply
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/replies")
    @RoleWith(Role.CONTRIBUTOR)
    public PagedResults<Reply> replyList(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        return this.boardService.getReplies(pageIndex);
    }

    @PostMapping("/topics/" + ID + "/replies")
    @RoleWith(Role.SUBSCRIBER)
    public Reply replyCreate(@PathVariable("id") long id, @RequestBody ReplyBean bean) {
        User user = HttpContext.getRequiredCurrentUser();
        Topic topic = this.boardService.getTopicById(id);
        Reply reply = null;
        try {
            reply = this.boardService.createReply(user, topic, bean);
        } catch (ApiException e) {
            if (e.error == ApiError.SECURITY_ANTI_SPAM && lockDaysForSpam > 0) {
                this.userService.lockUser(user, lockDaysForSpam);
            }
            throw e;
        }
        this.boardService.deleteBoardFromCache(topic.boardId);
        this.boardService.deleteTopicsFromCache(topic.boardId);
        return reply;
    }

    @PostMapping("/replies/" + ID + "/delete")
    @RoleWith(Role.EDITOR)
    public Map<String, Boolean> replyDelete(@PathVariable("id") long id) {
        this.boardService.deleteReply(HttpContext.getRequiredCurrentUser(), id);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // settings
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @PostMapping("/setting/website")
    public Map<String, Boolean> settingWebsiteUpdate(@RequestBody Website bean) {
        this.settingService.setWebsite(bean);
        this.settingService.deleteWebsiteFromCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/setting/snippet")
    public Map<String, Boolean> settingSnippetUpdate(@RequestBody Snippet bean) {
        this.settingService.setSnippet(bean);
        this.settingService.deleteSnippetFromCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/setting/follow")
    public Map<String, Boolean> settingFollowUpdate(@RequestBody Follow bean) {
        this.settingService.setFollow(bean);
        this.settingService.deleteFollowFromCache();
        return API_RESULT_TRUE;
    }

    @PostMapping("/setting/security")
    public Map<String, Boolean> settingSecurityUpdate(@RequestBody Security bean) {
        this.settingService.setSecurity(bean);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // single page
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/singlePages")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<String, List<SinglePage>> singlePages() {
        return Map.of(RESULTS, this.singlePageService.getAll());
    }

    @GetMapping("/singlePages/" + ID)
    @RoleWith(Role.CONTRIBUTOR)
    public SinglePageWithContent singlePage(@PathVariable("id") long id) {
        SinglePage sp = this.singlePageService.getById(id);
        String content = this.textService.getById(sp.textId).content;
        return new SinglePageWithContent(sp, content);
    }

    @PostMapping("/singlePages")
    @RoleWith(Role.ADMIN)
    public SinglePage singlePageCreate(@RequestBody SinglePageBean bean) {
        return this.singlePageService.createSinglePage(bean);
    }

    @PostMapping("/singlePages/" + ID)
    @RoleWith(Role.ADMIN)
    public SinglePage singlePageUpdate(@PathVariable("id") long id, @RequestBody SinglePageBean bean) {
        SinglePage sp = this.singlePageService.updateSinglePage(id, bean);
        this.singlePageService.deleteSinglePageFromCache(id);
        return sp;
    }

    @PostMapping("/singlePages/" + ID + "/delete")
    @RoleWith(Role.ADMIN)
    public Map<String, Boolean> singlePageDelete(@PathVariable("id") long id) {
        this.singlePageService.deleteSinglePage(id);
        this.singlePageService.deleteSinglePageFromCache(id);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // topic
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/topics")
    @RoleWith(Role.CONTRIBUTOR)
    public PagedResults<Topic> topics(@RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        return this.boardService.getTopics(pageIndex);
    }

    @GetMapping("/ref/" + ID + "/topics")
    public Map<String, List<TopicWithReplies>> topicsByRefId(@PathVariable("id") long refId) {
        return Map.of(RESULTS, this.boardService.getTopicsByRefId(refId));
    }

    private void checkTopicSpam(User user) {
        long start = System.currentTimeMillis() - 60_000;
        int n = this.boardService.getRecentTopicCountByUser(user.id, start);
        if (n >= this.freqPerMin) {
            this.userService.lockUser(user, 60);
            throw new ApiException(ApiError.OPERATION_FAILED, "topic", "rate limit");
        }
    }

    @PostMapping("/comments/{tag}")
    @RoleWith(Role.SUBSCRIBER)
    public Topic topicCreateByRefType(@PathVariable("tag") String tag, @RequestBody TopicBean bean) {
        Board board = this.boardService.getBoardByTag(tag);
        if (board.locked) {
            throw new ApiException(ApiError.OPERATION_FAILED, "board", "Board is locked.");
        }
        if (bean.refId == 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "refId", "refId cannot be zero.");
        }
        switch (bean.refType) {
        case ARTICLE:
            this.articleService.getPublishedById(bean.refId);
            break;
        case WIKI:
            this.wikiService.getById(bean.refId);
            break;
        case WIKIPAGE:
            this.wikiService.getWikiPageById(bean.refId);
            break;
        case NONE:
        default:
            throw new ApiException(ApiError.PARAMETER_INVALID, "refType", "Unsupported refType: " + bean.refType);
        }
        Topic topic = null;
        User user = HttpContext.getRequiredCurrentUser();
        checkTopicSpam(user);
        try {
            topic = this.boardService.createTopic(user, board, bean);
        } catch (ApiException e) {
            if (e.error == ApiError.SECURITY_ANTI_SPAM && lockDaysForSpam > 0) {
                this.userService.lockUser(user, lockDaysForSpam);
            }
            throw e;
        }
        this.boardService.deleteBoardFromCache(topic.boardId);
        this.boardService.deleteTopicsFromCache(topic.boardId);
        return topic;
    }

    @PostMapping("/boards/" + ID + "/topics")
    @RoleWith(Role.SUBSCRIBER)
    public Topic topicCreate(@PathVariable("id") long id, @RequestBody TopicBean bean) {
        Board board = this.boardService.getBoardFromCache(id);
        if (board.locked) {
            throw new ApiException(ApiError.OPERATION_FAILED, "board", "Board is locked.");
        }
        if (bean.refType != RefType.NONE && bean.refId == 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "refId", "refId cannot be zero.");
        }
        switch (bean.refType) {
        case ARTICLE:
            this.articleService.getPublishedById(bean.refId);
            break;
        case WIKI:
            this.wikiService.getById(bean.refId);
            break;
        case WIKIPAGE:
            this.wikiService.getWikiPageById(bean.refId);
            break;
        case NONE:
            break;
        default:
            throw new ApiException(ApiError.PARAMETER_INVALID, "refType", "Unsupported refType: " + bean.refType);
        }
        Topic topic = null;
        User user = HttpContext.getRequiredCurrentUser();
        checkTopicSpam(user);
        try {
            topic = this.boardService.createTopic(user, board, bean);
        } catch (ApiException e) {
            if (e.error == ApiError.SECURITY_ANTI_SPAM && lockDaysForSpam > 0) {
                this.userService.lockUser(user, lockDaysForSpam);
            }
            throw e;
        }
        this.boardService.deleteBoardFromCache(topic.boardId);
        this.boardService.deleteTopicsFromCache(topic.boardId);
        return topic;
    }

    @PostMapping("/topics/" + ID + "/delete")
    @RoleWith(Role.EDITOR)
    public Map<String, Boolean> topicDelete(@PathVariable("id") long id) {
        Topic topic = this.boardService.deleteTopic(HttpContext.getRequiredCurrentUser(), id);
        this.boardService.deleteBoardFromCache(topic.boardId);
        this.boardService.deleteTopicsFromCache(topic.boardId);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // user
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/users")
    @RoleWith(Role.CONTRIBUTOR)
    public PagedResults<User> users(@RequestParam(value = "q", defaultValue = "") String q, @RequestParam(value = "page", defaultValue = "1") int pageIndex) {
        if (q.isBlank()) {
            return this.userService.getUsers(pageIndex);
        }
        try {
            long id = Long.parseLong(q);
            User user = this.userService.getById(id);
            return new PagedResults<>(new Page(1, 10, 1, 1), List.of(user));
        } catch (NumberFormatException e) {
            // ignore
        } catch (ApiException e) {
            // ignore
        }
        List<User> users = this.userService.searchUsers(q);
        return new PagedResults<>(new Page(1, users.size(), 1, users.size()), users);
    }

    @GetMapping("/users/ids")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<Long, User> usersByIds(HttpServletRequest request) {
        String[] idList = request.getParameterValues("id");
        if (idList == null) {
            return Map.of();
        }
        long[] ids = Arrays.stream(idList).mapToLong(Long::parseLong).toArray();
        List<User> users = this.userService.getUsersByIds(ids);
        return users.stream().collect(Collectors.toMap(u -> u.id, u -> u));
    }

    @PostMapping("/users/" + ID + "/role/{role}")
    @RoleWith(Role.ADMIN)
    public User userUpdateRole(@PathVariable("id") long id, @PathVariable("role") Role role) {
        return this.userService.updateUserRole(id, role);
    }

    @PostMapping("/users/" + ID + "/lock/{timestamp}")
    @RoleWith(Role.ADMIN)
    public User userUpdateLock(@PathVariable("id") long id, @PathVariable("timestamp") long timestamp) {
        if (timestamp < 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "timestamp", "Invalid timestamp.");
        }
        return this.userService.updateUserLockedUntil(id, timestamp);
    }

    @PostMapping("/users/" + ID + "/password")
    @RoleWith(Role.ADMIN)
    public User userUpdatePassword(@PathVariable("id") long id, @RequestBody UpdatePasswordBean bean) {
        return this.userService.updateUserPassword(id, bean.password);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // wiki and wiki page
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/wikis")
    @RoleWith(Role.CONTRIBUTOR)
    public Map<String, List<Wiki>> wikis() {
        return Map.of(RESULTS, this.wikiService.getWikis());
    }

    @GetMapping("/wikis/" + ID)
    @RoleWith(Role.CONTRIBUTOR)
    public WikiWithContent wiki(@PathVariable("id") long id) {
        Wiki wiki = this.wikiService.getById(id);
        String content = this.textService.getById(wiki.textId).content;
        return new WikiWithContent(wiki, content);
    }

    @GetMapping("/wikis/" + ID + "/tree")
    @RoleWith(Role.CONTRIBUTOR)
    public Wiki wikiTree(@PathVariable("id") long id) {
        return this.wikiService.getWikiTree(id);
    }

    @PostMapping("/wikis")
    @RoleWith(Role.EDITOR)
    public Wiki wikiCreate(@RequestBody WikiBean bean) {
        Wiki wiki = this.wikiService.createWiki(HttpContext.getRequiredCurrentUser(), bean);
        this.wikiService.removeWikiFromCache(wiki.id);
        asyncIndex(wiki);
        return wiki;
    }

    @PostMapping("/wikis/" + ID)
    @RoleWith(Role.EDITOR)
    public Wiki wikiUpdate(@PathVariable("id") long id, @RequestBody WikiBean bean) {
        Wiki wiki = this.wikiService.updateWiki(HttpContext.getRequiredCurrentUser(), id, bean);
        this.wikiService.removeWikiFromCache(id);
        asyncIndex(wiki);
        return wiki;
    }

    @PostMapping("/wikis/" + ID + "/delete")
    @RoleWith(Role.EDITOR)
    public Map<String, Boolean> wikiDelete(@PathVariable("id") long id) {
        this.wikiService.deleteWiki(HttpContext.getRequiredCurrentUser(), id);
        this.wikiService.removeWikiFromCache(id);
        asyncUnindex(id);
        return API_RESULT_TRUE;
    }

    @PostMapping("/wikis/" + ID + "/wikiPages")
    @RoleWith(Role.EDITOR)
    public WikiPage wikiPageCreate(@PathVariable("id") long id, @RequestBody WikiPageBean bean) {
        Wiki wiki = this.wikiService.getById(id);
        WikiPage wikiPage = this.wikiService.createWikiPage(HttpContext.getRequiredCurrentUser(), wiki, bean);
        this.wikiService.removeWikiFromCache(id);
        asyncIndex(wiki, wikiPage);
        return wikiPage;
    }

    @GetMapping("/wikiPages/" + ID)
    @RoleWith(Role.EDITOR)
    public WikiPageWithContent wikiPage(@PathVariable("id") long id) {
        WikiPage wp = this.wikiService.getWikiPageById(id);
        String content = this.textService.getById(wp.textId).content;
        return new WikiPageWithContent(wp, content);
    }

    @PostMapping("/wikiPages/" + ID)
    @RoleWith(Role.EDITOR)
    public WikiPage wikiPageUpdate(@PathVariable("id") long id, @RequestBody WikiPageBean bean) {
        WikiPage wikiPage = this.wikiService.updateWikiPage(HttpContext.getRequiredCurrentUser(), id, bean);
        this.wikiService.removeWikiFromCache(wikiPage.wikiId);
        Wiki wiki = this.wikiService.getById(wikiPage.wikiId);
        asyncIndex(wiki, wikiPage);
        return wikiPage;
    }

    @PostMapping("/wikiPages/" + ID + "/move")
    @RoleWith(Role.EDITOR)
    public WikiPage wikiPageUpdate(@PathVariable("id") long wikiPageId, @RequestBody WikiPageMoveBean bean) {
        bean.validate(true);
        WikiPage wikiPage = this.wikiService.moveWikiPage(HttpContext.getRequiredCurrentUser(), wikiPageId, bean.parentId, bean.displayIndex);
        this.wikiService.removeWikiFromCache(wikiPage.wikiId);
        Wiki wiki = this.wikiService.getById(wikiPage.wikiId);
        asyncIndex(wiki, wikiPage);
        return wikiPage;
    }

    @PostMapping("/wikiPages/" + ID + "/delete")
    @RoleWith(Role.EDITOR)
    public Map<String, Boolean> wikiPageDelete(@PathVariable("id") long id) {
        WikiPage wikiPage = this.wikiService.deleteWikiPage(HttpContext.getRequiredCurrentUser(), id);
        this.wikiService.removeWikiFromCache(wikiPage.wikiId);
        asyncUnindex(id);
        return API_RESULT_TRUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // async index
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void asyncIndex(Article article) {
        new Thread(() -> {
            try {
                searcher.indexArticles(article);
            } catch (Exception e) {
                logger.error("index article failed.", e);
            }
        }).start();
    }

    private void asyncUnindex(long id) {
        new Thread(() -> {
            try {
                searcher.unindexSearchableDocument(id);
            } catch (Exception e) {
                logger.error("unindex failed.", e);
            }
        }).start();
    }

    private void asyncIndex(Wiki wiki) {
        new Thread(() -> {
            try {
                searcher.indexWiki(wiki);
            } catch (Exception e) {
                logger.error("index wiki failed.", e);
            }
        }).start();
    }

    private void asyncIndex(Wiki wiki, WikiPage wikiPage) {
        new Thread(() -> {
            try {
                searcher.indexWikiPages(wiki, List.of(wikiPage));
            } catch (Exception e) {
                logger.error("index wiki page failed.", e);
            }
        }).start();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // extended JSON-serializable bean
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static class ArticleWithContent extends Article {

        public String content;

        public ArticleWithContent(Article article, String content) {
            copyPropertiesFrom(article);
            this.content = content;
        }

    }

    public static class SinglePageWithContent extends SinglePage {

        public String content;

        public SinglePageWithContent(SinglePage singlePage, String content) {
            copyPropertiesFrom(singlePage);
            this.content = content;
        }

    }

    public static class WikiWithContent extends Wiki {

        public String content;

        public WikiWithContent(Wiki wiki, String content) {
            copyPropertiesFrom(wiki);
            this.content = content;
        }

    }

    public static class WikiPageWithContent extends WikiPage {

        public String content;

        public WikiPageWithContent(WikiPage wikiPage, String content) {
            copyPropertiesFrom(wikiPage);
            this.content = content;
        }

    }

    public static class NavigationMenu {

        public String name;
        public String url;

        public NavigationMenu(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    public static class UpdatePasswordBean {
        public String password;
    }

    private static final String RESULTS = "results";

    private static final Map<String, Boolean> API_RESULT_TRUE = Map.of("result", Boolean.TRUE);

}
