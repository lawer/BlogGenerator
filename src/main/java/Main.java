import XML.Author;
import XML.Entry;
import XML.Feed;
import XML.Link;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {
        Map<String, String> config = carregaConfiguracio("src/main/java/config.ini");
        ArrayList<Post> posts = carregaJson("src/main/java/posts.json");

        buildWeb(config, posts);
        buildRss(config, posts);
    }

    private static Map<String, String> carregaConfiguracio(String path) throws IOException {
        Path filePath = Path.of(path);

        try (Stream<String> s = Files.lines(filePath)) {
            return s.filter(l -> l.contains("="))
                    .map(l -> l.split("="))
                    .map(files -> new String[]{files[0].strip(), files[1].strip()})
                    .collect(
                            Collectors.toMap(kv -> kv[0], kv -> kv[1])
                    );
        }
    }

    private static ArrayList<Post> carregaJson(String path) throws IOException {
        Path filePath = Path.of(path);

        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerForListOf(Post.class);

        return reader.readValue(filePath.toFile());
    }

    private static void buildWeb(Map<String, String> config, ArrayList<Post> posts) throws IOException {
        buildIndexPage(config, posts);
        buildIndividualPages(config, posts);
    }

    private static String templateSubst(String template, Map<String, String> context) {
        Pattern pattern = Pattern.compile("@@(\\S*)@@", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(template);
        String out = template;

        while (matcher.find()) {
            String trobat = matcher.group();
            String key = matcher.group(1);

            out = out.replace(trobat, context.get(key));
        }

        return out;
    }

    private static void buildIndexPage(Map<String, String> config, ArrayList<Post> posts) throws IOException {
        Path indexTmplPath = Path.of("src/main/java/index_tmpl.html");
        Path indexOuthPath = Path.of("src/main/java/out/index.html");

        String template = Files.readString(indexTmplPath);

        String out = createPostList(template, posts);
        out = templateSubst(out, config);

        Files.writeString(indexOuthPath, out);
    }

    private static String createPostList(String template, ArrayList<Post> posts) {
        Pattern pattern = Pattern.compile("@#\\S*#@(.*)@#\\S*#@", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(template);
        String out = "";

        while (matcher.find()) {
            String trobat = matcher.group();
            String template_post = matcher.group(1);

            ArrayList<String> postTemplateList = new ArrayList<>();

            for (Post post : posts) {
                String text = templateSubst(template_post, post.asMap());
                postTemplateList.add(text);
            }
            String postList = String.join("", postTemplateList);

            out = template.replace(trobat, postList);
        }

        return out;
    }

    private static void buildIndividualPages(Map<String, String> config, ArrayList<Post> posts) throws IOException {
        Path indexTmplPath = Path.of("src/main/java/post_tmpl.html");
        String template = Files.readString(indexTmplPath);

        for (Post post : posts) {
            Path indexOuthPath = Path.of(
                    String.format("src/main/java/out/%s.html", post.getId())
            );

            String out = templateSubst(template, post.asMap());

            Files.writeString(indexOuthPath, out);
        }
    }

    private static void buildRss(Map<String, String> config, ArrayList<Post> posts) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();

        Feed feed = new Feed();
        feed.setXmlns("http://www.w3.org/2005/Atom");
        feed.setTitle(config.get("titol"));
        feed.setSubtitle(config.get("tematica"));
        feed.setLink(new Link("http://localhost/"));
        feed.setUpdated(Instant.now().toString());
        feed.setId("http://localhost");

        ArrayList<Entry> entries = feed.getEntries();

        for (Post post : posts) {
            Entry entry = new Entry();
            entry.setTitle(post.getTitle());
            entry.setSummary(post.getBody());
            entry.setId(String.format("http://localhost/%s.html/", post.getId()));
            entry.setLink(new Link(String.format("http://localhost/%s.html/", post.getId())));
            entry.setUpdated(Instant.now().toString());

            Author author = new Author();
            author.setName("Pepito LosPalotes");
            author.setEmail("ppito@lospalotes.com");

            entry.setAuthor(author);

            entries.add(entry);
        }

        String out = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(feed);

        Path xmlOutPath = Path.of("src/main/java/out/feed.xml");
        Files.writeString(xmlOutPath, out);
    }
}
