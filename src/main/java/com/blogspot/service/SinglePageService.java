package com.blogspot.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.blogspot.bean.SinglePageBean;
import com.blogspot.common.ApiException;
import com.blogspot.enums.ApiError;
import com.blogspot.enums.Role;
import com.blogspot.model.SinglePage;
import com.blogspot.model.User;
import com.blogspot.web.filter.HttpContext;

@Component
public class SinglePageService extends AbstractDbService<SinglePage> {

    @Autowired
    TextService textService;

    private static final String KEY_SINGLE_PAGES = "__singlepages__";

    public List<SinglePage> getAll() {
        return this.db.from(SinglePage.class).orderBy("publishAt").desc().orderBy("id").desc().list();
    }

    public SinglePage getPublishedById(long id) {
        SinglePage sp = this.redisService.hget(KEY_SINGLE_PAGES, id, SinglePage.class);
        if (sp == null) {
            sp = getById(id);
            this.redisService.hset(KEY_SINGLE_PAGES, id, sp);
        }
        if (sp.publishAt > System.currentTimeMillis()) {
            User user = HttpContext.getCurrentUser();
            if (user != null && user.role.value > Role.CONTRIBUTOR.value) {
                throw new ApiException(ApiError.ENTITY_NOT_FOUND, "SinglePage", "SinglePage not found.");
            }
        }
        return sp;
    }

    @Transactional
    public SinglePage createSinglePage(SinglePageBean bean) {
        bean.validate(true);
        SinglePage sp = new SinglePage();
        sp.name = bean.name;
        sp.publishAt = bean.publishAt;
        sp.tags = bean.tags;
        sp.textId = textService.createText(bean.content).id;
        this.db.insert(sp);
        return sp;
    }

    @Transactional
    public void deleteSinglePage(long id) {
        SinglePage sp = this.getById(id);
        this.db.remove(sp);
    }

    @Transactional
    public SinglePage updateSinglePage(long id, SinglePageBean bean) {
        bean.validate(false);
        SinglePage sp = this.getById(id);
        sp.name = bean.name;
        sp.publishAt = bean.publishAt;
        sp.tags = bean.tags;
        if (bean.content != null) {
            sp.textId = textService.createText(bean.content).id;
        }
        this.db.update(sp);
        return sp;
    }

    public void deleteSinglePageFromCache(long id) {
        this.redisService.hdel(KEY_SINGLE_PAGES, id);
    }
}
