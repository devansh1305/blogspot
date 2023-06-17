package com.blogspot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.blogspot.bean.setting.Website;
import com.blogspot.enums.RefType;
import com.blogspot.enums.ResourceEncoding;
import com.blogspot.enums.Role;
import com.blogspot.model.AbstractEntity;
import com.blogspot.model.Article;
import com.blogspot.model.Attachment;
import com.blogspot.model.Board;
import com.blogspot.model.Category;
import com.blogspot.model.Headline;
import com.blogspot.model.LocalAuth;
import com.blogspot.model.Navigation;
import com.blogspot.model.OAuth;
import com.blogspot.model.Reply;
import com.blogspot.model.Resource;
import com.blogspot.model.Setting;
import com.blogspot.model.SinglePage;
import com.blogspot.model.Text;
import com.blogspot.model.Topic;
import com.blogspot.model.User;
import com.blogspot.model.Wiki;
import com.blogspot.model.WikiPage;
import com.blogspot.util.HashUtil;

/**
 * Generate database schema for API server.
 */
public class SchemaBuilder {

    static final Random random = new Random(1234567890L);
    static ZonedDateTime base = LocalDateTime.of(2022, 2, 22, 22, 22, 22).atZone(ZoneId.of("Z"));

    static long currentTimeMillis() {
        base = base.plusDays(1).plusHours(9).plusMinutes(18);
        return base.toEpochSecond() * 1000L;
    }

