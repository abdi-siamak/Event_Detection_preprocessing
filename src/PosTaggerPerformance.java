import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import java.util.Arrays;
public class PosTaggerPerformance {
    public static List<String> main(final String sentence) throws IOException {
        List<String> POS_nouns = new ArrayList<String>();
        //Loading Parts of speech-maxent model
        InputStream inputStream = new FileInputStream("OpenNLP_models/en-pos-maxent.bin");
        POSModel model = new POSModel(inputStream);

        //Creating an object of WhitespaceTokenizer class
        WhitespaceTokenizer whitespaceTokenizer= WhitespaceTokenizer.INSTANCE;

        //Tokenizing the sentence
        ArrayList<String> tokens = new ArrayList<>();
        for (String token : whitespaceTokenizer.tokenize(sentence)) {
            tokens.add(token);
        }

        Iterator<String> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            String x = iterator.next();
            if (x.startsWith("@") && x.length()>1){
                if(!EventDetection.mentions.contains(x) && EventDetection.KR){
                    EventDetection.mentions.add(x);
                }
                iterator.remove();
                EventDetection.removedMentions++;
            }else if(x.startsWith("#") && x.length()>1) {
                if (!EventDetection.hashtags.containsKey(x) && EventDetection.KR) {
                    EventDetection.hashtags.put(x, 1);
                } else if (EventDetection.hashtags.containsKey(x) && EventDetection.KR) {
                    EventDetection.hashtags.put(x, EventDetection.hashtags.get(x) + 1);
                }
                iterator.remove();
                EventDetection.removedHashtags++;
            }else if(EventDetection.stopwords.contains(x) && EventDetection.ST){
                iterator.remove();
                EventDetection.removedStopwords++;
            }else if(x.length()<4 || x.length()>21){
                iterator.remove();
                EventDetection.removedUnwantedLength++;
            }
        }

        //Instantiating POSTaggerME class
        POSTaggerME tagger = new POSTaggerME(model);
        //Generating tags
        // Convert ArrayList<String> to String[]
        String[] tokensArray = tokens.toArray(new String[tokens.size()]);
        String[] tags = tagger.tag(tokensArray);
        //Instantiating POSSample class
        //POSSample sample = new POSSample(tokens, tags);
        for(int i=0;i<tokensArray.length;i++){ // applying POS
            //System.out.println(tokens[i]+"\t:\t"+tags[i]);
            if (tags[i].contains("NN")){
                tokensArray[i] = tokensArray[i].replaceAll("[^a-zA-Z0-9\\s]+", "");
                if(tags[i].equals("NNS") && EventDetection.lemmatizerWords.get(tokensArray[i]) != null){
                    if (EventDetection.FW && !EventDetection.filterOutWords.contains(EventDetection.lemmatizerWords.get(tokensArray[i]))){
                        POS_nouns.add(EventDetection.lemmatizerWords.get(tokensArray[i]));
                    }else{
                        EventDetection.removedFilteredOutWords++;
                    }
                }else {
                    if (EventDetection.FW && !EventDetection.filterOutWords.contains(tokensArray[i])){
                        POS_nouns.add(tokensArray[i]);
                    }else{
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