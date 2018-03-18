import java.util.*;

/**
 * A genetic algorithm that is used to evolve the Tetris AI and find the best weight multiplier for each feature
 */
public class GeneticAlgorithm {
    private static final int NUM_CHROMOSOMES = 5;
    private static final float PERCENTAGE_OFFSPRING = 0.3f;
    private static final float MUTATION_AMOUNT = 0.2f;
    // we want the first mutation to occur with higher probability to get out of local maximas
    private static final float INITIAL_MUTATION_AMOUNT = 10 * MUTATION_AMOUNT;
    private int population = 100;
    private int generation = 1;
    private final float MUTATION_RATE = 0.2f;
    private List<float[]> chromosomes = new ArrayList<>();
    private int currentCandidate = 0;
    private static ArrayList<Candidate> scores = new ArrayList<>();
    private Random rnd = new Random();
    private static float[] fittestCandidate = new float[NUM_CHROMOSOMES];
    private static float fittestScore = Float.NEGATIVE_INFINITY;
    private static int fittestGeneration = 0;
    private static int fittestIndex = 0;


    public GeneticAlgorithm(List<float[]> weights) {
        setUpChromosomes(weights);
    }

    /**
     * Initialise chromosomes with values from parameters.txt.
     * parameters.txt should have a population to be loaded into {@code chromosomes}, else a new population has to be
     * generated using {@link #createRandomChromosome()}
     * @param weights the weights of the population in parameters.txt
     */
    private void setUpChromosomes(List<float[]> weights) {
        chromosomes.addAll(weights);
        population = weights.size();
        System.out.println("Population: " +  population);
    }

    /**
     * Creates random chromosomes with in the range -0.5 to 0.5.
     * This function is only used when there is no initial population in parameters.txt.
     * @return a normalized set of NUM_CHROMOSOMES random chromosomes
     */
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
        Collections.sort(scores);
        // Pair 1 with 2, 3 with 4, etc.

        List<float[]> offspring_population = new ArrayList<>(population/2);
        // Pair up two winners at a time
        for (int i = 0; i < population - 1; i++) {
            Candidate winner1 = scores.get(population - i - 1);
            Candidate winner2 = scores.get(population - i - 2);

//            System.out.println(Arrays.toString(winner1.getMultiplierWeights()));
//            System.out.println(Arrays.toString(winner2.getMultiplierWeights()));

            float[] firstChild = mutateByCrossoverCandidates(winner1, winner2);
//            System.out.println("first child: " + Arrays.toString(firstChild));
//            offspring_population.add(firstChild);

            for (int j = 0; j < 2; j++) {
                offspring_population.add(
                        mutateCandidateRandomly(firstChild,
                                MUTATION_RATE,
                                MUTATION_AMOUNT));
            }
        }

        // Shuffle the offspring population.
        Collections.shuffle(offspring_population, rnd);

        // Replace the least fit PERCENTAGE_OFFSPRING of the population
//        ArrayList<float[]> newChromosomes = new ArrayList<>(population);
        int numberOfWinners = (int) (population * (1 - PERCENTAGE_OFFSPRING)) + 1;

        System.out.println("SIZE: " + offspring_population.size());

        chromosomes = new ArrayList<>();

        for(int i = 0; i < numberOfWinners; i++) {
            chromosomes.add(scores.get(population - 1 - i).getMultiplierWeights());
        }

        for(int k = 0; k < population - numberOfWinners; k++) {
            chromosomes.add(offspring_population.get(k));
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
        System.out.println("local Generation " + generation + " candid. " + (maxIndex+1) + " chosen (max score: " + maxScore + "): " + arrayToString(chromosomes.get(maxIndex)));

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
        String s = arrayToString(chromosomes.get(currentCandidate));
        String string = "Generation " + generation + "; Candidate " + (currentCandidate + 1) + ": " + s + " Score = " + score;
        System.out.println(string);
        scores.add(currentCandidate, new Candidate(weights, score));
        currentCandidate++;

        if (scores.size() == population) {
            findFittestCandidate();
            createNewGeneration();
            PlayerSkeleton.setMultiplierWeights(chromosomes.get(0));
        } else {
//            System.out.println("Current chromosome: " + Arrays.toString(chromosomes.get(currentCandidate)));
            PlayerSkeleton.setMultiplierWeights(chromosomes.get(currentCandidate));
        }
    }

    /**
     * Returns the best set of multiplier weights
     */
    public float[] getFittestCandidate() {
        System.out.println("fittest score: " + fittestScore + " from generation " + fittestGeneration + " candid. " + (fittestIndex+1)+" : " + arrayToString(fittestCandidate));
        return fittestCandidate;
    }

    /**
     * Returns the latest population
     */
    public List<float[]> getLatestPopulation() {
        return chromosomes;
    }

    /**
     * Converts float array to string
     * @param a a float[] array
     * @return an properly formatted string of array contents
     */
    private String arrayToString(float[] a) {
        String s = "";
        for (int i = 0; i < a.length; i++) {
            s += Float.toString(((float) Math.round(a[i] * 1000)) / 1000);
            if (i != a.length - 1) {
                s += ", ";
            }
        }
        return "[" + s + "]";
    }

    /**
     * Mutate a candidate randomly with a mutation rate of {@code mutationRate}
     * @param candidate candidate to be mutated
     * @param mutationRate mutation rate
     * @param mutationAmount a float number that is used to mutate the chromosome
     * @return
     */
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

        if (normal == 0) {
            return candidate;
        } else {
            for (int i = 0; i < candidate.length; i++) {
                candidate[i] /= normal;
            }

            return candidate;
        }
    }

}