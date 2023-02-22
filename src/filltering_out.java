import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class filltering_out {
    private static Map<String, String> le_words = new HashMap<>();

    public static void main (String[] args) throws IOException {
        BufferedReader reader_w = new BufferedReader(new FileReader("opennlp-en-lemmatizer-dict.txt")); // loading lemmatizer words
        String w = reader_w.readLine();
        while (w != null) {
            String[] t = w.split("\\s+"); // tokenizing
            if (t[1].contains("NNS")){
                le_words.put(t[0], t[2]);
            }
            w = reader_w.readLine();
        }
        //System.out.println(le_words.size());
        FileWriter fw = new FileWriter("opennlp-en-lemmatizer-dict-NNS.txt"); // building the graph file
        System.out.println("Writing the lemmatizer file...");
        le_words.forEach((k,v) -> {
            try {
                fw.write(k + " " + v +"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        fw.close();
    }
}
