import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

public class read
{
    public static void main(String[] args) throws Exception
    {
        BufferedReader reader_1 = new BufferedReader(new FileReader("stopwords.txt")); // loading the stopwords file
        String l = reader_1.readLine();
        List<String> stopwords = new ArrayList<>();
        while (l != null) {
            stopwords.add(l);
            l = reader_1.readLine();
        }

        Set<String> dictionary = new HashSet<String>(); // generating dictionary
        List<String> BOW = new ArrayList<>(); // generating BOW
        BufferedReader reader = new BufferedReader(new FileReader("Dataset/2016-06-24_18-57-56_10000tw.txt")); // loading the Dataset file
        String line = reader.readLine();
        while (line != null) {
            Object obj = new JSONParser().parse(line);
            // typecasting obj to JSONObject
            JSONObject jo = (JSONObject) obj;
            // getting firstName and lastName
            try {
                //  Block of code to try
                String tweet = (String) jo.get("text"); // getting tweets
                List<String> tokens = getTokens(tweet); // tokenizing
                Set<String> hSet = new HashSet<String>(); // converting to a set
                for (String x : tokens)
                    hSet.add(x.toLowerCase());

                if (hSet.contains(stopwords)) { // removing stopwords
                    //System.out.println("YES");
                    hSet.remove(stopwords);
                }
                dictionary.addAll(hSet); // adding to the dictionary
                BOW.add(hSet.toString()); // adding to the BOW


                //System.out.println(tokens);
                //System.out.println(hSet);
                //System.out.println(dictionary);
                //System.out.println(BOW);

                line = reader.readLine(); // read next line
            }
            catch(Exception e) {
                //  Block of code to handle errors
                line = reader.readLine(); // read next line
            }
        }
        List<String> dict = new ArrayList<>(dictionary);
        //System.out.println(dict);
        //System.out.println(BOW.get(0));
        //System.out.println(BOW.get(0).contains("@brexit"));

        FileWriter fw = new FileWriter("graph.txt");
        float percentage;
        for (int i=0;i<dict.size();i++){
            percentage = (float) i*100 / dict.size();
            System.out.println("building the graph: "+percentage+" %");
            for (int j=i+1;j<dict.size();j++){
                int times = 0;
                for(int t=0;t<BOW.size();t=t+1){
                    if(BOW.get(t).contains(dict.get(i)) && BOW.get(t).contains(dict.get(j))){
                        times = times + 1;
                    }
                }
                if(times>0) {
                    fw.write(i + " ");
                    fw.write(j + " ");
                    fw.write(((int) times) + "\n");
                }
            }
        }
        fw.close();
    }
    public static List<String> getTokens(String str) {
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(str, " ");
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
            //System.out.println(tokenizer.nextToken());
        }
        return tokens;
    }
}
