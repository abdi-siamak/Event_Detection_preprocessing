import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
/**
 * File: EventDetection.java
 * Description: Implements Preprocessing and Pruning Steps on Text Documents Like Tweets.
 * Author: Siamak Abdi
 * Date: April 30, 2024
 */
public class EventDetection {
    private static BiMap<String, Integer> dictionaryList = HashBiMap.create(); // dictionary-unique words (word -> index)
    private static Map<List<Integer>, Integer> graph = new HashMap<>(); // graph structure (edge -> weight) ([source node, destination node] -> number of occurrences)
    private static Map<Integer, List<Integer>> DicInformation = new HashMap<>(); // Dictionary Excel file (node -> [number of edges, sum of weights, max weight])
    private static Map<Integer, Integer> histogram = new HashMap<>(); // histogram of weights (weight -> number of occurrences)
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static Integer maxWeight = 0;
    private static Boolean RT; // switch for retweets
    public static Boolean ST; // switch for stopwords
    private static Boolean POS; // switch for activating POS
    private static Boolean Pru; //switch for activating pruning methods
    public static Boolean KR; // switch for saving removed words
    public static Boolean FW; // switch for removing words that don't represent a specific topic
    private static boolean MAP; // switch for mapping the graph to sequential order ids (to correspond with CHkS algorithm)
    private static int index = 0; // index of words in the dictionary
    public static Set<String> stopwords = new HashSet<>();
    public static Map<String, String> lemmatizerWords = new HashMap<>(); // (word -> lemmatized word)
    public static Set<String> filterOutWords = new HashSet<>();
    public static Map<String, Integer> hashtags = new HashMap<>(); // for keeping removed hashtags (hashtag -> number of occurrences)
    public static Set<String> mentions = new HashSet<>(); // for keeping removed mentions
    private static Map<String, Integer> wordFrequency = new HashMap<>(); //(word -> number of occurrences)
    public static int removedUnwantedLength = 0; // counter for removed words with unwanted length
    public static int removedStopwords = 0; // counter for removed stopwords
    private static int removedRetweets = 0; // counter for removed retweets
    public static int removedHashtags = 0; // counter for removed hashtags
    public static int removedMentions = 0; // counter for removed mentions
    private static int removedNonEnglishTweets = 0; // counter for removed non-English tweets
    public static int removedNon_Nouns = 0; // counter for removed non-nouns
    public static int removedFilteredOutWords = 0; // counter for removed filtered out words
    //private static HashMap<String, String> frequentHashtags = new HashMap<>(); //
    private static HashMap<Integer, Integer> idMap = new HashMap<>(); // (old id -> new id)
    private static int lines = 0; // counter for number of all tweets
    private interface myInterface {
        void building() throws Exception;
    }
    private static ArrayList<String> loading (final String pathTweets, final String pathStopwords, final String pathLemmatizers, final String pathFilteredOut) throws IOException {
        System.out.println("Loading ...");
        if (ST){
            BufferedReader reader = new BufferedReader(new FileReader(pathStopwords)); // loading the stopwords file
            String l = reader.readLine();
            while (l != null) {
                stopwords.add(l);
                l = reader.readLine();
            }
            //System.out.println(stopwords);
        }
        if (POS){
            BufferedReader reader = new BufferedReader(new FileReader(pathLemmatizers)); // loading the lemmatizer words
            String w = reader.readLine();
            while (w != null) {
                String[] t = w.split("\\s+"); // tokenizing
                lemmatizerWords.put(t[0], t[1]);
                w = reader.readLine();
            }
            //System.out.println(lemmatizerWords);
        }
        if (FW){
            BufferedReader reader = new BufferedReader(new FileReader(pathFilteredOut)); // loading the filter out words
            String w = reader.readLine();
            while (w != null) {
                filterOutWords.add(w);
                w = reader.readLine();
            }
            //System.out.println(filterOutWords);
        }
        //////////////////////////////////////////////////////////////////////////////////// loading tweets
        File dir = new File(pathTweets);
        String[] folders = dir.list(); // list of folders
        ArrayList<String> files = new ArrayList<>(); // list of files within folders
        for (String folder:folders){
            if (folder.startsWith("disc")){
                File subDir = new File(pathTweets + "/" +folder);
                for (String file : subDir.list()){
                    files.add(pathTweets + folder + "/" + file);
                }
            }
        }
        lines = getNumOfLines(files); // getting the number of all tweets
        return files;
    }
    public static void graphBuilding(final String month, final Integer day, final PrintWriter outputRunning, final ArrayList<String> files) throws Exception {
        int iter = 1;
        float percentage;
        String line;
        Object obj;
        System.out.println("Building the graph: " + "0 %");
        for (String file : files) {
            if (file.endsWith(".txt")){
                //System.out.println("Reading file: " + file);
                //outputRunning.print("\n Reading file: " + file);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                ///////////////////////////////////////////////////////////////////////////////////
                while ((line = reader.readLine()) != null) {
                    try {
                        obj = new JSONParser().parse(line);
                    } catch (ParseException e) {
                        continue;
                    } finally {
                        ////////////////////////////////////////////////////////////////
                        percentage = (float) iter * 100 / lines;
                        //long startTime = System.nanoTime();
                        if (percentage % 1 == 0) {
                            System.out.println("Building the graph: " + percentage + " %");
                            //outputRunning.print("\nBuilding the graph: " + percentage + " %");
                        }
                        iter = iter + 1;
                        /////////////////////////////////////////////////////////////////
                    }
                    // typecasting obj to JSONObject
                    JSONObject jo = (JSONObject) obj;
                    ////////////////////////////////////// Preprocessing steps
                    myInterface i = () -> {
                        String tweet = (String) jo.get("text"); // getting tweets
                        ArrayList<String> tokens = null;
                        if (POS) { // applying part of speech method
                            tweet = tweet.replaceAll("http\\p{L}+", "") // 1. removing links (string that starts with "http" followed by one or more letters from any language)
                                    .replaceAll("[^a-zA-Z'@# \\s]+", "") //2. removing any sequence of characters that is not a letter: (both lowercase and uppercase), a single quote, "@" or "#", or whitespace.
                                    .toLowerCase() // 3. lowercasing words
                                    .replaceAll("\\b[tT][cC][oO]\\w*\\b", ""); // 4. removing any word that starts with "tco", "Tco", "tCo", or "TCo" (case-insensitive), followed by zero or more word characters.
                            //tokens = PosTaggerPerformance.main(tweet).toArray(String[]::new);
                            List<String> tokenList = PosTaggerPerformance.main(tweet); // 5. applying POS (tokenizing)
                            tokens = new ArrayList<>(tokenList); // tokenizing + [removing mentions, removing hashtags, removing stopwords, removing words with unwanted length, and removing from filter-out words]
                        } else {
                            tweet = tweet.replaceAll("http\\p{L}+", "") // 1.
                                    .replaceAll("[^a-zA-Z@# \\s]+", "") // 2.
                                    .toLowerCase() // 3.
                                    .replaceAll("\\b[tT][cC][oO]\\w*\\b", ""); // 4.
                            String[] tokenArray = tweet.split("\\s+"); // Split the tweet by whitespace to get tokens (tokenizing)
                            tokens.addAll(Arrays.asList(tokenArray)); // Add tokens to the tokens list

                            Iterator<String> iterator = tokens.iterator();
                            while (iterator.hasNext()) {
                                String x = iterator.next();
                                if (x.startsWith("@") && x.length() > 1) { // 5. removing mentions
                                    if (!mentions.contains(x)&& KR) {
                                        mentions.add(x);
                                    }
                                    iterator.remove();
                                    removedMentions++;
                                } else if (x.startsWith("#") && x.length() > 1) { // 6. removing hashtags
                                    if (!hashtags.containsKey(x) && KR) {
                                        hashtags.put(x, 1);
                                    } else if (hashtags.containsKey(x) && KR) {
                                        hashtags.put(x, hashtags.get(x) + 1);
                                    }
                                    iterator.remove();
                                    removedHashtags++;
                                } else if (stopwords.contains(x) && ST) { // 7. removing stopwords
                                    iterator.remove();
                                    removedStopwords++;
                                } else if ((x.length() < 4 || x.length() > 21)) { // 8. removing words with unwanted length
                                    iterator.remove();
                                    removedUnwantedLength++;
                                } else if (filterOutWords.contains(x) && FW) { // 9. removing from filter-out words
                                    iterator.remove();
                                    removedFilteredOutWords++;
                                }
                            }
                        }
                        Set<String> hSet = new HashSet<>();
                        for (String x : tokens) {
                            hSet.add(x); // converting to a set to remove duplicates
                            if (!wordFrequency.containsKey(x)) { // creating frequency of words or terms
                                wordFrequency.put(x, 1);
                            } else {
                                int freq = wordFrequency.get(x);
                                freq = freq + 1;
                                wordFrequency.put(x, freq);
                            }
                            if (!dictionaryList.containsKey(x)) { // adding to the dictionary
                                dictionaryList.put(x, index);
                                index = index + 1;
                            }
                        }
                        //System.out.println("2 "+hSet);
                        //System.out.println(dictionaryList);
////////////////////////////////////////////////////////////////////////////////////////////////
                        List<String> BOW = new ArrayList<>(hSet); // Bag Of Words
                        List<Integer> BOI = new ArrayList<>(getSorted(BOW)); //sort the all words based on their indexes, (Bag Of Indexes)
                        //System.out.println(BOI);
                        for (int i1 = 0; i1 < BOI.size(); i1++) {
                            for (int j = i1 + 1; j < BOI.size(); j++) {
                                List<Integer> tempKey = new ArrayList<>();
                                tempKey.add(BOI.get(i1));
                                tempKey.add(BOI.get(j));
                                //System.out.println(tempList);
                                if (graph.containsKey(tempKey)) {
                                    //System.out.println(tempList);
                                    int weight = graph.get(tempKey);
                                    weight = weight + 1;
                                    graph.put(tempKey, weight);
                                } else {
                                    graph.put(tempKey, 1);
                                    //System.out.println(tempList);
                                }
                            }
                        }
                    };
//////////////////////////////////////////////////////////////////////////////////////////////
                    String createData = (String) jo.get("created_at");
                    if (createData != null) {
                        String[] date = createData.split("\\s+"); // split a string based on whitespace characters
                        if (month.equals(date[1]) && day.equals(Integer.parseInt(date[2]))) { // filtering tweets based on their date
                            if (jo.get("text") != null && jo.get("retweeted_status") == null && jo.get("lang").equals("en") && RT) {  // filtering-out retweets
                                //System.out.println("1 " + iter);
                                i.building();
                            } else if (jo.get("text") != null && jo.get("retweeted_status") != null && RT) {
                                removedRetweets++;
                            } else if (jo.get("text") != null && !jo.get("lang").equals("en") && RT) {
                                removedNonEnglishTweets++;
                            }
                            if (jo.get("text") != null && jo.get("lang").equals("en") && !RT) { // Continue with including retweets
                                //System.out.println("2 " + iter);
                                i.building();
                            } else if (jo.get("text") != null && !jo.get("lang").equals("en") && !RT) {
                                removedNonEnglishTweets++;
                            }
                        }
                    }
///////////////////////////////////////////////////////////////////////////////////////////////
                }
            }
        }
        //System.out.println(dictionaryList);
        //System.out.println(graph);
        //System.out.println(maxWeight);
        System.out.println("\n\n Information of removed words after preprocessing steps: \n");
        System.out.println("# of removed retweets: " + removedRetweets);
        System.out.println("# of removed non-English tweets: " + removedNonEnglishTweets);
        System.out.println("# of removed mentions: " + removedMentions);
        System.out.println("# of removed Hashtags: " + removedHashtags);
        System.out.println("# of removed stopwords: " + removedStopwords);
        System.out.println("# of removed words with unwanted length: " + removedUnwantedLength);
        System.out.println("# of filtered-out words: " + removedFilteredOutWords + "\n");
        outputRunning.print("\n\n Information of removed words after preprocessing steps: \n");
        outputRunning.print("\n# of removed retweets: " + removedRetweets);
        outputRunning.print("\n# of removed non-English tweets: " + removedNonEnglishTweets);
        outputRunning.print("\n# of removed mentions: " + removedMentions);
        outputRunning.print("\n# of removed Hashtags: " + removedHashtags);
        outputRunning.print("\n# of removed stopwords: " + removedStopwords);
        outputRunning.print("\n# of removed words with unwanted length: " + removedUnwantedLength);
        outputRunning.print("\n# of filtered-out words: " + removedFilteredOutWords + "\n");
    }
    public static void graphWriting(final String pathGraph, final String month, final Integer day, final PrintWriter outputRunning) throws IOException {
        File theDir = new File(pathGraph + month+"_"+day);
        if (!theDir.exists()){
            theDir.mkdirs();
        }
        FileWriter fw = new FileWriter(pathGraph + month+"_"+day + "/graph.txt"); // building the graph file
        System.out.println("Writing the graph file...");
        outputRunning.print("\nWriting the graph file...");
        ////////////////////////////////////// finding and removing outliers and unwanted weights
        if (Pru){ // PRUNING THE GRAPH (pruning steps-after building the graph)
            System.out.println("\n Information of the graph after pruning steps: \n");
            outputRunning.print("\n\n Information of the graph after pruning steps: \n");
            Set<Integer> weights = new HashSet<>();
            for(int g :graph.values()){ // obtaining a list of weights
                if(!weights.contains(g)){
                    weights.add(g);
                }
            }
            double[] array = getMueSd (weights); // getting mue, double[0] and sd, double[1]
            //double sd = get_mue_sd(weights.keySet())[1];
            //System.out.println("sum_sd: " + sum_sd);

            maxWeight = Collections.max(graph.values());
            System.out.println("max weight before removing outliers: "+maxWeight);
            System.out.println("# of nodes before removing outliers: "+getNumOfNodes(graph));
            System.out.println("# of edges before removing outliers: "+graph.size());
            outputRunning.print("\nmax weight before removing outliers: "+maxWeight);
            outputRunning.print("\n# of nodes before removing outliers: "+getNumOfNodes(graph));
            outputRunning.print("\n# of edges before removing outliers: "+graph.size());
            graph.values().removeIf(v -> v<(array[0] - 3*array[1])); // 1. removing edges with unwanted weights (removing outliers)
            graph.values().removeIf(v -> v>(array[0] + 3*array[1]));
            System.out.println("-------------------------------------");
            outputRunning.print("\n-------------------------------------");
            maxWeight = Collections.max(graph.values());
            System.out.println("max weight after removing outliers: "+maxWeight);
            System.out.println("# of nodes after removing outliers: "+getNumOfNodes(graph));
            System.out.println("# of edges after removing outliers: "+graph.size());
            outputRunning.print("\nmax weight after removing outliers: "+maxWeight);
            outputRunning.print("\n# of nodes after removing outliers: "+getNumOfNodes(graph));
            outputRunning.print("\n# of edges after removing outliers: "+graph.size());
            graph.values().removeIf(v -> v<(0.02*maxWeight)); // 2. removing edges with unwanted weights (frequency filter)
            graph.values().removeIf(v -> v>(0.9*maxWeight));
            System.out.println("-------------------------------------");
            outputRunning.print("\n-------------------------------------");
            maxWeight = Collections.max(graph.values());
            System.out.println("max weight after frequency filter: "+maxWeight);
            System.out.println("# of nodes after frequency filter: "+getNumOfNodes(graph));
            System.out.println("# of edges after frequency filter: "+graph.size());
            outputRunning.print("\nmax weight after frequency filter: "+maxWeight);
            outputRunning.print("\n# of nodes after frequency filter: "+getNumOfNodes(graph));
            outputRunning.print("\n# of edges after frequency filter: "+graph.size());

            System.out.println("mue: "+array[0]);
            outputRunning.print("\nmue: "+array[0]);
            //System.out.println("sum_d: "+sum_sd);
            System.out.println("sd: "+array[1] + "\n");
            outputRunning.print("\nsd: "+array[1] + "\n");
        }else{
            System.out.println("\n Information of the graph: \n");
            outputRunning.print("\n Information of the graph: \n");
            maxWeight = Collections.max(graph.values());
            System.out.println("max weight: "+maxWeight);
            System.out.println("# of nodes: "+getNumOfNodes(graph));
            System.out.println("# of edges: "+graph.size() + "\n");
            outputRunning.print("\nmax weight: "+maxWeight);
            outputRunning.print("\n# of nodes: "+getNumOfNodes(graph));
            outputRunning.print("\n# of edges: "+graph.size() + "\n");
        }
        if (MAP){
            //System.out.println("1. " + dictionaryList);
            idMap = getMapper (graph); // getting a map table (mapping old ids to new ids)
            //System.out.println("2. " + idMap);
            dictionaryList = updateDictionary(dictionaryList, idMap); // updating the dictionary based on the map table (new ids)
            //System.out.println("3. " + dictionaryList);
            //System.out.println("4. " + graph);
            graph = updateGraph(graph, idMap); // updating the graph based on the map table (new ids)
            //System.out.println("5. " + graph);
        }
        ///////////////////////////////////////////////////////////////////////////////////////////
        graph.forEach((k,v) -> { // writing the pruned graph
            try {
                fw.write(k.get(0) + " " + k.get(1) + " " + v +"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        fw.close();
    }
    public static void DicBuilding(final PrintWriter outputRunning) { // building DicInformation
        System.out.println("Writing the Dictionary file...");
        outputRunning.print("\nWriting the Dictionary file...");
        graph.forEach((k,v) -> {
            if(!(DicInformation.containsKey(k.get(0)))){ // for source nodes
                List<Integer> list = new ArrayList<>();
                list.add(0,1); // # of edges
                list.add(1,v); // Sum of weights
                list.add(2,v); // Max weight
                DicInformation.put(k.get(0),list);
            }else{
                List<Integer> list = new ArrayList<>(DicInformation.get(k.get(0)));
                int tmp_0 = list.get(0);
                int tmp_1 = list.get(1);
                int tmp_2 = list.get(2);
                tmp_0 = tmp_0 + 1;
                tmp_1 = tmp_1 + v;
                if (tmp_2 < v){
                    tmp_2 = v;
                }
                list.set(0,tmp_0); // # of edges
                list.set(1,tmp_1); // Sum of weights
                list.set(2,tmp_2); // Max weight
                DicInformation.put(k.get(0),list);
            }
            if(!(DicInformation.containsKey(k.get(1)))){ // for destination nodes
                List<Integer> list = new ArrayList<>();
                list.add(0,1); // # of edges
                list.add(1,v); // Sum of weights
                list.add(2,v); // Max weight
                DicInformation.put(k.get(1),list);
            }else{
                List<Integer> list = new ArrayList<>(DicInformation.get(k.get(1)));
                int tmp_0 = list.get(0);
                int tmp_1 = list.get(1);
                int tmp_2 = list.get(2);
                tmp_0 = tmp_0 + 1;
                tmp_1 = tmp_1 + v;
                if (tmp_2 < v){
                    tmp_2 = v;
                }
                list.set(0,tmp_0); // # of edges
                list.set(1,tmp_1); // Sum of weights
                list.set(2,tmp_2); // Max weight
                DicInformation.put(k.get(1),list);
            }
        });
        //System.out.println(DicInformation);
        //System.out.println("final # of total nodes: "+ DicInformation.size());
        //System.out.println("final # of total edges: "+ graph.size());
    }
    // create and write the Dictionary and Histogram Excel files from DicInformation and graph structures
    public static void createAndWriteExcels(final String pathDicInfo, final String pathHis, final String month, final Integer day, final PrintWriter outputRunning) throws IOException {
        System.out.println("Writing the Excel files Dictionary.xlsx and Histogram.xlsx...");
        outputRunning.print("\nWriting the Excel files Dictionary.xlsx and Histogram.xlsx...");
        File f_1 = new File(pathDicInfo + month+"_"+day + "/Dictionary.xlsx");
        FileOutputStream file_1 = new FileOutputStream(f_1);
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Dictionary");
        File f_2 = new File(pathHis + month+"_"+day + "/Histogram.xlsx");
        FileOutputStream file_2 = new FileOutputStream(f_2);
        XSSFWorkbook  workbook_2 = new XSSFWorkbook();
        XSSFSheet sheet_2 = workbook_2.createSheet("Histogram");
        ////////////////////////////////////////////////////////////////////// writing the Info file
        Row r = sheet.createRow(0);        // writing the titles
        Row r_2 = sheet_2.createRow(0);    // writing the titles
        Cell c0 = r.createCell(0);
        Cell c1 = r.createCell(1);
        Cell c2 = r.createCell(2);
        Cell c3 = r.createCell(3);
        Cell c4 = r.createCell(4);
        Cell c5 = r.createCell(5);
        Cell c6 = r.createCell(6);
        c0.setCellValue("ID");
        c1.setCellValue("Term");
        c2.setCellValue("# of edges");
        c3.setCellValue("Sum of weights");
        c4.setCellValue("Avg. of weights");
        c5.setCellValue("Frequency");
        c6.setCellValue("Max. weight");
        int rowCount = 0;
        for(HashMap.Entry entry:DicInformation.entrySet()) {
            Row row = sheet.createRow(++rowCount);
            List<Integer> values = new ArrayList<>();
            values.addAll((Collection<? extends Integer>) entry.getValue());
            //System.out.println(values);
            String term = dictionaryList.inverse().get((Integer)entry.getKey());
            //String term = getKey(dictionaryList, (Integer)entry.getKey());
            row.createCell(0).setCellValue((Integer)entry.getKey());
            row.createCell(1).setCellValue(term);
            row.createCell(2).setCellValue(values.get(0));
            row.createCell(3).setCellValue(values.get(1));
            row.createCell(4).setCellValue(df.format((float)values.get(1)/(float)values.get(0)));
            row.createCell(5).setCellValue(wordFrequency.get(term));
            row.createCell(6).setCellValue(values.get(2));
        };
        workbook.write(file_1);
        file_1.flush();
        file_1.close();
        ////////////////////////////////////////////////////////////////////// writing the Histogram file
        Cell c_0 = r_2.createCell(0);
        Cell c_1 = r_2.createCell(1);
        c_1.setCellValue("Frequency");
        c_0.setCellValue("Weight");
        for(int g :graph.values()){ // building a histogram for weights
            if(!histogram.containsKey(g)){
                histogram.put(g, 1);
            }else{
                int h = histogram.get(g);
                h = h + 1;
                histogram.put(g, h);
            }
        }
        //System.out.println(histogram);
        int rowCount_2 = 0;
        for(HashMap.Entry entry:histogram.entrySet()) { // writing the histogram file
            Row row_2 = sheet_2.createRow(++rowCount_2);
            row_2.createCell(0).setCellValue((Integer) entry.getKey());
            row_2.createCell(1).setCellValue((Integer) entry.getValue());
        }
        workbook_2.write(file_2);
        file_2.flush();
        file_2.close();
    }
    public static void fileWriting (final String pathHashtag, final String pathMention, final String month, final Integer day, final PrintWriter outputRunning) throws IOException {
        System.out.println("Writing the file Hashtags.xlsx...");
        outputRunning.print("\nWriting the file Hashtags.xlsx...");
        File e = new File(pathHashtag + month+"_"+day + "/Hashtags.xlsx");
        FileOutputStream e_2 = new FileOutputStream(e);
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Hashtags");
        Row row = sheet.createRow(0);
        Cell c_0 = row.createCell(0);
        Cell c_1 = row.createCell(1);
        c_1.setCellValue("Frequency");
        c_0.setCellValue("Hashtag");
        int rowCount = 0;
        for(HashMap.Entry entry:hashtags.entrySet()) {
            Row row_n = sheet.createRow(++rowCount);
            row_n.createCell(0).setCellValue((String) entry.getKey());
            row_n.createCell(1).setCellValue((Integer) entry.getValue());
        }
        workbook.write(e_2);
        e_2.flush();
        e_2.close();
        FileWriter fileWriter = new FileWriter(pathMention + month+"_"+day + "/Mentions.txt");
        for (String str : mentions) {
            fileWriter.write(str + System.lineSeparator());
        }
        fileWriter.close();
    }
    public static List<Integer> getSorted (final List<String> array){ // sort elements of an arraylist based on their indexes
        ArrayList<Integer> indexList = new ArrayList<>();
        for (String w:array){
            indexList.add(dictionaryList.get(w));
        }
        Collections.sort(indexList);
        return indexList;
    }
    public static List<String> getSorted2 (final String key) { // sort two words based on their indexes
        //System.out.println("key: " + key);
        ArrayList<Integer> indexList = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(key);
        while (tokenizer.hasMoreTokens()) {
            Integer index = dictionaryList.get(tokenizer.nextToken());
            indexList.add(index);
        }
        //System.out.println(indexList);
        Collections.sort(indexList);
        // Convert the List of integers to a List of Strings
        List<String> strList = indexList.stream().map(Object::toString).collect(Collectors.toList());
        //System.out.println(strList);
        return strList;
    }
    private static String getKey(final Map<String, Integer> map, final Integer value) { // finding a key (string) based on a value (integer)
        return map
                .entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), value))
                .map(Map.Entry::getKey)
                .findFirst()
                .map(Object::toString)
                .orElse(null);

    }
    public static Boolean findMatch (final List array, final String x){ // finding an element (string) in an arraylist
        Boolean k = false;
        for (Object s : array) {
            if (x.equals(s.toString())) {
                k = true;
                break;
            }
        }
        return k;
    }
    public static double[] getMueSd (final Set<Integer> weights){ // calculating the mue and sd values
        double[] results = new double[2];
        double sum = 0;
        for (Integer w: weights) {
            sum = sum + w;
        }
        results[0] = sum/(weights.size()); // mue
        //System.out.println("sum: " + sum + " size: " + weights.keySet().size());
        double sum_sd = 0;
        for (Integer w: weights) {
            sum_sd += (w - results[0])*(w - results[0]);
        }
        results[1] = Math.sqrt(sum_sd/(weights.size() - 1)); // sd
        return results;
    }
    public static int getNumOfNodes (final Map<List<Integer>, Integer> graph) { // calculating the number of nodes in the graph
        Set<Integer> nodes = new HashSet<>();
        for (List<Integer> edge : graph.keySet()) {
            nodes.add(edge.get(0));
            nodes.add(edge.get(1));
        }
        return nodes.size();
    }
    /*
    public static int getNumOfNodes (Map<String, Integer> graph){ // calculating the number of nodes in the graph
        ArrayList<String> seenNodes = new ArrayList<String>();
        graph.forEach((k,v) -> {
            StringTokenizer tokenizer = new StringTokenizer(k);
            while (tokenizer.hasMoreTokens()) {
                String word = tokenizer.nextToken();
                if (!seenNodes.contains(word)) {
                    seenNodes.add(word);
                }
            }
        });
        return seenNodes.size();
    }
    */
    public static ArrayList<String> loadFilterOutWords (String path) throws FileNotFoundException {
        ArrayList<String> words = new ArrayList<>();
        File textFile = new File(path);
        Scanner in = new Scanner(textFile);
        while (in.hasNextLine()){
            words.add(in.nextLine());
        }
        return words;
    }
    /*
    public static HashMap ExcelToHashMap(String path) throws IOException{ // loading the frequent hashtags file, words that should be removed
        FileInputStream file = new FileInputStream(path);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0); // assuming data is in the first sheet

        for (Row row : sheet) {
            Cell keyCell = row.getCell(0);
            Cell valueCell = row.getCell(1);
            String key = keyCell.getStringCellValue();
            String value = valueCell.getStringCellValue();
            if (value.equals("y")){
                frequentHashtags.put(key, value);
            }
        }
        workbook.close();
        file.close();
        // use the hashmap as needed
        //System.out.println(dataMap);
        return frequentHashtags;
    }
    */
    public static HashMap<Integer, Integer> getMapper (final Map<List<Integer>, Integer> graph){
        HashMap<Integer, Integer> mapping = new HashMap<>(); // creating a mapping table
        int count = 0; // mapping the nodes starting from zero
        // Loop through remaining nodes in graph hashmap to create the mapping table
        for (Map.Entry<List<Integer>, Integer> entry : graph.entrySet()) {
            List<Integer> nodes = entry.getKey(); // get the old keys
            //int weight = entry.getValue();
            for (int node : nodes) {
                if (!mapping.containsKey(node)) {
                    // Add new mapping
                    mapping.put(node, count); // update with the new keys
                    count++;
                }
            }
        }
        return mapping;
    }
    public static BiMap<String, Integer> updateDictionary(BiMap<String, Integer> dictionary, final HashMap<Integer, Integer> mapping){
        // updating the dictionary by removing terms that were removed from the graph
        Iterator<Map.Entry<String, Integer>> iterator = dictionary.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (!mapping.containsKey(entry.getValue())) {
                iterator.remove(); // Use the iterator's remove() method to safely remove the entry
            }
        }

        // Update node IDs in dictionary hashmap
        BiMap<String, Integer> updatedDictionary = HashBiMap.create();
        for (Map.Entry<String, Integer> entry : dictionary.entrySet()) {
            String key = entry.getKey();
            int oldValue = entry.getValue(); // getting the old id
            int mappedValue = mapping.get(oldValue); // getting the new id
            updatedDictionary.put(key, mappedValue); // update the entry
        }
        dictionary = null;
        return updatedDictionary;
    }
    public static Map<List<Integer>, Integer>  updateGraph (final Map<List<Integer>, Integer> graph, final HashMap<Integer, Integer> mapping){
        Map<List<Integer>, Integer> tempMap = new HashMap<>();
        // Update node IDs in graph hashmap
        for (Map.Entry<List<Integer>, Integer> entry : graph.entrySet()) {
            List<Integer> nodes = entry.getKey();
            for (int i = 0; i < nodes.size(); i++) {
                int node = nodes.get(i); // getting the old id
                int mappedNode = mapping.get(node); // getting the new id
                nodes.set(i, mappedNode);
            }
            int weight = entry.getValue();
            tempMap.put(nodes, weight); // update the graph's entry
        }
        return tempMap;
    }
    private static int getNumOfLines(final ArrayList<String> files) throws IOException {
        System.out.println("Calculating the number of tweets...");
        int lines = 0;
        for (String file:files){
            if (file.endsWith(".txt")){
                LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
                lineNumberReader.skip(Long.MAX_VALUE);
                lines += lineNumberReader.getLineNumber();
                lineNumberReader.close();
            }
        }
        return lines;
    }
    public static void main(String[] args) throws Exception {
        //String pathTweets = "data/brexit/"; // input tweets file-for local PC
        String pathTweets = "/data2/sabdi/brexit/"; // input tweets file-for server
        String pathStopwords = "stopwords.txt"; // input stopwords file
        String pathLemmatizers = "opennlp-en-lemmatizer-dict-NNS.txt"; // input lemmatizer words file
        String pathGraph = "preprocessing_results/"; // output graph file
        String pathDicInfo = "preprocessing_results/"; // output dictionary file
        String pathHistogram = "preprocessing_results/"; // output histogram file
        String pathHashtags = "preprocessing_results/"; // output Hashtags file
        String pathMentions = "preprocessing_results/"; // output Mentions file
        String pathFilteredOut = "filteredOutWords.txt"; // input filteredOut file
        /////////////////////////////// Options ///////////////////////////////////////////////////////////////
        RT = true; // remove retweets?
        ST = true; // remove stopwords?
        POS = true; // use the part of speech method? (POS)
        Pru = true; // pruning steps-(1.removing outliers, 2.frequency filter)
        KR = true; // record removed words?
        FW = true; // remove words that don't represent a specific topic (filtered-out words)
        MAP = true; // mapping the graph to sequential order ids to match with CHkS algorithm
        String MONTH = "May";
        int DAY_FROM = 31;
        int DAY_TO = 31;
        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        ArrayList<String> months = new ArrayList<>(Arrays.asList(MONTH)); //*** months to be run
        //////////////////////////////////////////////////////////////////////////////////////////////////
        ArrayList<String> files = loading(pathTweets, pathStopwords, pathLemmatizers, pathFilteredOut); // loading ...
        for (String month:months){
            for (Integer day=DAY_FROM; day<=DAY_TO; day++){ //*** days (from x to n) to be run
                String pathRunningInformation = "Running_information/" + month + "_" +day +".txt";
                File file = new File(pathRunningInformation);
                if (!file.exists()){file.createNewFile();}
                PrintWriter outputRunning = new PrintWriter(pathRunningInformation);
                System.out.println("Month: "+month + "   /   Day: " + day);
                outputRunning.print("Month: "+month + "   /   Day: " + day);
                long startTime = System.currentTimeMillis();
                graphBuilding(month, day, outputRunning, files); // building the graph + preprocessing
                if (!(graph.size() ==0)){
                    graphWriting(pathGraph, month, day, outputRunning); // pruning-removing edges with unwanted weights
                    DicBuilding(outputRunning);
                    createAndWriteExcels(pathDicInfo, pathHistogram, month, day, outputRunning);
                    if (KR){fileWriting(pathHashtags, pathMentions, month, day, outputRunning);} // writing removed words, separately
                    long endTime = System.currentTimeMillis();
                    long totalTime = endTime - startTime;
                    System.out.println("The total time of processing: " + totalTime/60000 + "(min)");
                    outputRunning.print("\nThe total time of processing: " + totalTime/60000 + "(min)");
                }
                outputRunning.close();
            }
        }
        //////////////////////////////////////////////////////////////////////////////////////////////////
    }
}