import java.io.*;
import java.util.*;

public class MovieLens {
    private static final int NUM_USERS = 943;
    private static final int NUM_MOVIES = 1682;
    private static final int[] NUM_HASH_FUNCTIONS = { 50, 100, 200 };
    private static final Random random = new Random();
    private static final List<int[]> hashFunctions = new ArrayList<>();
    private static final Map<Integer, Set<Integer>> userMovieRatings = new HashMap<>();

    public static void main(String[] args) throws IOException {
        loadMovieLensData("u.data");
        computeExactJaccard();
        computeMinHashSignatures();
    }

    private static void loadMovieLensData(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            int user = Integer.parseInt(parts[0]);
            int movie = Integer.parseInt(parts[1]);
            userMovieRatings.computeIfAbsent(user, k -> new HashSet<>()).add(movie);
        }
        br.close();
    }

    private static void computeExactJaccard() {
        System.out.println("Exact Jaccard Similarities (>= 0.5):");
        for (int i = 1; i <= NUM_USERS; i++) {
            for (int j = i + 1; j <= NUM_USERS; j++) {
                Set<Integer> set1 = userMovieRatings.getOrDefault(i, new HashSet<>());
                Set<Integer> set2 = userMovieRatings.getOrDefault(j, new HashSet<>());

                if (!set1.isEmpty() && !set2.isEmpty()) {
                    double jaccard = computeJaccard(set1, set2);
                    if (jaccard >= 0.5) {
                        System.out.println(i + " - " + j + " : " + jaccard);
                    }
                }
            }
        }
    }

    private static double computeJaccard(Set<Integer> set1, Set<Integer> set2) {
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
    }

    private static void computeMinHashSignatures() {
        for (int numHashes : NUM_HASH_FUNCTIONS) {
            generateHashFunctions(numHashes);
            int[][] signatures = generateSignatures(numHashes);
            evaluateMinHash(signatures, numHashes);
        }
    }

    private static void generateHashFunctions(int numHashes) {
        hashFunctions.clear();
        for (int i = 0; i < numHashes; i++) {
            int a = random.nextInt(NUM_MOVIES) + 1;
            int b = random.nextInt(NUM_MOVIES) + 1;
            hashFunctions.add(new int[] { a, b });
        }
    }

    private static int[][] generateSignatures(int numHashes) {
        int[][] signatures = new int[NUM_USERS + 1][numHashes];
        for (int user = 1; user <= NUM_USERS; user++) {
            Arrays.fill(signatures[user], Integer.MAX_VALUE);
            Set<Integer> movies = userMovieRatings.getOrDefault(user, new HashSet<>());
            for (int movie : movies) {
                for (int i = 0; i < numHashes; i++) {
                    int hash = (hashFunctions.get(i)[0] * movie + hashFunctions.get(i)[1]) % NUM_MOVIES;
                    signatures[user][i] = Math.min(signatures[user][i], hash);
                }
            }
        }
        return signatures;
    }

    private static void evaluateMinHash(int[][] signatures, int numHashes) {
        System.out.println("MinHash Similarities with " + numHashes + " hash functions (>= 0.5):");
        int falsePositives = 0, falseNegatives = 0, totalPairs = 0;

        for (int i = 1; i <= NUM_USERS; i++) {
            for (int j = i + 1; j <= NUM_USERS; j++) {
                double approxJaccard = computeMinHashJaccard(signatures[i], signatures[j]);
                double exactJaccard = computeJaccard(
                        userMovieRatings.getOrDefault(i, new HashSet<>()),
                        userMovieRatings.getOrDefault(j, new HashSet<>()));
                if (approxJaccard >= 0.5) {
                    System.out.println(i + " - " + j + " : " + approxJaccard);
                }
                if (approxJaccard >= 0.5 && exactJaccard < 0.5)
                    falsePositives++;
                if (approxJaccard < 0.5 && exactJaccard >= 0.5)
                    falseNegatives++;
                totalPairs++;
            }
        }

        System.out.println("False Positives: " + falsePositives);
        System.out.println("False Negatives: " + falseNegatives);
    }

    private static double computeMinHashJaccard(int[] sig1, int[] sig2) {
        int matches = 0;
        for (int i = 0; i < sig1.length; i++) {
            if (sig1[i] == sig2[i])
                matches++;
        }
        return (double) matches / sig1.length;
    }
}
