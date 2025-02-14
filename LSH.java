import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class LSH {
    private static final int NUM_HASHES = 160; // Total MinHash functions
    private static final int BANDS = 8;
    private static final int ROWS_PER_BAND = NUM_HASHES / BANDS;
    private static final double THRESHOLD = 0.7;
    private static final int M = 10000; // Hash space size

    public static void main(String[] args) throws IOException {
        String[] docNames = { "D1.txt", "D2.txt", "D3.txt", "D4.txt" };
        List<Set<String>> kGramSets = new ArrayList<>();

        // Read 3-grams for each document
        for (String doc : docNames) {
            String content = readFile(doc);
            kGramSets.add(KGramJaccard.generateKGrams(content, 3));
        }

        // Compute MinHash signatures
        int[][] signatures = computeMinHashSignatures(kGramSets, NUM_HASHES);

        // Compute Jaccard similarities
        double[][] jaccardMatrix = computeJaccardMatrix(kGramSets);

        // Perform LSH to find candidate document pairs
        Map<Integer, List<Integer>> candidatePairs = performLSH(signatures);

        // **Filter candidate pairs using Jaccard similarity**
        System.out.println("\nFiltered Candidate Pairs (Jaccard Sim >= 0.7):");
        for (var entry : candidatePairs.entrySet()) {
            for (int pair : entry.getValue()) {
                double jaccardSim = jaccardMatrix[entry.getKey()][pair];

                if (jaccardSim >= THRESHOLD) {
                    System.out.printf("D%d - D%d (Jaccard: %.4f)\n", entry.getKey() + 1, pair + 1, jaccardSim);
                }
            }
        }

        // Compute LSH probabilities
        computeLSHProbabilities(jaccardMatrix);
    }

    private static int[][] computeMinHashSignatures(List<Set<String>> sets, int numHashes) {
        int[][] signatures = new int[sets.size()][numHashes];
        Random rand = new Random();

        for (int i = 0; i < numHashes; i++) {
            int a = rand.nextInt(M), b = rand.nextInt(M);
            for (int j = 0; j < sets.size(); j++) {
                int minHash = Integer.MAX_VALUE;
                for (String kGram : sets.get(j)) {
                    int hash = Math.abs((a * kGram.hashCode() + b) % M);
                    minHash = Math.min(minHash, hash);
                }
                signatures[j][i] = minHash;
            }
        }
        return signatures;
    }

    private static Map<Integer, List<Integer>> performLSH(int[][] signatures) {
        Map<Integer, List<Integer>> candidatePairs = new HashMap<>();

        for (int band = 0; band < BANDS; band++) {
            Map<String, List<Integer>> buckets = new HashMap<>();
            for (int doc = 0; doc < signatures.length; doc++) {
                StringBuilder keyBuilder = new StringBuilder();
                for (int row = 0; row < ROWS_PER_BAND; row++) {
                    keyBuilder.append(signatures[doc][band * ROWS_PER_BAND + row]).append(",");
                }
                String key = keyBuilder.toString();
                buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(doc);
            }

            // Add document pairs that fall into the same bucket
            for (List<Integer> bucket : buckets.values()) {
                for (int i = 0; i < bucket.size(); i++) {
                    for (int j = i + 1; j < bucket.size(); j++) {
                        int doc1 = bucket.get(i);
                        int doc2 = bucket.get(j);

                        // Ensure doc1 < doc2 to avoid duplicates
                        if (doc1 > doc2) {
                            int temp = doc1;
                            doc1 = doc2;
                            doc2 = temp;
                        }

                        candidatePairs.computeIfAbsent(doc1, k -> new ArrayList<>()).add(doc2);
                    }
                }
            }
        }

        return candidatePairs;
    }

    private static double[][] computeJaccardMatrix(List<Set<String>> sets) {
        int n = sets.size();
        double[][] jaccardMatrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Set<String> intersection = new HashSet<>(sets.get(i));
                intersection.retainAll(sets.get(j));

                Set<String> union = new HashSet<>(sets.get(i));
                union.addAll(sets.get(j));

                jaccardMatrix[i][j] = (double) intersection.size() / union.size();
                jaccardMatrix[j][i] = jaccardMatrix[i][j]; // Symmetric
            }
        }

        System.out.println("\nJaccard Similarity Matrix:");
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                System.out.printf("D%d - D%d: %.4f\n", i + 1, j + 1, jaccardMatrix[i][j]);
            }
        }
        return jaccardMatrix;
    }

    private static void computeLSHProbabilities(double[][] jaccardMatrix) {
        System.out.println("\nLSH Estimated Probabilities (Threshold 0.7):");

        for (int i = 0; i < jaccardMatrix.length; i++) {
            for (int j = i + 1; j < jaccardMatrix.length; j++) {
                double s = jaccardMatrix[i][j];
                double probability = estimateLSHProbability(s);
                System.out.printf("D%d - D%d: %.4f\n", i + 1, j + 1, probability);
            }
        }
    }

    private static String readFile(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename))).toLowerCase();
    }

    private static double estimateLSHProbability(double jaccardSim) {
        return 1 - Math.pow((1 - Math.pow(jaccardSim, ROWS_PER_BAND)), BANDS);
    }
}
