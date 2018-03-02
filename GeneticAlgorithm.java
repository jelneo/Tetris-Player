import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A genetic algorithm that is used to evolve the Tetris AI and find the best weight multiplier for each feature
 */
public class GeneticAlgorithm {
    private static final int NUM_CHROMOSOMES = 5;
    private static final float PERCENTAGE_OFFSPRING = 0.3f;
    private static final float MUTATION_AMOUNT = 1;
    private int population = 100;
    private int generation = 1;
    private final float mutation_rate = 0.2f;
    private float[][] chromosomes;
    private int currentCandidate = 0;
    private ArrayList<ScoreIndexPair> scores = new ArrayList<>(population);
    List<float[]> offspring_population = new ArrayList<>(population/2);
    private Random rnd = new Random();
    private static float[] fittestCandidate = new float[NUM_CHROMOSOMES];
    private static float fittestScore = Float.NEGATIVE_INFINITY;
    private static int fittestGeneration = 0;
    private static int fittestIndex = 0;


    public GeneticAlgorithm(float[] weights) {
        setUpChromosomes(weights);
    }

    /**
     * Initialise chromosomes with values from -5 to 5.
     * @param weights the weights of the features defined in {@code PlayerSkeleton.java}
     */
    private void setUpChromosomes(float[] weights) {
        chromosomes = new float[population][NUM_CHROMOSOMES];
        for (int i = 0; i < population; i++) {
            for (int j = 0; j < NUM_CHROMOSOMES; j++) {
                if (i % 2 == 0) {
                    chromosomes[i][j] = weights[j];
                } else {
                    chromosomes[i][j] = rnd.nextFloat() * 10 - 5;
                }
            }
        }
    }

    /**
     * Create new generation
     */
    private void createNewGeneration() {
        List<float[]> winners = new ArrayList<>();

        // Pair 1 with 2, 3 with 4, etc.
        for (int i = 0; i < (population / 2); i++) {

            // Pick the fitter one of the two
            float score1 = scores.get(i).getScore();
            float score2 = scores.get(i+1).getScore();
            int winner = score1 > score2 ? i : i + 1;

            // Keep the winner, discard the loser.
            winners.add(chromosomes[winner]);
        }

        List<float[]> offspring_population = new ArrayList<>(population/2);
        // Pair up two winners at a time
        for (int i = 0; i < (winners.size() / 2); i++) {
            float[] winner1 = winners.get(i);
            float[] winner2 = winners.get(i + 1);

            // Create 2 new offspring
            for (int j = 0; j < 2; j++) {

                float[] child = new float[NUM_CHROMOSOMES];

                // Pick at random a mixed subset of the two winners and make it the new chromosome
                for (int k = 0; k < NUM_CHROMOSOMES; k++) {
                    child[j] = rnd.nextInt(2) > 0 ? winner1[k] : winner2[k];

                    // Mutation
                    boolean mutate = rnd.nextFloat() < mutation_rate;
                    if (mutate) {
                        // Change this value anywhere from -5 to 5
                        float change = rnd.nextFloat() * MUTATION_AMOUNT * 2 - MUTATION_AMOUNT;
                        child[j] += change;
                    }
                }

                offspring_population.add(child);
            }
        }

        // Shuffle the offspring population.
        Collections.shuffle(offspring_population, rnd);

        // Replace the least fit PERCENTAGE_OFFSPRING of the population
        Collections.sort(scores);
        for (int i = 0; i < (int)(PERCENTAGE_OFFSPRING * population); i++) {
            for (int j = 0; j < NUM_CHROMOSOMES; j++) {
                chromosomes[scores.get(i).getIndex()][j] = offspring_population.get(i)[j];
            }
        }

        generation++;
        currentCandidate = 0;
        scores = new ArrayList<>(population);
    }

    /**
     * Find the fittest candidate in the population ie. the best set of multiplier weights
     */
    private void findFittestCandidate() {
        // Update the fittest candidate with previous generation's best candidate
        int maxIndex = 0;
        float maxScore = scores.get(0).getScore();
        for (int i = 0; i < scores.size(); i++) {
            float currScore = scores.get(i).getScore();
            if (currScore > maxScore) {
                maxIndex = i;
                maxScore = currScore;
            }
        }
        System.out.println("local Generation " + generation + " candid. " + (maxIndex+1) + " chosen (max score: " + maxScore + "): " + aToS(chromosomes[maxIndex]));

        if (maxScore > fittestScore) {
            fittestScore = maxScore;
            for (int  i = 0; i < NUM_CHROMOSOMES ; i++) {
                fittestCandidate[i] = chromosomes[maxIndex][i];
            }
            fittestGeneration = generation;
            fittestIndex = maxIndex;
        }
    }

    /**
     * Allow the PlayerSkeleton class to send scores for a set of multiplier weights
     * @param score from using a set of multiplier weights
     */
    public void sendScore(float score) {
        String s = aToS(chromosomes[currentCandidate]);
        String string = "Generation " + generation + "; Candidate " + (currentCandidate + 1) + ": " + s + " Score = " + score;
        System.out.println(string);
        scores.add(currentCandidate,new ScoreIndexPair(score, currentCandidate));
        currentCandidate++;

        if (currentCandidate == population) {
            findFittestCandidate();
            createNewGeneration();
        } else {
            PlayerSkeleton.setMultiplierWeights(chromosomes[currentCandidate]);
        }
    }

    /**
     * Returns the best set of multiplier weights
     */
    public float[] getFittestCandidate() {
        System.out.println("fittest score: " + fittestScore + " from generation " + fittestGeneration + " candid. " + (fittestIndex+1)+" : " + aToS(fittestCandidate));
        return fittestCandidate;
    }

    /**
     * Converts float array to string
     * @param a a float[] array
     * @return an properly formatted string of array contents
     */
    private String aToS(float[] a) {
        String s = "";
        for (int i = 0; i < a.length; i++) {
            s += Float.toString(((float) Math.round(a[i] * 100)) / 100);
            if (i != a.length - 1) {
                s += ", ";
            }
        }
        return "[" + s + "]";
    }

    private float[] mutateCandidateRandomly(float[] candidate) {
        float[] mutant = new float[NUM_CHROMOSOMES];

        // Pick at random a mixed subset of the two winners and make it the new chromosome
        for (int k = 0; k < NUM_CHROMOSOMES; k++) {

            // Mutation
            boolean mutate = rnd.nextFloat() < mutation_rate;
            if (mutate) {
                // Change this value anywhere from -5 to 5
                float change = rnd.nextFloat() * MUTATION_AMOUNT * 2 - MUTATION_AMOUNT;
                mutant[k] = candidate[k] + change;
            }
        }

        return mutant;
    }




}