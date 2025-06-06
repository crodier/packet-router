package other;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * // This is the HtmlParser's API interface.
 * // You should not implement it, or speculate about its implementation
 * interface HtmlParser {
 *     public List<String> getUrls(String url) {}
 * }
 */
class Solution {

    private static class HtmlParser {
        private List<String> getUrls(String url) {
            if (url.equals("yahoo")) {
                return List.of("yahoo.com/news", "yahoo.com/sports");
            }
            return null;
        }
    }

    private Map<String, String> seen = new ConcurrentHashMap<>();

    private ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private Queue<Future> futureQueue = new ConcurrentLinkedQueue<>();

    public List<String> crawl(String startUrl, HtmlParser htmlParser) throws ExecutionException, InterruptedException {
        if (seen.containsKey(startUrl)) {
            return null;
        }

        seen.put(startUrl, startUrl);
        List<String> links = htmlParser.getUrls(startUrl);

        // this will block on es.submit
        Future future = es.submit(() -> {
            for (String link : links) {
                if (seen.containsKey(link)) {
                    continue;
                }
                seen.put(link, link);
                crawl(link, htmlParser);
            }

            return links;
        });
        futureQueue.add(future);

        return new ArrayList<>(seen.keySet());
    }
}