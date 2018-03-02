public class Candidate {
    private static final int NUM_PARAMETERS = 5;
    private static final int ROWS_CLEARED_MULT_INDEX = 0;
    private static final int GLITCH_COUNT_MULT_INDEX = 1;
    private static final int BUMPINESS_MULT_INDEX = 2;
    private static final int TOTAL_HEIGHT_MULT_INDEX = 3;
    private static final int MAX_HEIGHT_MULT_INDEX = 4;

    // Heavily prioritise objective of row clearing. Other Multipliers used for tiebreakers.
    // initialized to default values
    private static float[] multiplierWeights = {10f, -0.1f, -01.f, -0.5f, -0.1f};

    private static String[] multiplierNames = {
            "ROWS_CLEARED_MULT", "GLITCH_COUNT_MUL", "BUMPINESS_MUL", "TOTAL_HEIGHT_MUL", "MAX_HEIGHT_MUL"
    };

    public Candidate(float[] weights) {
        multiplierWeights = weights;
    }

    public void normalize() {
        float normal = 0;
        for(float weight : multiplierWeights) {
            normal += weight * weight;
        }

        normal = (float) Math.sqrt((double) normal);

        for(int i = 0; i < multiplierWeights.length; i++) {
            multiplierWeights[i] /= normal;
        }
    }

    public float[] getMultiplierWeights() {
        return multiplierWeights;
    }
}
