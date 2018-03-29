public class Candidate implements Comparable<Candidate>{
    private static final int NUM_PARAMETERS = 14;

    // Heavily prioritise objective of row clearing. Other Multipliers used for tiebreakers.
    // initialized to default values
    private double[] multiplierWeights = {0.5f, -0.1f, -01.f, -0.5f, -0.1f, 0.1f, 0.1f, 0.2f, 0.2f, 0.3f, 0.1f, 0.2f, 0.2f, 0.2f};
    private double fitnessScore = 0;

    public Candidate(double[] weights, double fitness) {
        for (int i = 0; i < NUM_PARAMETERS; i++) {
            multiplierWeights[i] = weights[i];
        }
        fitnessScore = fitness;
    }

    public double getFitnessScore() {
        return fitnessScore;
    }

    public double[] getMultiplierWeights() {
        return multiplierWeights;
    }


    @Override
    public int compareTo(Candidate other) {
        return Double.compare(fitnessScore, other.fitnessScore);
    }
}