    public static void main(String[] args) throws Exception {
        SchemaBuilder builder = new SchemaBuilder();
        String ddl = builder.generateDDL();
        String inserts = builder.generateEntities();
        File ddlFile = new File("dev/sql/ddl.sql").getAbsoluteFile();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(ddlFile), "UTF-8"))) {
            writer.write(ddl);
        }
        System.out.println("\nGenerated SQL:\n" + ddl);
        System.out.println("\nRun generated SQL:\n\nmysql -u root --password=password < " + ddlFile);
    }

    String dbName = "it";
    String dbUser = "root";
    String dbPassword = "password";
    String loginPassword = "password";
    User admin;
    User editor;
    User subscriber;
    long[] imageIds;

    AtomicLong nextId = new AtomicLong();

    SchemaBuilder() {

        db.setBasePackages(List.of("com.blogspot.model"));
        db.init();
    }

    String generateTable(Class<?> clazz) {
        Set<Class<?>> classesWithUtf8 = Set.of(OAuth.class, LocalAuth.class, Resource.class);
        String charset = classesWithUtf8.contains(clazz) ? "UTF8" : "UTF
                MB4";
        return db.getDDL(clazz).replace(" BIT ", " BOOL ").replace(");", ") Engine=INNODB DEFAULT CHARSET=" + charset + ";\n");
    }

    String generateDDL() {
        String[] tables = db.getEntities().stream().map(this::generateTable).sorted().toArray(String[]::new);
        String schema = String.join("\n", tables);
        StringBuilder sb = new StringBuilder(4096);
        sb.append("\n\n-- BEGIN generate DDL --\n\n");
        sb.append(String.format("DROP DATABASE IF EXISTS %s;\n\n", dbName));
        sb.append(String.format("CREATE DATABASE %s;\n\n", dbName));
        if (!"root".equals(dbUser)) {
            sb.append(String.format("CREATE USER IF NOT EXISTS %s@'%%' IDENTIFIED BY '%s';\n\n", dbUser, dbPassword));
            sb.append(String.format("GRANT SELECT,INSERT,DELETE,UPDATE ON %s.* TO %s@'%%' WITH GRANT OPTION;\n\n",
                    dbName, dbUser));
            sb.append(String.format("FLUSH PRIVILEGES;\n\n"));
        }
        sb.append(String.format("USE %s;\n\n", dbName));
        sb.append(schema);
        sb.append("\n-- END generate DDL --\n");
        return sb.toString();
    }

    String generateEntities() {
        List<AbstractEntity> entities = new ArrayList<>();
        initUsers(entities);
        initSinglePages(entities);
        initHeadlines(entities);
        initArticles(entities);
        initDiscuss(entities);
        initWiki(entities);

        Navigation nav = new Navigation();
        nav.name = "Discuss";
        nav.icon = "commenting-o";
        nav.url = "/discuss";
        nav.displayOrder = 5;
        entities.add(nav);

        nav = new Navigation();
        nav.name = "External";
        nav.icon = "external-link";
        nav.url = "https://weibo.com/";
        nav.displayOrder = 6;
        nav.blank = true;
        entities.add(nav);

        Setting s = new Setting();
        s.settingGroup = Website.class.getSimpleName();
        s.settingKey = "name";
        s.settingValue = "blogspot";
        entities.add(s);

        StringBuilder sb = new StringBuilder(102400);
        sb.append("-- generated initial data\n\n");
        sb.append("USE it;\n\n");
        entities.forEach(entity -> {
            if (entity.id == 0) {
                entity.id = nextId.incrementAndGet();
            }
            if (entity.createdAt == 0) {
                entity.createdAt = entity.updatedAt = currentTimeMillis();
            }

            sb.append(generateInsert(db.getTable(entity.getClass()), db.getInsertableFields(entity.getClass()),
                    db.getInsertableValues(entity)));
        });
        return sb.toString();
    }

    void initSinglePages(List<AbstractEntity> entities) {
        Text text = initText(entities);
        SinglePage page = new SinglePage();
        page.id = nextId.incrementAndGet();
        page.name = "Help";
        page.tags = "help,doc";
        page.textId = text.id;
        page.publishAt = currentTimeMillis();
        entities.add(page);

        Navigation nav = new Navigation();
        nav.name = page.name;
        nav.icon = "plane";
        nav.url = "/single/" + page.id;
        nav.displayOrder = 3;
        entities.add(nav);
    }

    void initUsers(List<AbstractEntity> entities) {

        List<String> emails = Arrays.stream(Role.values()).map(r -> r.name().toLowerCase() + "@blogspot.com")
                .collect(Collectors.toList());
        // insert users with password:
        emails.forEach(email -> {
            User user = new User();
            user.id = nextId.incrementAndGet();
            user.email = email;
            user.name = email.substring(0, email.indexOf('@'));
            user.role = Role.valueOf(user.name.toUpperCase());
            user.lockedUntil = user.role == Role.ADMIN ? 0 : 10000000000000L;
            user.imageUrl = "/avatar/" + HashUtil.sha1(user.name);
            entities.add(user);
            if (user.role == Role.ADMIN) {
                this.admin = user;
            }
            if (user.role == Role.EDITOR) {
                this.editor = user;
            }
            if (user.role == Role.SUBSCRIBER) {
                this.subscriber = user;
            }
            final String hashedPasswd = HashUtil.hmacSha256(loginPassword, user.email);
            LocalAuth auth = new LocalAuth();
            auth.id = nextId.incrementAndGet();
            auth.userId = user.id;
            auth.salt = HashUtil.sha256(user.email);
            auth.passwd = HashUtil.hmacSha256(hashedPasswd, auth.salt);
            entities.add(auth);
        });
    }

    void initHeadlines(List<AbstractEntity> entities) {
        Navigation nav = new Navigation();
        nav.name = "Headlines";
        nav.icon = "calendar";
        nav.url = "/headline";
        nav.displayOrder = 0;
        entities.add(nav);

        for (int n = 0; n < 25; n++) {
            Headline h = new Headline();
            h.userId = admin.id;
            h.name = randomLine(1) + " " + n;
            h.published = n < 15;
            h.publishAt = currentTimeMillis();
            h.description = randomLine(30);
            h.url = "https://www.google.com/search?q=headline+" + n;
            entities.add(h);
        }
    }

    void initArticles(List<AbstractEntity> entities) {
        Category category = new Category();
        category.id = nextId.incrementAndGet();
        category.name = "Sample";
        category.tag = "sample";
        category.description = "Java Series";
        entities.add(category);

        Navigation nav = new Navigation();
        nav.name = category.name;
        nav.icon = "coffee";
        nav.url = "/category/" + category.id;
        nav.displayOrder = 1;
        entities.add(nav);

        initAttachments(entities);

        for (int n = 0; n < imageIds.length; n++) {
            Text t = initText(entities);
            Article a = new Article();
            a.userId = admin.id;
            a.categoryId = category.id;
            a.publishAt = currentTimeMillis();
            a.name = randomLine(1) + " " + n;
            a.description = randomLine(10);
            a.tags = "abc,xyz,hello";
            a.textId = t.id;
            a.imageId = imageIds[n];
            entities.add(a);
        }
    }

    Text initText(List<AbstractEntity> entities) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("# ").append(randomLine(10)).append("\n\n");
        int lines = (int) (random.nextDouble() * 10) + 10;
        for (int i = 0; i < lines; i++) {
            sb.append(randomLine(20)).append("\n\n");
        }
        String content = sb.toString();
        Text t = new Text();
        t.id = nextId.incrementAndGet();
        t.content = content;
        t.hash = HashUtil.sha256(content);
        entities.add(t);
        return t;
    }

    void initWiki(List<AbstractEntity> entities) {
        Wiki wiki = new Wiki();
        wiki.id = nextId.incrementAndGet();
        wiki.description = "A Sample Wiki";
        wiki.imageId = imageIds[0];
        wiki.name = "Sample Tutorial";
        wiki.tag = "sample";
        wiki.textId = initText(entities).id;
        wiki.userId = admin.id;
        entities.add(wiki);

        for (int i = 0; i < 3; i++) {
            WikiPage p = new WikiPage();
            p.id = nextId.incrementAndGet();
            p.wikiId = wiki.id;
            p.displayOrder = i;
            p.name = "page " + (i + 1);
            p.parentId = wiki.id;
            p.textId = initText(entities).id;
            p.publishAt = currentTimeMillis();
            entities.add(p);

            for (int j = 0; j < 4; j++) {
                WikiPage sub = new WikiPage();
                sub.id = nextId.incrementAndGet();
                sub.wikiId = wiki.id;
                sub.displayOrder = j;
                sub.name = "sub " + (j + 1);
                sub.parentId = p.id;
                sub.textId = initText(entities).id;
                sub.publishAt = currentTimeMillis();
                entities.add(sub);

                if (j == 2) {
                    for (int n = 0; n < 3; n++) {
                        WikiPage leaf = new WikiPage();
                        leaf.id = nextId.incrementAndGet();
                        leaf.wikiId = wiki.id;
                        leaf.displayOrder = n;
                        leaf.name = "leaf " + (n + 1);
                        leaf.parentId = sub.id;
                        leaf.textId = initText(entities).id;
                        leaf.publishAt = currentTimeMillis();
                        entities.add(leaf);
                    }
                }
            }
        }

        Navigation nav = new Navigation();
        nav.name = wiki.name;
        nav.icon = "magic";
        nav.url = "/wiki/" + wiki.id;
        nav.displayOrder = 3;
        entities.add(nav);
    }

    void initDiscuss(List<AbstractEntity> entities) {
        Board board = new Board();
        board.id = nextId.incrementAndGet();
        board.name = "Discuss Sample";
        board.description = "Discuss Sample.";
        board.tag = "sample";
        board.topicNumber = 20;
        entities.add(board);

        for (int i = 0; i < board.topicNumber; i++) {
            Topic topic = new Topic();
            topic.id = nextId.incrementAndGet();
            topic.boardId = board.id;
            topic.content = randomLine(50);
            topic.userId = subscriber.id;
            topic.userName = subscriber.name;
            topic.userImageUrl = subscriber.imageUrl;
            topic.createdAt = topic.updatedAt = currentTimeMillis();
            topic.name = randomLine(3) + (i + 1);
            topic.refId = 0;
            topic.refType = RefType.NONE;
            topic.replyNumber = i;
            entities.add(topic);

            for (int j = 0; j < topic.replyNumber; j++) {
                Reply reply = new Reply();
                reply.topicId = topic.id;
                reply.userId = subscriber.id;
                reply.userName = subscriber.name;
                reply.userImageUrl = subscriber.imageUrl;
                reply.createdAt = reply.updatedAt = currentTimeMillis();
                reply.content = randomLine(50);
                entities.add(reply);
            }
        }
    }

    void initAttachments(List<AbstractEntity> entities) {
        imageIds = new long[12];
        for (int i = 0; i < imageIds.length; i++) {
            Resource r = new Resource();
            r.id = nextId.incrementAndGet();
            r.encoding = ResourceEncoding.BASE64;
            r.content = IMGS[i];
            r.hash = HashUtil.sha256(r.content);
            entities.add(r);

            Attachment a = new Attachment();
            a.id = nextId.incrementAndGet();
            a.width = 640;
            a.height = 360;
            a.mime = "image/jpeg";
            a.name = "img-" + i;
            a.resourceId = r.id;
            a.size = r.content.length() / 4 * 3;
            a.userId = admin.id;
            entities.add(a);

            imageIds[i] = a.id;
        }
    }
}
