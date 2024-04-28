import java.io.*;
import java.util.*;
import parcs.*;

public class WordCount implements AM {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Number of workers not specified.");
            System.exit(1);
        }

        int numWorkers = Integer.parseInt(args[0]);
        task curtask = new task();
        curtask.addJarFile("WordCount.jar");
        AMInfo info = new AMInfo(curtask, null);

        System.err.println("Reading input...");
        String inputText = "";
        try {
            Scanner sc = new Scanner(new File(info.curtask.findFile("input.txt")));
            while (sc.hasNextLine()) {
                inputText += sc.nextLine() + " ";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int len = inputText.length();
        int partLength = (len + numWorkers - 1) / numWorkers;

        System.err.println("Forwarding parts to workers...");
        channel[] channels = new channel[numWorkers];
        long startTime = System.nanoTime(); // Start timing

        for (int i = 0; i < numWorkers; i++) {
            int start = i * partLength;
            int end = Math.min((i + 1) * partLength, len);
            String substring = inputText.substring(start, end);

            point p = info.createPoint();
            channel c = p.createChannel();
            p.execute("WordCount");
            c.write(substring);
            channels[i] = c;
        }

        System.err.println("Getting results from workers...");
        Map<String, Integer> globalCounts = new HashMap<>();
        for (int i = 0; i < numWorkers; i++) {
            Map<String, Integer> localCounts = (Map<String, Integer>) channels[i].readObject();
            mergeCounts(globalCounts, localCounts);
        }

        long endTime = System.nanoTime(); // End timing
        long duration = (endTime - startTime) / 1_000_000; // Convert ns to ms

        System.err.println("Result:");
        for (Map.Entry<String, Integer> entry : globalCounts.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        System.err.println("Time passed: " + duration + " ms.");

        curtask.end();
    }

    public void run(AMInfo info) {
        String textChunk = (String) info.parent.readObject();
        Map<String, Integer> wordCounts = countWords(textChunk);
        info.parent.write((Serializable) wordCounts);;
    }

    private static Map<String, Integer> countWords(String text) {
        Map<String, Integer> counts = new HashMap<>();
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                counts.put(word, counts.getOrDefault(word, 0) + 1);
            }
        }
        return counts;
    }

    private static void mergeCounts(Map<String, Integer> global, Map<String, Integer> local) {
        for (Map.Entry<String, Integer> entry : local.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();
            global.put(word, global.getOrDefault(word, 0) + count);
        }
    }
}
