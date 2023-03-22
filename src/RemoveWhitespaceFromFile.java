import java.io.*;

public class RemoveWhitespaceFromFile {
    public static void main(String[] args) {
        try {
            // Open the file for reading
            File file = new File("filteredOutWords.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));

            // Open a temporary file for writing
            File tempFile = new File("temp.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            // Read each line, remove whitespace, and write to temporary file
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    writer.write(trimmedLine);
                    writer.newLine();
                }
            }

            // Close readers and writers
            reader.close();
            writer.close();

            // Replace original file with temporary file
            file.delete();
            tempFile.renameTo(file);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
