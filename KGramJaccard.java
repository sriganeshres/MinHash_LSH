import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class KGramJaccard {
    public static void main(String[] args) throws IOException {
        List<String> docs = readDocuments(Arrays.asList("D1.txt", "D2.txt", "D3.txt", "D4.txt"));

        // Compute and print k-grams for each document
        for (int k : new int[]{2, 3}) {
            System.out.println("Character " + k + "-grams:");
            for (int i = 0; i < docs.size(); i++) {
                Set<String> kGrams = generateKGrams(docs.get(i), k);
                System.out.println("D" + (i + 1) + ": " + kGrams.size());
            }
        }

        System.out.println("\nWord 2-grams:");
        for (int i = 0; i < docs.size(); i++) {
            Set<String> kGrams = generateWordKGrams(docs.get(i), 2);
            System.out.println("D" + (i + 1) + ": " + kGrams.size());
        }

        // Compute Jaccard similarity between document pairs
        System.out.println("\nJaccard Similarities:");
        for (int k : new int[]{2, 3}) {
            System.out.println("Character " + k + "-grams:");
            computeJaccardSimilarity(docs, k, false);
        }

        System.out.println("\nWord 2-grams:");
        computeJaccardSimilarity(docs, 2, true);
    }

    private static List<String> readDocuments(List<String> filenames) throws IOException {
        List<String> docs = new ArrayList<>();
        for (String filename : filenames) {
            docs.add(new String(Files.readAllBytes(new File(filename).toPath())).toLowerCase());
        }
        return docs;
    }

    public static Set<String> generateKGrams(String text, int k) {
        Set<String> kGrams = new HashSet<>();
        for (int i = 0; i <= text.length() - k; i++) {
            kGrams.add(text.substring(i, i + k));
        }
        return kGrams;
    }

    private static Set<String> generateWordKGrams(String text, int k) {
        String[] words = text.split("\\s+");
        Set<String> kGrams = new HashSet<>();
        for (int i = 0; i <= words.length - k; i++) {
            kGrams.add(String.join(" ", Arrays.copyOfRange(words, i, i + k)));
        }
        return kGrams;
    }

    private static void computeJaccardSimilarity(List<String> docs, int k, boolean isWordGram) {
        for (int i = 0; i < docs.size(); i++) {
            for (int j = i + 1; j < docs.size(); j++) {
                Set<String> set1 = isWordGram ? generateWordKGrams(docs.get(i), k) : generateKGrams(docs.get(i), k);
                Set<String> set2 = isWordGram ? generateWordKGrams(docs.get(j), k) : generateKGrams(docs.get(j), k);
                double similarity = jaccard(set1, set2);
                System.out.printf("D%d-D%d: %.4f\n", i + 1, j + 1, similarity);
            }
        }
    }

    public static double jaccard(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
    }
}
