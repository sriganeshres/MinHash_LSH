import java.io.*;
import java.util.*;

public class LSHMovieLens {
    private static final int NUM_USERS = 943;
    private static final int NUM_MOVIES = 1682;
    private static final int[] NUM_HASH_FUNCTIONS = {50, 100, 200};
    private static final int[][] LSH_CONFIGS = {
            {5, 10},  // 50 hash functions
            {5, 20},  // 100 hash functions
            {5, 40},  // 200 hash functions
            {10, 20}  // 200 hash functions, different banding
    };
    private static final Random random = new Random();
    private static final List<int[]> hashFunctions = new ArrayList<>();
    private static final Map<Integer, Set<Integer>> userMovieRatings = new HashMap<>();

    public static void main(String[] args) throws IOException {
        loadMovieLensData("u.data");

        for (int i = 0; i < NUM_HASH_FUNCTIONS.length; i++) {
            int numHashes = NUM_HASH_FUNCTIONS[i];
            generateHashFunctions(numHashes);
            int[][] signatures = generateSignatures(numHashes);
            int r = LSH_CONFIGS[i][0], b = LSH_CONFIGS[i][1];
            performLSH(signatures, numHashes, r, b, 0.6);
            performLSH(signatures, numHashes, r, b, 0.8);
        }
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

    private static void generateHashFunctions(int numHashes) {
        hashFunctions.clear();
        for (int i = 0; i < numHashes; i++) {
            int a = random.nextInt(NUM_MOVIES) + 1;
            int b = random.nextInt(NUM_MOVIES) + 1;
            hashFunctions.add(new int[]{a, b});
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

    private static void performLSH(int[][] signatures, int numHashes, int r, int b, double similarityThreshold) {
        System.out.println("LSH with " + numHashes + " hashes, r=" + r + ", b=" + b + ", threshold=" + similarityThreshold);
        Map<String, List<Integer>> hashBuckets = new HashMap<>();
        Set<String> candidatePairs = new HashSet<>();

        for (int user = 1; user <= NUM_USERS; user++) {
            for (int band = 0; band < b; band++) {
                StringBuilder bandHash = new StringBuilder();
                for (int row = 0; row < r; row++) {
                    bandHash.append(signatures[user][band * r + row]).append("-");
                }
                String key = bandHash.toString();
                hashBuckets.computeIfAbsent(key, k -> new ArrayList<>()).add(user);
            }
        }

        for (List<Integer> users : hashBuckets.values()) {
            if (users.size() > 1) {
                for (int i = 0; i < users.size(); i++) {
                    for (int j = i + 1; j < users.size(); j++) {
                        candidatePairs.add(users.get(i) + "-" + users.get(j));
                    }
                }
            }
        }

        int falsePositives = 0, falseNegatives = 0, truePositives = 0, totalPairs = 0;
        for (String pair : candidatePairs) {
            String[] parts = pair.split("-");
            int user1 = Integer.parseInt(parts[0]);
            int user2 = Integer.parseInt(parts[1]);

            double approxJaccard = computeMinHashJaccard(signatures[user1], signatures[user2]);
            double exactJaccard = computeJaccard(
                    userMovieRatings.getOrDefault(user1, new HashSet<>()),
                    userMovieRatings.getOrDefault(user2, new HashSet<>()));

            if (approxJaccard >= similarityThreshold) {
                if (exactJaccard >= similarityThreshold) {
                    truePositives++;
                } else {
                    falsePositives++;
                }
            } else if (exactJaccard >= similarityThreshold) {
                falseNegatives++;
            }
            totalPairs++;
        }

        System.out.println("True Positives: " + truePositives);
        System.out.println("False Positives: " + falsePositives);
        System.out.println("False Negatives: " + falseNegatives);
        System.out.println();
    }

    private static double computeJaccard(Set<Integer> set1, Set<Integer> set2) {
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
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
