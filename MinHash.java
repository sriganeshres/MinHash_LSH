import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MinHash {
    private static final int M = 10000; // Large prime for hashing

    public static void main(String[] args) throws IOException {
        // Read all documents
        String[] docNames = { "D1.txt", "D2.txt", "D3.txt", "D4.txt" };
        List<Set<String>> kGramSets = new ArrayList<>();

        for (String doc : docNames) {
            String content = readFile(doc);
            kGramSets.add(KGramJaccard.generateKGrams(content, 3)); // 3-grams
        }

        // Compute MinHash signatures for different t values
        int[] tValues = { 20, 60, 150, 300, 600 };
        for (int t : tValues) {
            int[][] signatures = computeMinHashSignatures(kGramSets, t);

            System.out.println("\nFor t = " + t + ":");
            computeAllPairwiseJaccard(signatures, docNames, kGramSets);
        }
    }

    private static String readFile(String filepath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filepath))).toLowerCase().replaceAll("[^a-z ]", "");
    }

    private static int[][] computeMinHashSignatures(List<Set<String>> sets, int t) {
        int[][] signatures = new int[sets.size()][t];
        Random rand = new Random();
        int[] a = new int[t], b = new int[t];

        // Generate t random hash functions
        for (int i = 0; i < t; i++) {
            a[i] = rand.nextInt(M - 1) + 1; // Avoid zero
            b[i] = rand.nextInt(M);
        }

        for (int i = 0; i < sets.size(); i++) {
            Set<String> kGrams = sets.get(i);
            for (int j = 0; j < t; j++) {
                int minHash = Integer.MAX_VALUE;
                for (String kGram : kGrams) {
                    int hashValue = (a[j] * kGram.hashCode() + b[j]) % M;
                    minHash = Math.min(minHash, hashValue);
                }
                signatures[i][j] = minHash;
            }
        }
        return signatures;
    }

    private static void computeAllPairwiseJaccard(int[][] signatures, String[] docNames, List<Set<String>> kGramSets) {
        int numDocs = signatures.length;
        for (int i = 0; i < numDocs; i++) {
            for (int j = i + 1; j < numDocs; j++) {
                double estimatedJaccard = estimateJaccard(signatures[i], signatures[j]);
                double actualJaccard = KGramJaccard.jaccard(kGramSets.get(i), kGramSets.get(j));
                System.out.printf("Jaccard(%s, %s): MinHash = %.4f, Exact = %.4f\n",
                        docNames[i], docNames[j], estimatedJaccard, actualJaccard);
            }
        }
    }

    private static double estimateJaccard(int[] sig1, int[] sig2) {
        int matches = 0;
        for (int i = 0; i < sig1.length; i++) {
            if (sig1[i] == sig2[i])
                matches++;
        }
        return (double) matches / sig1.length;
    }
}
