package ca.mcgill.story_testing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.cucumber.core.cli.Main;

public class RandomOrderTestRunner {
    private static final int NUMBER_OF_RUNS = 5;
    private static final String FEATURE_PATH = "src/test/resources/ca/mcgill/story_testing/features";
    private final List<List<String>> executionOrders = new ArrayList<>();
    private final Set<String> uniqueOrders = new HashSet<>();

    @Test
    void runTestsInRandomOrder() {
        List<Integer> failedRuns = new ArrayList<>();
        System.out.println("\n=== Random Order Test Runner ===");
        System.out.println("Running all feature files " + NUMBER_OF_RUNS + " times in random order\n");
        
        List<String> baseFeatureFiles = findFeatureFiles();
        int totalFeatures = baseFeatureFiles.size();
        
        for (int run = 1; run <= NUMBER_OF_RUNS; run++) {
            System.out.println("\n=== Starting Test Run " + run + " of " + NUMBER_OF_RUNS + " ===\n");
            
            // Create a new copy and shuffle
            List<String> featureFiles = new ArrayList<>(baseFeatureFiles);
            Collections.shuffle(featureFiles);
            
            // Store and print execution order
            List<String> thisRunOrder = new ArrayList<>();
            System.out.println("Execution order for run " + run + ":");
            for (int i = 0; i < featureFiles.size(); i++) {
                String featureName = getFeatureName(featureFiles.get(i));
                thisRunOrder.add(featureName);
                System.out.println((i + 1) + ". " + featureName);
            }
            executionOrders.add(thisRunOrder);
            uniqueOrders.add(String.join(",", thisRunOrder));
            System.out.println();

            // Prepare Cucumber arguments
            List<String> args = new ArrayList<>();
            args.add("--glue");
            args.add("ca.mcgill.story_testing.stepdefs");
            args.add("--plugin");
            args.add("pretty");
            args.addAll(featureFiles);

            // Run Cucumber with the specified features
            byte exitStatus = Main.run(args.toArray(String[]::new));
            
            if (exitStatus != 0) {
                failedRuns.add(run);
                System.out.println("\n❌ Run " + run + " FAILED!");
            } else {
                System.out.println("\n✅ Run " + run + " PASSED!");
            }
        }
        
        // Print final summary with randomization verification
        System.out.println("\n=== Test Summary ===");
        System.out.println("Total Runs: " + NUMBER_OF_RUNS);
        System.out.println("Successful Runs: " + (NUMBER_OF_RUNS - failedRuns.size()));
        System.out.println("Failed Runs: " + failedRuns.size());
        System.out.println("Unique Execution Orders: " + uniqueOrders.size() + " out of " + NUMBER_OF_RUNS);
        System.out.println("\nOrder Summary:");
        for (int i = 0; i < executionOrders.size(); i++) {
            System.out.println("Run " + (i + 1) + ": " + String.join(" → ", executionOrders.get(i)));
        }
        
        // Verify randomization
        if (uniqueOrders.size() < Math.min(NUMBER_OF_RUNS, factorial(totalFeatures))) {
            System.out.println("\n⚠️ Warning: Some execution orders were repeated!");
        }
        
        if (!failedRuns.isEmpty()) {
            System.out.println("\nFailed Run Numbers: " + failedRuns);
            throw new RuntimeException("Tests failed in runs: " + failedRuns);
        }
    }

    private List<String> findFeatureFiles() {
        File featuresDir = new File(FEATURE_PATH);
        List<String> features = new ArrayList<>();
        
        if (featuresDir.exists() && featuresDir.isDirectory()) {
            File[] files = featuresDir.listFiles((dir, name) -> name.endsWith(".feature"));
            if (files != null) {
                for (File file : files) {
                    features.add(file.getAbsolutePath());
                }
            }
        }
        
        return features;
    }

    private String getFeatureName(String path) {
        String name = new File(path).getName();
        return name.substring(0, name.length() - 8); // Remove .feature
    }
    
    private long factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }
}