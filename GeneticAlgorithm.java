import java.util.*;

/**
 * A genetic algorithm that is used to evolve the Tetris AI and find the best weight multiplier for each feature
 */
public class GeneticAlgorithm {
    private static final int NUM_CHROMOSOMES = 5;
    private static final float PERCENTAGE_OFFSPRING = 0.3f;
    private static final float MUTATION_AMOUNT = 0.35f;
    // we want the first mutation to occur with higher probability to get out of local maximas
    private static final float INITIAL_MUTATION_AMOUNT = 10 * MUTATION_AMOUNT;
    private int population = 100;
    private int generation = 1;
    private final float MUTATION_RATE = 0.05f;
    private List<float[]> chromosomes;
    private int currentCandidate = 0;
    private ArrayList<Candidate> scores = new ArrayList<>(population);
    List<float[]> offspring_population = new ArrayList<>(population/2);
    private Random rnd = new Random();
    private static float[] fittestCandidate = new float[NUM_CHROMOSOMES];
    private static float fittestScore = Float.NEGATIVE_INFINITY;
    private static int fittestGeneration = 0;
    private static int fittestIndex = 0;
    private static Candidate bestCandidate = null;


    public GeneticAlgorithm(float[] weights) {
        setUpChromosomes(weights);
    }

    /**
     * Initialise chromosomes with values from -5 to 5.
     * @param weights the weights of the features defined in {@code PlayerSkeleton.java}
     */
    private void setUpChromosomes(float[] weights) {
        chromosomes = new ArrayList<>(population);
        chromosomes.add(weights);
        for (int i = 1; i < population; i++) {
            /* In this portion below, the first line mutates the candidate from parameter.txt slightly to create the
               The second line just generates the population randomly
            */
            chromosomes.add(mutateCandidateRandomly(weights, 1, INITIAL_MUTATION_AMOUNT));
//            chromosomes.add(createRandomChromosome());
        }
    }

    private float[] createRandomChromosome() {
        float[] randomChromosome = new float[NUM_CHROMOSOMES];
        for(int i = 0; i < randomChromosome.length; i++) {
            randomChromosome[i] = (float) (Math.random() - 0.5);
        }

        return normalize(randomChromosome);
    }

    /**
     * Create new generation
     */
    private void createNewGeneration() {
        List<Candidate> winners;
        Collections.sort(scores);
        // Pair 1 with 2, 3 with 4, etc.
        winners = scores.subList(population / 2, population - 1);

        List<float[]> offspring_population = new ArrayList<>(population/2);
        // Pair up two winners at a time
        for (int i = 0; i < (winners.size() / 2); i++) {
            Candidate winner1 = winners.get(i);
            Candidate winner2 = winners.get(i + 1);

            offspring_population.add(
                    mutateCandidateRandomly(
                            mutateByCrossoverCandidates(winner1, winner2),
                            MUTATION_RATE,
                            MUTATION_AMOUNT));
        }

        // Shuffle the offspring population.
        Collections.shuffle(offspring_population, rnd);

        // Replace the least fit PERCENTAGE_OFFSPRING of the population
        chromosomes = chromosomes.subList((int) PERCENTAGE_OFFSPRING * population, population - 1);
        for(int i = 0; chromosomes.size() < population && i < offspring_population.size(); i++ ) {
            chromosomes.add(offspring_population.get(i));
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
        float maxScore = 0;
        for (int i = 0; i < scores.size(); i++) {
            float currScore = scores.get(i).getFitnessScore();
            if (currScore > maxScore) {
                maxIndex = i;
                maxScore = currScore;
            }
        }
        System.out.println("local Generation " + generation + " candid. " + (maxIndex+1) + " chosen (max score: " + maxScore + "): " + aToS(chromosomes.get(maxIndex)));

        if (maxScore > fittestScore) {
            fittestScore = maxScore;
            for (int i = 0; i < NUM_CHROMOSOMES ; i++) {
                fittestCandidate[i] = chromosomes.get(maxIndex)[i];
            }
            fittestGeneration = generation;
            fittestIndex = maxIndex;
        }
    }

    /**
     * Allow the PlayerSkeleton class to send scores for a set of multiplier weights
     * @param score from using a set of multiplier weights
     */
    public void sendScore(float[] weights, float score) {
        String s = aToS(chromosomes.get(currentCandidate));
        String string = "Generation " + generation + "; Candidate " + (currentCandidate + 1) + ": " + s + " Score = " + score;
//        System.out.println(string);
        scores.add(currentCandidate, new Candidate(weights, score));
        currentCandidate++;

        if (scores.size() == population) {
            findFittestCandidate();
            createNewGeneration();
        } else {
            PlayerSkeleton.setMultiplierWeights(chromosomes.get(currentCandidate));
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
            s += Float.toString(((float) Math.round(a[i] * 1000)) / 1000);
            if (i != a.length - 1) {
                s += ", ";
            }
        }
        return "[" + s + "]";
    }

    private float[] mutateCandidateRandomly(float[] candidate, float mutationRate, float mutationAmount) {
        float[] mutant = new float[NUM_CHROMOSOMES];

        for (int k = 0; k < NUM_CHROMOSOMES; k++) {
            float change = 0;
            // Mutation
            boolean mutate = rnd.nextFloat() < mutationRate;
            if (mutate) {
                // Change this value anywhere from -5 to 5
                change = rnd.nextFloat() * mutationAmount * 2 - mutationAmount;

            }

            mutant[k] = candidate[k] + change;
        }

        return normalize(mutant);
    }

    /**
     * Takes in two candidates and generate new values weighted on their fitness scores.
     */
    private float[] mutateByCrossoverCandidates(Candidate candidate1, Candidate candidate2) {
        float[] mutant = new float[NUM_CHROMOSOMES];

        for(int i = 0; i < NUM_CHROMOSOMES; i++) {
            mutant[i] = candidate1.getFitnessScore() * candidate1.getMultiplierWeights()[i]
                      + candidate2.getFitnessScore() * candidate2.getMultiplierWeights()[i];
        }

        return normalize(mutant);
    }

    /**
     * Normalizes the weights of candidate vectors to fit within a unit n-sphere.
     */
    private float[] normalize(float[] candidate) {
        float normal = 0;
        for(float weight : candidate) {
            normal += weight * weight;
        }

        normal = (float) Math.sqrt((double) normal);

        for(int i = 0; i < candidate.length; i++) {
            candidate[i] /= normal;
        }

        return candidate;
    }




}