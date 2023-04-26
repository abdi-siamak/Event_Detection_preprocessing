import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import java.util.Arrays;
public class PosTaggerPerformance {
    public static List<String> main(String sentence) throws IOException {
        List<String> POS_nouns = new ArrayList<String>();
        //Loading Parts of speech-maxent model
        InputStream inputStream = new FileInputStream("OpenNLP_models/en-pos-maxent.bin");
        POSModel model = new POSModel(inputStream);

        //Creating an object of WhitespaceTokenizer class
        WhitespaceTokenizer whitespaceTokenizer= WhitespaceTokenizer.INSTANCE;

        //Tokenizing the sentence
        //String sentence = "Brexit was a very big topic!";
        String[] tokens = whitespaceTokenizer.tokenize(sentence);
        /*
        for (String x: tokens){
            System.out.println("1" + x);
        }
         */
        for(String x:tokens){
            if (x.startsWith("@") && x.length()>1){
                if(!EventDetection.findMatch(EventDetection.mentions, x) && EventDetection.KR){
                    EventDetection.mentions.add(x);
                }
                List<String> List = new ArrayList<>(Arrays.asList(tokens));
                List.remove(x);
                tokens = List.toArray(new String[0]);
                EventDetection.removedMentions++;
            }else if(x.startsWith("#") && x.length()>1) {
                if (!EventDetection.hashtags.containsKey(x) && EventDetection.KR) {
                    EventDetection.hashtags.put(x, 1);
                } else if (EventDetection.hashtags.containsKey(x) && EventDetection.KR) {
                    EventDetection.hashtags.put(x, EventDetection.hashtags.get(x) + 1);
                }
                List<String> List_2 = new ArrayList<>(Arrays.asList(tokens));
                List_2.remove(x);
                tokens = List_2.toArray(new String[0]);
                EventDetection.removedHashtags++;
            }else if(EventDetection.stopwords.containsKey(x)){
                List<String> List_3 = new ArrayList<>(Arrays.asList(tokens));
                List_3.remove(x);
                tokens = List_3.toArray(new String[0]);
                EventDetection.removedStopwords++;
            }else if(x.length()<4 || x.length()>21){
                List<String> List_4 = new ArrayList<>(Arrays.asList(tokens));
                List_4.remove(x);
                tokens = List_4.toArray(new String[0]);
                EventDetection.removedUnwantedLength++;
            }
        }
        /*
        for (String x: tokens){
            System.out.println("2" + x);
        }
         */
        //Instantiating POSTaggerME class
        POSTaggerME tagger = new POSTaggerME(model);
        //Generating tags
        String[] tags = tagger.tag(tokens);
        //Instantiating POSSample class
        //POSSample sample = new POSSample(tokens, tags);
        for(int i=0;i<tokens.length;i++){
            //System.out.println(tokens[i]+"\t:\t"+tags[i]);
            if (tags[i].contains("NN")){
                tokens[i] = tokens[i].replaceAll("[^a-zA-Z0-9\\s]+", "");
                if(tags[i].equals("NNS") && EventDetection.lemmatizerWords.get(tokens[i]) != null){
                    if (!EventDetection.filterOutWords.containsKey(EventDetection.lemmatizerWords.get(tokens[i]))){
                        POS_nouns.add(EventDetection.lemmatizerWords.get(tokens[i]));
                        EventDetection.removedFilteredOutWords++;
                    }
                }else {
                    if (!EventDetection.filterOutWords.containsKey(tokens[i])){
                        POS_nouns.add(tokens[i]);
                        EventDetection.removedFilteredOutWords++;
                    }
                }
            }else{
                EventDetection.removedNon_Nouns++;
            }
        }
        //System.out.println(sample.toString());
        //System.out.println("1" +POS_nouns);
        //Monitoring the performance of POS tagger
        //PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
        //perfMon.start();
        //perfMon.incrementCounter();
        //perfMon.stopAndPrintFinalResult();
        return POS_nouns;
    }
    // Function to remove the element
}