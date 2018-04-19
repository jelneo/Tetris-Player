import java.util.*;

/**
 * A genetic algorithm that is used to evolve the Tetris AI and find the best weight multiplier for each feature
 */
public class GeneticAlgorithm {
    private static final int NUM_CHROMOSOMES = 14;
    private static final double PERCENTAGE_OFFSPRING = 0.3;
    private static final double MUTATION_AMOUNT = 0.2;
    // we want the first mutation to occur with higher probability to get out of local maximas
    private static final double INITIAL_MUTATION_AMOUNT = 10 * MUTATION_AMOUNT;
    private int population = 10;
    private int generation = 1;
    private final double MUTATION_RATE = 0.05;
    private List<double[]> chromosomes = new ArrayList<>();
    private int currentCandidate = 0;
    private static ArrayList<Candidate> scores = new ArrayList<>();
    private Random rnd = new Random();
    private static double[] fittestCandidate = new double[NUM_CHROMOSOMES];
    private static double fittestScore = Double.NEGATIVE_INFINITY;
    private static int fittestGeneration = 0;
    private static int fittestIndex = 0;


    public GeneticAlgorithm(List<double[]> weights) {
        setUpChromosomes(weights);
    }

    /**
     * Initialise chromosomes with values from parameters.txt.
     * parameters.txt should have a population to be loaded into {@code chromosomes}, else a new population has to be
     * generated using {@link #createRandomChromosome()}
     * @param weights the weights of the population in parameters.txt
     */
    private void setUpChromosomes(List<double[]> weights) {
        chromosomes.addAll(weights);
        population = weights.size();
        System.out.println("Population: " +  population);
    }

    /**
     * Creates random chromosomes with in the range -0.5 to 0.5.
     * This function is only used when there is no initial population in parameters.txt.
     * @return a normalized set of NUM_CHROMOSOMES random chromosomes
     */
    public static double[] createRandomChromosome() {
        double[] randomChromosome = new double[NUM_CHROMOSOMES];
        for(int i = 0; i < randomChromosome.length; i++) {
            randomChromosome[i] = Math.random() - 0.5;
        }

        return normalize(randomChromosome);
    }

    /**
     * Create new generation
     */
    private void createNewGeneration() {
        Collections.sort(scores);
        // Pair 1 with 2, 3 with 4, etc.

        List<double[]> offspring_population = new ArrayList<>(population/2);
        // Pair up two winners at a time
        int offspringNumber = (int) (((1 - PERCENTAGE_OFFSPRING) * population + 1) / 2) + 1;
        for (int i = 0; i < offspringNumber; i++) {
            Candidate candidate1 = scores.get(population - i - 1);
            Candidate candidate2 = scores.get(population - i - 2);

            double[] offspring = mutateByCrossoverCandidates(candidate1, candidate2);

            for (int j = 0; j < 2; j++) {
                offspring_population.add(
                        mutateCandidateRandomly(offspring,
                                MUTATION_RATE,
                                MUTATION_AMOUNT));
            }
        }

        // Shuffle the offspring population.
        Collections.shuffle(offspring_population, rnd);

        // Replace the least fit PERCENTAGE_OFFSPRING of the population
//        ArrayList<double[]> newChromosomes = new ArrayList<>(population);
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
        double maxScore = 0;
        for (int i = 0; i < scores.size(); i++) {
            double currScore = scores.get(i).getFitnessScore();
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
    public void sendScore(double[] weights, double score) {
        String s = arrayToString(chromosomes.get(currentCandidate));
        String string = "Generation " + generation + "; Candidate " + (currentCandidate + 1) + ": " + s + " Score = " + score;
        System.out.println(string);
        scores.add(currentCandidate, new Candidate(weights, score));
        currentCandidate++;
        if (scores.size() == population) {
            findFittestCandidate();
            createNewGeneration();
            PlayerSkeleton.setMultiplierWeights(chromosomes.get(0));
            PlayerSkeleton.triggerSaveParameters();
        } else {
//            System.out.println("Current chromosome: " + Arrays.toString(chromosomes.get(currentCandidate)));
            PlayerSkeleton.setMultiplierWeights(chromosomes.get(currentCandidate));
        }
    }

    /**
     * Returns the best set of multiplier weights
     */
    public double[] getFittestCandidate() {
        System.out.println("fittest score: " + fittestScore + " from generation " + fittestGeneration + " candid. " + (fittestIndex+1)+" : " + arrayToString(fittestCandidate));
        return fittestCandidate;
    }

    /**
     * Returns the latest population
     */
    public List<double[]> getLatestPopulation() {
        return chromosomes;
    }

    /**
     * Converts double array to string
     * @param a a double[] array
     * @return an properly formatted string of array contents
     */
    private String arrayToString(double[] a) {
        String s = "";
        for (int i = 0; i < a.length; i++) {
            s += Double.toString(((double) Math.round(a[i] * 1000)) / 1000);
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
     * @param mutationAmount a double number that is used to mutate the chromosome
     * @return an array containing the mutated weights
     */
    private double[] mutateCandidateRandomly(double[] candidate, double mutationRate, double mutationAmount) {
        double[] mutant = new double[NUM_CHROMOSOMES];

        for (int k = 0; k < NUM_CHROMOSOMES; k++) {
            double change = 0;
            // Mutation
            boolean mutate = rnd.nextDouble() < mutationRate;
            if (mutate) {

                change = mutationAmount * (rnd.nextDouble() * 2 - 1);
            }

            mutant[k] = candidate[k] + change;
        }

        return normalize(mutant);
    }

    /**
     * Takes in two candidates and generate new values weighted on their fitness scores.
     */
    private double[] mutateByCrossoverCandidates(Candidate candidate1, Candidate candidate2) {
        double[] mutant = new double[NUM_CHROMOSOMES];

        for(int i = 0; i < NUM_CHROMOSOMES; i++) {
            mutant[i] = candidate1.getFitnessScore() * candidate1.getMultiplierWeights()[i]
                      + candidate2.getFitnessScore() * candidate2.getMultiplierWeights()[i];
        }

        return normalize(mutant);
    }

    /**
     * Normalizes the weights of candidate vectors to fit within a unit n-sphere.
     */
    private static double[] normalize(double[] candidate) {
        double normal = 0;
        for(double weight : candidate) {
            normal += weight * weight;
        }

        normal = Math.sqrt(normal);

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
