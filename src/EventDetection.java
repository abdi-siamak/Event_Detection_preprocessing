import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.sun.deploy.util.StringUtils;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;


public class EventDetection {
    private static Map<String,Integer> dictionaryList = new HashMap<>(); // dictionary-unique words
    private static Map<List<Integer>, Integer> graph = new HashMap<>(); // graph structure
    private static Map<Integer, List<Integer>> DicInformation = new HashMap<>(); // Dictionary Excel file
    private static Map<Integer, Integer> histogram = new HashMap<>(); // histogram of weights
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static Integer maxWeight = 0;
    private static Boolean RT; // switch for retweets
    private static Boolean ST; // switch for stopwords
    private static Boolean POS; // switch for activating POS
    private static Boolean Pru; //switch for activating pruning methods
    public static Boolean KR; // switch for saving removed words
    private static Boolean FW; // switch for removing words that don't represent a specific topic
    private static boolean MAP; // switch for mapping the graph to sequential order ids
    private static int index = 0; // index of dictionary
    public static Map<String, String> stopwords = new HashMap<>();
    public static Map<String, String> lemmatizerWords = new HashMap<>();
    public static Map<String, String> filterOutWords = new HashMap<>();
    public static Map<String, Integer> hashtags = new HashMap<>(); // for keeping removed hashtags
    public static List<String> mentions = new ArrayList<>(); // for keeping removed mentions
    private static Map<String, Integer> wordFrequency = new HashMap<>();
    public static int removedUnwantedLength = 0;
    public static int removedStopwords = 0;
    private static int removedRetweets = 0;
    public static int removedHashtags = 0;
    public static int removedMentions = 0;
    private static int removedNonEnglishTweets = 0;
    public static int removedNon_Nouns = 0;
    public static int removedFilteredOutWords = 0;
    private static HashMap<String, String> frequentHashtags = new HashMap<>();
    private static HashMap<Integer, Integer> idMap = new HashMap<>();
    private interface myInterface {
        void building() throws Exception;
    }
    public static void graphBuilding(String pathTweets, String pathStopwords,String pathLemmatizers, String pathFilteredOut) throws Exception {
        if (ST){
            BufferedReader reader = new BufferedReader(new FileReader(pathStopwords)); // loading the stopwords file
            String l = reader.readLine();
            while (l != null) {
                stopwords.put(l, null);
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
                filterOutWords.put(w, null);
                w = reader.readLine();
            }
            //System.out.println(filterOutWords);
        }
//////////////////////////////////////////////////////////////////////////////////// loading tweets
        File dir = new File(pathTweets);
        String[] folders = dir.list(); // list of folders
        ArrayList<String> files = new ArrayList<>(); // list of files within folders
        for (String folder:folders){
            if (!folder.equals(".DS_Store")){
                File subDir = new File(pathTweets + "/" +folder);
                for (String file : subDir.list()){
                    files.add(pathTweets + folder + "/" + file);
                }
            }
        }
        int lines;
        int iter = 0;
        float percentage;
        lines = getNumOfLines(files, pathTweets); // getting the number of all tweets
        for (String file : files) {
            if (!file.contains(".DS_Store")){
                System.out.println("Reading file: " + file);
                BufferedReader reader = new BufferedReader(new FileReader(file));
///////////////////////////////////////////////////////////////////////////////////
                String line = reader.readLine();
                ///////////////////////////////////////////////////
                while (line != null) {
                    //System.out.println(line);
                    Object obj = new JSONParser().parse(line);
                    // typecasting obj to JSONObject
                    JSONObject jo = (JSONObject) obj;
                    ////////////////////////////////////// Preprocessing
                    myInterface i = new myInterface() {
                        @Override
                        public void building() throws Exception {
                            String tweet = (String) jo.get("text"); // getting tweets
                            String[] tokens;
                            if (POS){ // applying part of speech method
                                tweet = tweet.replaceAll("http\\p{L}+", "")
                                        .replaceAll("[^a-zA-Z'@# \\s]+", "")
                                        .toLowerCase()
                                        .replaceAll("\\b[tT][cC][oO]\\w*\\b","");
                                //tokens = PosTaggerPerformance.main(tweet).toArray(String[]::new);
                                List<String> tokenList = PosTaggerPerformance.main(tweet);
                                tokens = tokenList.toArray(new String[tokenList.size()]);
                            }else{
                                tokens = tweet.replaceAll("http\\p{L}+", "")
                                        .replaceAll("[^a-zA-Z@# ]", "")
                                        .toLowerCase()
                                        .replaceAll("\\b[tT][cC][oO]\\w*\\b","")
                                        .split("\\s+"); // tokenizing and preprocessing
                                for(String x:tokens){
                                    if (x.startsWith("@") && x.length()>1){
                                        if(!findMatch(mentions, x) && KR){
                                            mentions.add(x);
                                        }
                                        List<String> List = new ArrayList<>(Arrays.asList(tokens));
                                        List.remove(x);
                                        tokens = List.toArray(new String[0]);
                                        removedMentions++;
                                    }else if(x.startsWith("#") && x.length()>1) {
                                        if (!hashtags.containsKey(x) && KR) {
                                            hashtags.put(x, 1);
                                        } else if (hashtags.containsKey(x) && KR) {
                                            hashtags.put(x, hashtags.get(x) + 1);
                                        }
                                        List<String> List = new ArrayList<>(Arrays.asList(tokens));
                                        List.remove(x);
                                        tokens = List.toArray(new String[0]);
                                        removedHashtags++;
                                    }else if(stopwords.containsKey(x)){
                                        List<String> List = new ArrayList<>(Arrays.asList(tokens));
                                        List.remove(x);
                                        tokens = List.toArray(new String[0]);
                                        removedStopwords++;
                                    }else if((x.length()<4 || x.length()>21)){
                                        List<String> List = new ArrayList<>(Arrays.asList(tokens));
                                        List.remove(x);
                                        tokens = List.toArray(new String[0]);
                                        removedUnwantedLength++;
                                    }else if(filterOutWords.containsKey(x)){
                                        List<String> List = new ArrayList<>(Arrays.asList(tokens));
                                        List.remove(x);
                                        tokens = List.toArray(new String[0]);
                                        removedFilteredOutWords++;
                                    }
                                }
                            }
                            Set<String> hSet = new HashSet<>();
                            for (String x : tokens) {
                                hSet.add(x); // converting to a set
                                if (!wordFrequency.containsKey(x)){ // creating frequency of terms
                                    wordFrequency.put(x,  1);
                                }else{
                                    int freq = wordFrequency.get(x);
                                    freq = freq + 1;
                                    wordFrequency.put(x,  freq);
                                }
                                if (!dictionaryList.containsKey(x)) { // adding to the dictionary
                                    dictionaryList.put(x,  index);
                                    index = index +1;
                                }
                            }
                            //System.out.println("2 "+hSet);
                            //System.out.println(dictionaryList);
////////////////////////////////////////////////////////////////////////////////////////////////
                            List<String> BOW = new ArrayList<>(hSet); // Bag Of Words
                            List<Integer> BOI = new ArrayList<>(getSorted(BOW)); //sort the all words based on their indexes, Bag Of Indexes
                            //System.out.println(BOI);
                            for (int i = 0; i < BOI.size(); i++) {
                                for (int j = i + 1; j < BOI.size(); j++) {
                                    List<Integer> tempList = new ArrayList<>();
                                    tempList.add(BOI.get(i));
                                    tempList.add(BOI.get(j));
                                    //System.out.println(tempList);
                                    if (graph.containsKey(tempList)) {
                                        //System.out.println(tempList);
                                        int tmp = graph.get(tempList);
                                        tmp = tmp + 1;
                                        graph.put(tempList, tmp);
                                    } else {
                                        graph.put(tempList, 1);
                                        //System.out.println(tempList);
                                    }
                                }
                            }
                        }
                    };
///////////////////////////////////////////////////////////////////////////////////////////////
                    percentage = (float) iter * 100 / lines;
                    //long startTime = System.nanoTime();
                    if (percentage % 5 == 0) {
                        System.out.println("Building the graph: " + percentage + " %");
                    }
                    iter = iter + 1;
                    line = reader.readLine(); // read next line
///////////////////////////////////////////////////////////////////////////////////////////////
                    //String data = (String) jo.get("created_at");
                    //WhitespaceTokenizer whitespaceTokenizer= WhitespaceTokenizer.INSTANCE;
                    //String[] tokens = whitespaceTokenizer.tokenize(data);
                    if (true){
                        if (jo.get("text") != null && jo.get("retweeted_status") == null && jo.get("lang").equals("en") && RT){  // removing retweets
                            //System.out.println("1 " + iter);
                            i.building();
                        }else if (jo.get("text") != null && jo.get("retweeted_status") != null && RT){
                            removedRetweets++;
                        }else if (jo.get("text") != null && !jo.get("lang").equals("en") && RT){
                            removedNonEnglishTweets++;
                        }

                        if (jo.get("text") != null && jo.get("lang").equals("en") && !RT){ // including retweets
                            //System.out.println("2 " + iter);
                            i.building();
                        } else if (jo.get("text") != null && !jo.get("lang").equals("en") && !RT) {
                            removedNonEnglishTweets++;
                        }
                    }
///////////////////////////////////////////////////////////////////////////////////////////////
                }
            }
        }
        //System.out.println(dictionaryList);
        //System.out.println(graph);
        //System.out.println(maxWeight);
        System.out.println("\n Information of removed words after preprocessing step: \n");
        System.out.println("# of removed retweets: " + removedRetweets);
        System.out.println("# of removed non-English tweets: " + removedNonEnglishTweets);
        System.out.println("# of removed mentions: " + removedMentions);
        System.out.println("# of removed Hashtags: " + removedHashtags);
        System.out.println("# of removed stopwords: " + removedStopwords);
        System.out.println("# of removed words with unwanted length: " + removedUnwantedLength);
        System.out.println("# of filtered-out words: " + removedFilteredOutWords + "\n");
    }
    public static void graphWriting(String pathGraph) throws IOException {
        FileWriter fw = new FileWriter(pathGraph); // building the graph file
        System.out.println("Writing the graph file...");
        ////////////////////////////////////// finding and removing outliers and unwanted weights
        if (Pru){ // pruning the graph
            System.out.println("\n Information of the graph after pruning step: \n");
            HashMap<Integer, Integer> weights = new HashMap<>();
            for(int g :graph.values()){ // obtaining a list of weights
                if(!weights.containsKey(g)){
                    weights.put(g, null);
                }
            }
            double[] array = getMueSd (weights.keySet()); // getting mue, double[0] and sd, double[1]
            //double sd = get_mue_sd(weights.keySet())[1];
            //System.out.println("sum_sd: " + sum_sd);

            maxWeight = Collections.max(graph.values());
            System.out.println("max weight before removing outliers: "+maxWeight);
            System.out.println("# of nodes before removing outliers: "+getNumOfNodes(graph));
            System.out.println("# of edges before removing outliers: "+graph.size());
            graph.values().removeIf(v -> v<(array[0] - 3*array[1])); // removing edges with unwanted weights
            graph.values().removeIf(v -> v>(array[0] + 3*array[1]));

            maxWeight = Collections.max(graph.values());
            System.out.println("max weight after removing outliers: "+maxWeight);
            System.out.println("# of nodes after removing outliers: "+getNumOfNodes(graph));
            System.out.println("# of edges after removing outliers: "+graph.size());
            graph.values().removeIf(v -> v<(0.02*maxWeight)); // removing edges with unwanted weights
            graph.values().removeIf(v -> v>(0.9*maxWeight));

            maxWeight = Collections.max(graph.values());
            System.out.println("max weight after frequency filter: "+maxWeight);
            System.out.println("# of nodes after frequency filter: "+getNumOfNodes(graph));
            System.out.println("# of edges after frequency filter: "+graph.size());

            System.out.println("mue: "+array[0]);
            //System.out.println("sum_d: "+sum_sd);
            System.out.println("sd: "+array[1] + "\n");
        }else{
            System.out.println("\n Information of the graph: \n");
            maxWeight = Collections.max(graph.values());
            System.out.println("max weight: "+maxWeight);
            System.out.println("# of nodes: "+getNumOfNodes(graph));
            System.out.println("# of edges: "+graph.size() + "\n");
        }
        ////////////////////////////////////////////////////////////////////////////////////////////
        /*
        if (FW){ // filtered-out words
            ArrayList<String> removewords = new ArrayList<>(); // words that should be removed from the graph
            ArrayList<List<Integer>> removeKeys = new ArrayList<>();
            removewords = loadFilterOutWords (pathFilteredOut);
            for (Map.Entry<List<Integer>, Integer> entry : graph.entrySet()) {
                if (removewords.contains(getKey(dictionaryList,entry.getKey().get(0)))||removewords.contains(getKey(dictionaryList,entry.getKey().get(1)))){
                    removeKeys.add(entry.getKey());
                }
            }
            for (List<Integer> key:removeKeys){
                graph.remove(key);
            }
            System.out.println("max weight after frequent hashtags filter: "+ Collections.max(graph.values()));
            System.out.println("# of nodes after frequent hashtags filter: "+getNumOfNodes(graph));
            System.out.println("# of edges after frequent hashtags filter: "+graph.size() + "\n");
        }
        ///////////////////////////////////////////////////////////////////////////////////////////
         */
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
    public static void DicBuilding() {
        System.out.println("Writing the Dictionary file...");
        graph.forEach((k,v) -> {
            if(!(DicInformation.containsKey(k.get(0)))){
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
            if(!(DicInformation.containsKey(k.get(1)))){
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
    public static void createExcel(String pathDicInfo, String pathHis) throws IOException {
        System.out.println("Writing the Excel files...");
        File fileName = new File(pathDicInfo);
        FileOutputStream file = new FileOutputStream(fileName);
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Dictionary");
        File f_2 = new File(pathHis);
        FileOutputStream f = new FileOutputStream(f_2);
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
            String term = getKey(dictionaryList, (Integer)entry.getKey());
            row.createCell(0).setCellValue((Integer)entry.getKey());
            row.createCell(1).setCellValue(term);
            row.createCell(2).setCellValue(values.get(0));
            row.createCell(3).setCellValue(values.get(1));
            row.createCell(4).setCellValue(df.format((float)values.get(1)/(float)values.get(0)));
            row.createCell(5).setCellValue(wordFrequency.get(term));
            row.createCell(6).setCellValue(values.get(2));
        };
        workbook.write(file);
        file.flush();
        file.close();
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
        workbook_2.write(f);
        f.flush();
        f.close();
    }
    public static void fileWriting (String pathHashtag, String pathMention) throws IOException {
        System.out.println("Writing the removed words...");
        File e = new File(pathHashtag);
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
        FileWriter fileWriter = new FileWriter(pathMention);
        for (String str : mentions) {
            fileWriter.write(str + System.lineSeparator());
        }
        fileWriter.close();
    }
    public static List<Integer> getSorted (List<String> array){ // sort elements of an arraylist based on their indexs
        ArrayList<Integer> indexList = new ArrayList<>();
        for (String w:array){
            indexList.add(dictionaryList.get(w));
        }
        Collections.sort(indexList);
        return indexList;
    }
    public static List<String> getSorted2 (String key) { // sort two words based on their indexes
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
    private static String getKey(Map<String, Integer> map, Integer value) { // finding a key (string) based on a value (integer)
        return map
                .entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), value))
                .map(Map.Entry::getKey)
                .findFirst()
                .map(Object::toString)
                .orElse(null);

    }
    public static Boolean findMatch (List array, String x){ // finding an element (string) in an arraylist
        Boolean k = false;
        for (Object s : array) {
            if (x.equals(s.toString())) {
                k = true;
                break;
            }
        }
        return k;
    }
    public static double[] getMueSd (Set<Integer> weights){ // calculating the mue and sd values
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
    public static int getNumOfNodes (Map<List<Integer>, Integer> graph) { // calculating the number of nodes in the graph
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
    public static HashMap<Integer, Integer> getMapper (Map<List<Integer>, Integer> graph){
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
    public static Map<String, Integer> updateDictionary(Map<String, Integer> dic, HashMap<Integer, Integer> mapping){
        // updating the dictionary by removing terms that were removed from the graph
        ArrayList<String> removedKeys = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dic.entrySet()) {
            if (!mapping.containsKey(entry.getValue())){
                removedKeys.add(entry.getKey()); // finding records that should be removed from the dictionary
            }
        }
        for (String key:removedKeys){
            dic.remove(key);
        }
        // Update node IDs in dictionary hashmap
        for (Map.Entry<String, Integer> entry : dic.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue(); // getting the old id
            int mappedValue = mapping.get(value); // getting the new id
            dic.put(key, mappedValue); // update the entry
        }
        return dic;
    }
    public static Map<List<Integer>, Integer>  updateGraph (Map<List<Integer>, Integer> graph, HashMap<Integer, Integer> mapping){
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
    private static int getNumOfLines(ArrayList<String> files, String pathTweets) throws IOException {
        int lines = 0;
        for (String file:files){
            if (!file.contains(".DS_Store")){
                LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
                lineNumberReader.skip(Long.MAX_VALUE);
                lines += lineNumberReader.getLineNumber();
                lineNumberReader.close();
            }
        }
        return lines;
    }
    public static void main(String[] args) throws Exception {
        String pathTweets = "/Users/siamakabdi/Projects/IdeaProjects/Event_Detection_preprocessing/Dataset/"; // input tweets file
        String pathStopwords = "stopwords.txt"; // input stopwords file
        String pathLemmatizers = "opennlp-en-lemmatizer-dict-NNS.txt"; // input lemmatizer words file
        String pathGraph = "preprocessing results/graph.txt"; // output graph file
        String pathDicInfo = "preprocessing results/Dictionary.xlsx"; // output dictionary file
        String pathHistogram = "preprocessing results/Histogram.xlsx"; // output histogram file
        String pathHashtags = "preprocessing results/Hashtags.xlsx";
        String pathMentions = "preprocessing results/Mentions.txt";
        String pathFilteredOut = "filteredOutWords.txt";
        /////////////////////////////// Options ///////////////////////////////////////////////////////////////
        RT = true; // remove retweets?
        ST = true; // remove stopwords?
        POS = true; // use the part of speech method? (POS)
        Pru = true; // remove unwanted weights? pruning-(1.removing outliers, 2.finding max weight, and 3.frequency filter)
        KR = true; // save removed words?
        FW = true; // remove words that don't represent a specific topic (filtered out words)
        MAP = true; // mapping the graph to sequential order ids
        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        long startTime = System.currentTimeMillis();
        graphBuilding(pathTweets, pathStopwords, pathLemmatizers, pathFilteredOut); // building the graph + preprocessing
        graphWriting(pathGraph); // pruning-removing edges with unwanted weights
        DicBuilding();
        createExcel(pathDicInfo, pathHistogram);
        if (KR){fileWriting(pathHashtags, pathMentions);} // writing removed words, separately
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("The total time of processing: " + totalTime/60000 + "(min)");
    }
}