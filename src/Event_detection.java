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


public class Event_detection {
    public static Map<String,Integer> dictionary_list = new HashMap<>(); // generating a dictionary
    public static Map<String, Integer> graph = new HashMap<>();
    public static Map<Integer, List<Integer>> Dic = new HashMap<>(); //for the Dictionary Excel file
    public static Map<Integer, Integer> histogram = new HashMap<>();
    public static final DecimalFormat df = new DecimalFormat("0.00");
    public static Integer max_weight = 0;
    public static Boolean RT;
    public static Boolean ST;
    public static Boolean POS;
    public static Boolean Pru;
    public static Boolean KR;
    public static Boolean FH;
    public static int index = 0;
    public static List<String> stopwords = new ArrayList<>();
    public static Map<String, String> le_words = new HashMap<>();
    public static Map<String, Integer> hashtags = new HashMap<>();
    public static List<String> mentions = new ArrayList<String>();
    public static Map<String, Integer> word_frequency = new HashMap<>();
    public static interface myInterface {
        void building() throws Exception;
    }
    public static void graph_building(String path, String path_2,String path_3) throws Exception {
        if (ST){
            BufferedReader reader_1 = new BufferedReader(new FileReader(path_2)); // loading the stopwords file
            String l = reader_1.readLine();
            while (l != null) {
                stopwords.add(l);
                l = reader_1.readLine();
            }
        }
        if (POS){
            BufferedReader reader_w = new BufferedReader(new FileReader(path_3)); // loading the lemmatizer words
            String w = reader_w.readLine();
            while (w != null) {
                String[] t = w.split("\\s+"); // tokenizing
                le_words.put(t[0], t[1]);
                w = reader_w.readLine();
            }
            //System.out.println(le_words);
        }
        File file = new File(path);
        BufferedReader reader = new BufferedReader(new FileReader(file)); // loading the tweets
///////////////////////////////////////////////////
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
        lineNumberReader.skip(Long.MAX_VALUE);
        int lines = lineNumberReader.getLineNumber();
        lineNumberReader.close();
///////////////////////////////////////////////////
        String line = reader.readLine();
///////////////////////////////////////////////////
        int iter = 0;
        float percentage;
///////////////////////////////////////////////////
        while (line != null) {
            Object obj = new JSONParser().parse(line);
            // typecasting obj to JSONObject
            JSONObject jo = (JSONObject) obj;
            ////////////////////////////////////// Preprocessing
            myInterface i = new myInterface() {
                @Override
                public void building() throws Exception {
                    //  Block of code to try
                    String tweet = (String) jo.get("text"); // getting tweets
                    String[] tokens;
                    if (POS){ // applying part of speech method
                        tweet = tweet.replaceAll("http\\p{L}+", "").replaceAll("[^a-zA-Z'@# \\s]+", "").toLowerCase().replaceAll("\\b[tT][cC][oO]\\w*\\b","");
                        tokens = PosTagger_Performance.main(tweet).toArray(String[]::new);
                    }else{
                        tokens = tweet.replaceAll("http\\p{L}+", "").replaceAll("[^a-zA-Z@# ]", "").toLowerCase().replaceAll("\\b[tT][cC][oO]\\w*\\b","").split("\\s+"); // tokenizing and preprocessing
                        for(String x:tokens){
                            if (x.startsWith("@") && x.length()>1){
                                if(!findmatch(mentions, x) && KR){
                                    mentions.add(x);
                                }
                                List<String> List = new ArrayList<String>(Arrays.asList(tokens));
                                List.remove(x);
                                tokens = List.toArray(new String[0]);
                            }else if(x.startsWith("#") && x.length()>1) {
                                if (!hashtags.containsKey(x) && KR) {
                                    hashtags.put(x, 1);
                                } else if (hashtags.containsKey(x) && KR) {
                                    hashtags.put(x, hashtags.get(x) + 1);
                                }
                                List<String> List_2 = new ArrayList<String>(Arrays.asList(tokens));
                                List_2.remove(x);
                                tokens = List_2.toArray(new String[0]);
                            }else if(findmatch(stopwords, x)){
                                List<String> List_3 = new ArrayList<String>(Arrays.asList(tokens));
                                List_3.remove(x);
                                tokens = List_3.toArray(new String[0]);
                            }else if((x.length()<4 || x.length()>21)){
                                List<String> List_4 = new ArrayList<String>(Arrays.asList(tokens));
                                List_4.remove(x);
                                tokens = List_4.toArray(new String[0]);
                            }
                        }
                    }
                    Set<String> hSet = new HashSet<String>();
                    for (String x : tokens) {
                        hSet.add(x); // converting to a set
                        if (!word_frequency.containsKey(x)){ // creating frequency of terms
                            word_frequency.put(x,  1);
                        }else{
                            int freq = word_frequency.get(x);
                            freq = freq + 1;
                            word_frequency.put(x,  freq);
                        }
                    }
                    for (String x : hSet) { // then adding to the dictionary
                        if (!dictionary_list.containsKey(x)) {
                            dictionary_list.put(x,  index);
                            index = index +1;
                        }
                    }
                    //System.out.println("2 "+hSet);
                    //System.out.println(dictionary_list);
////////////////////////////////////////////////////////////////////////////////////////////////
                    List<String> BOW = new ArrayList<>(hSet); // bag of words
                    BOW = getSorted(BOW); //sort the words based on their indexes
                    //List<String> BOW_tmp = new ArrayList<>(1); // temporal list used for sorting
                    //System.out.println(BOW);
                    for (int i = 0; i < BOW.size(); i++) {
                        for (int j = i + 1; j < BOW.size(); j++) {
                            //BOW_tmp = getSorted2(BOW.get(i) + " " + BOW.get(j)); // sort two selected words based on their indexes
                            if (graph.containsKey(BOW.get(i) + " " + (BOW.get(j)))) {
                                int tmp = graph.get(BOW.get(i) + " " + BOW.get(j));
                                tmp = tmp + 1;
                                graph.put(BOW.get(i) + " " + BOW.get(j), tmp);
                            } else {
                                graph.put(BOW.get(i) + " " + BOW.get(j), 1);
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
                //long endTime = System.nanoTime();
                //long totalTime = endTime - startTime;
                //System.out.println(totalTime);
            }
            iter = iter + 1;
            line = reader.readLine(); // read next line
///////////////////////////////////////////////////////////////////////////////////////////////
            if (jo.get("text") != null && jo.get("retweeted_status") == null && jo.get("lang").equals("en") && RT){  // removing retweets
                //System.out.println("1 " + iter);
                i.building();
            }
            else if (jo.get("text") != null && jo.get("lang").equals("en") && !RT){ // including retweets
                //System.out.println("2 " + iter);
                i.building();
            }
///////////////////////////////////////////////////////////////////////////////////////////////
        }
        //System.out.println(dictionary_list);
        //System.out.println(graph);
        //System.out.println(max_weight);
    }
    public static void graph_writing(String path_graph) throws IOException {
        FileWriter fw = new FileWriter(path_graph); // building the graph file
        System.out.println("Writing the graph file...");
        ////////////////////////////////////// finding and removing outliers and unwanted weights
        if (Pru){ // pruning the graph
            HashMap<Integer, Integer> weights = new HashMap<>();
            for(int g :graph.values()){ // obtaining a list of weights
                if(!weights.containsKey(g)){
                    weights.put(g, null);
                }
            }
            double[] array = get_mue_sd (weights.keySet()); // getting mue, double[0] and sd, double[1]
            //double sd = get_mue_sd(weights.keySet())[1];
            //System.out.println("sum_sd: " + sum_sd);
            max_weight = Collections.max(graph.values());
            System.out.println("max weight before removing outliers: "+max_weight);
            graph.values().removeIf(v -> v<(array[0] - 3*array[1])); // removing edges with unwanted weights
            graph.values().removeIf(v -> v>(array[0] + 3*array[1]));

            max_weight = Collections.max(graph.values());
            System.out.println("max weight after removing outliers: "+max_weight);
            graph.values().removeIf(v -> v<(0.01*max_weight)); // removing edges with unwanted weights
            graph.values().removeIf(v -> v>(0.9*max_weight));

            max_weight = Collections.max(graph.values());
            System.out.println("max weight after frequency filter: "+max_weight);

            System.out.println("mue: "+array[0]);
            //System.out.println("sum_d: "+sum_sd);
            System.out.println("sd: "+array[1]);
        }else{
            max_weight = Collections.max(graph.values());
            System.out.println("max weight: "+max_weight);
        }
        ////////////////////////////////////////////////////////////////////////////////////////////
        if (FH){ // removing frequent hashtags
            ArrayList<String> removedKeyList = new ArrayList<String>();//
            Integer fh = dictionary_list.get("brexit"); //getting the index of brexit
            //System.out.println(fh);
            for (String key:graph.keySet()){ //getting a list of keys that should be removed
                //System.out.println(keys);
                ArrayList<Integer> indexList = new ArrayList<Integer>(2);
                StringTokenizer tokenizer = new StringTokenizer(key);
                while (tokenizer.hasMoreTokens()) {
                    Integer index = Integer.parseInt(tokenizer.nextToken());
                    indexList.add(index);
                    //System.out.println(indexList);
                }
                //System.out.println(indexList.contains(fh));
                if (indexList.contains(fh)) {
                    removedKeyList.add(key); //getting a list of keys that should be removed
                }
                //System.out.println(removedList);
            }
            for (String key:removedKeyList){
                graph.remove(key);
            }
        }
        ///////////////////////////////////////////////////////////////////////////////////////////
        graph.forEach((k,v) -> {
            try {
                fw.write(k + " " + v +"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        fw.close();
    }
    public static void Dic_building() {
        System.out.println("Writing the Dictionary file...");
        graph.forEach((k,v) -> {
            ArrayList<String> wordsList = new ArrayList<String>();
            StringTokenizer tokenizer = new StringTokenizer(k);
            while (tokenizer.hasMoreTokens()) {
                String word = tokenizer.nextToken();
                wordsList.add(word);
            }
            if(!(Dic.containsKey(Integer.parseInt(wordsList.get(0))))){
                List<Integer> list = new ArrayList<>();
                list.add(0,1); // # of edges
                list.add(1,v); // Sum of weights
                list.add(2,v); // Max weight
                Dic.put(Integer.parseInt(wordsList.get(0)),list);
            }else{
                List<Integer> list = new ArrayList<>(Dic.get(Integer.parseInt(wordsList.get(0))));
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
                Dic.put(Integer.parseInt(wordsList.get(0)),list);
            }
            if(!(Dic.containsKey(Integer.parseInt(wordsList.get(1))))){
                List<Integer> list = new ArrayList<>();
                list.add(0,1); // # of edges
                list.add(1,v); // Sum of weights
                list.add(2,v); // Max weight
                Dic.put(Integer.parseInt(wordsList.get(1)),list);
            }else{
                List<Integer> list = new ArrayList<>(Dic.get(Integer.parseInt(wordsList.get(1))));
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
                Dic.put(Integer.parseInt(wordsList.get(1)),list);
            }
        });
        //System.out.println(info);
        System.out.println("# of total nodes: "+ Dic.size());
        System.out.println("# of total edges: "+ graph.size());
    }
    public static void createExcel(String path_info, String path_his) throws IOException {
        try {
            System.out.println("Writing the Excel files...");
            File fileName = new File(path_info);
            FileOutputStream file = new FileOutputStream(fileName);
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Dictionary");
            File f_2 = new File(path_his);
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
            for(HashMap.Entry entry:Dic.entrySet()) {
                Row row = sheet.createRow(++rowCount);
                List<Integer> v = new ArrayList<>();
                v.addAll((Collection<? extends Integer>) entry.getValue());
                //System.out.println(v);
                String term = getKey(dictionary_list, (Integer)entry.getKey());
                row.createCell(0).setCellValue((Integer)entry.getKey());
                row.createCell(1).setCellValue(term);
                row.createCell(2).setCellValue(v.get(0));
                row.createCell(3).setCellValue(v.get(1));
                row.createCell(4).setCellValue(df.format((float)v.get(1)/(float)v.get(0)));
                row.createCell(5).setCellValue(word_frequency.get(term));
                row.createCell(6).setCellValue(v.get(2));
            };
            workbook.write(file);
            file.flush();
            file.close();
            ////////////////////////////////////////////////////////////////////// writing the Histogram file
            Cell c_0 = r_2.createCell(0);
            Cell c_1 = r_2.createCell(1);
            c_1.setCellValue("Frequency");
            c_0.setCellValue("Weight");
            for(int g :graph.values()){ // building a histogram
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
            for(HashMap.Entry entry:histogram.entrySet()) {
                Row row_2 = sheet_2.createRow(++rowCount_2);
                row_2.createCell(0).setCellValue((Integer) entry.getKey());
                row_2.createCell(1).setCellValue((Integer) entry.getValue());
            }
            workbook_2.write(f);
            f.flush();
            f.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static void RW_file_writing (String path_hashtag, String path_mention) throws IOException {
        System.out.println("Writing the removed words...");
        try {
            File e = new File(path_hashtag);
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
            FileWriter fileWriter_2 = new FileWriter(path_mention);
            for (String str : mentions) {
                fileWriter_2.write(str + System.lineSeparator());
            }
            fileWriter_2.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static List<String> getSorted (List<String> BOW){
        ArrayList<Integer> indexList = new ArrayList<Integer>();
        for (String w:BOW){
            indexList.add(dictionary_list.get(w));
        }
        Collections.sort(indexList);
        List<String> strList = indexList.stream().map(Object::toString).collect(Collectors.toList());
        return strList;
    }
    public static List<String> getSorted2 (String key) { // sorted words based on their indexes
        //System.out.println("key: " + key);
        ArrayList<Integer> indexList = new ArrayList<Integer>();
        StringTokenizer tokenizer = new StringTokenizer(key);
        while (tokenizer.hasMoreTokens()) {
            Integer index = dictionary_list.get(tokenizer.nextToken());
            indexList.add(index);
        }
        //System.out.println(indexList);
        Collections.sort(indexList);
        // Convert the List of integers to a List of Strings
        List<String> strList = indexList.stream().map(Object::toString).collect(Collectors.toList());
        //System.out.println(strList);
        return strList;
    }
    private static String getKey(Map<String, Integer> map, Integer value) {
        return map
                .entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), value))
                .map(Map.Entry::getKey)
                .findFirst()
                .map(Object::toString)
                .orElse(null);

    }
    public static Boolean findmatch (List array, String x){
        Boolean k = false;
        for (Object s : array) {
            if (x.equals(s.toString())) {
                k = true;
                break;
            }
        }
        return k;
    }
    public static double[] get_mue_sd (Set<Integer> N){
        double[] results = new double[2];
        double sum = 0;
        for (Integer w: N) {
            sum = sum + w;
        }
        results[0] = sum/(N.size()); //mue
        //System.out.println("sum: " + sum + " size: " + weights.keySet().size());
        double sum_sd = 0;
        for (Integer w: N) {
            sum_sd += (w - results[0])*(w - results[0]);
        }
        results[1] = Math.sqrt(sum_sd/(N.size() - 1)); //sd
        return results;
    }
    public static void main(String[] args) throws Exception {
        String path = "Dataset/2016-06-24-all.txt"; // input tweets file
        String path_2 = "stopwords.txt"; // input stopwords file
        String path_3 = "opennlp-en-lemmatizer-dict-NNS.txt"; // input lemmatizer words file
        String path_graph = "preprocessing results/graph.txt"; // output graph file
        String path_info = "preprocessing results/Dictionary.xlsx"; // output dictionary file
        String path_his = "preprocessing results/Histogram.xlsx"; // output histogram file
        String path_hashtag = "preprocessing results/Hashtags.xlsx";
        String path_mention = "preprocessing results/Mentions.txt";
        RT = true; // remove retweets?
        ST = true; // remove stopwords?
        POS = true; // use the part of speech method? (POS)
        Pru = true; // remove unwanted weights? pruning-(removing outliers, finding max weight, and frequency filter)
        KR = true; // save removed words in a file?
        FH = true; // remove words that have frequent hashtags?
        graph_building(path, path_2, path_3); //building the graph + preprocessing
        graph_writing(path_graph); //pruning-removing edges with unwanted weights
        Dic_building();
        createExcel(path_info, path_his);
        if (KR){RW_file_writing(path_hashtag, path_mention);} // writing removed words, separately
    }
}
