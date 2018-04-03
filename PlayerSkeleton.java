import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import static java.lang.Integer.parseInt;

public class PlayerSkeleton {


	/********************************* Multipliers to determine value of simulated move *********************************/
    private static final int NUM_PARAMETERS = 14;
    private static final int ROWS_CLEARED_MULT_INDEX = 0;
    private static final int GLITCH_COUNT_MULT_INDEX = 1;
    private static final int BUMPINESS_MULT_INDEX = 2;
    private static final int TOTAL_HEIGHT_MULT_INDEX = 3;
    private static final int MAX_HEIGHT_MULT_INDEX = 4;
    private static final int VERTICALLY_CONNECTED_HOLES_MULT_INDEX = 5;
    private static final int SUM_OF_ALL_WELLS_INDEX = 6;
    private static final int MAX_WELL_DEPTH_INDEX = 7;
    private static final int BLOCKS_INDEX = 8;
    private static final int WEIGHTED_BLOCKS_INDEX = 9;
    private static final int ROW_TRANSITIONS_INDEX = 10;
    private static final int COL_TRANSITIONS_INDEX = 11;
    private static final int BALANCE_INDEX = 12;
    private static final int IDEAL_INDEX = 13;
    private static final int DEFAULT_GENERATION_SIZE = 100;

	// Heavily prioritise objective of row clearing. Other Multipliers used for tiebreakers.
	// initialized to default values
	private static double[] multiplierWeights = {0.5, -0.1, -0.1, -0.5, -0.1, 0.1, 0.1, 0.2, 0.2, 0.3, 0.1, 0.2, 0.2};
	private static String DEFAULT_PARAMETERS = "0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1";
	private static List<double[]> populationMultipliers;


    private static String[] multiplierNames = {
            "ROWS_CLEARED_MULT", "GLITCH_COUNT_MUL", "BUMPINESS_MUL", "TOTAL_HEIGHT_MUL", "MAX_HEIGHT_MUL", "VERTICAL_HOLES_MUL",
            "SUM_OF_WELLS_MUL", "MAX_WELL_DEPTH_MUL", "BLOCKS_MUL", "WEIGHTED_BLOCKS_MUL", "ROW_TRANSITIONS_MUL",
            "COL_TRANSITIONS_MUL", "BALANCE_MUL", "IDEAL_MUL"
	};

	/********************************* End of multipliers *********************************/

	private static boolean visualMode = false;
	private static final int DATA_SIZE = 30;
	private static final int TURNS_LIMIT = 5000;//Integer.MAX_VALUE;
	private static final int SAMPLING_INTERVAL = 100;
	private static GeneticAlgorithm geneticAlgorithm;

	//implement this function to have a working system
	/**
	 * Picks the move with the highest value.
	 *
	 * @param s present state
	 * @param legalMoves List of legal moves
	 * @return the move that has the maximum value based on
	 * {@link PlayerSkeleton#simulateMove(State, int[]) simulateMove} method
	 */
	public int pickMove(State s, int[][] legalMoves) {

		int maxIdx = 0;
		double max = simulateMove(s, legalMoves[0]);
		for (int i = 1; i < legalMoves.length; i++) {
			if (simulateMove(s, legalMoves[i]) > max) {
				maxIdx = i;
				max = simulateMove(s, legalMoves[i]);
			}
		}

		return maxIdx;
	}

	// Simulates a move and returns a double that allows for evaluation. The higher the better.
	public double simulateMove(State s, int[] move) {
		SimulatedState ss = new SimulatedState(s);
		return ss.getMoveValue(move);
	}

	public static void main(String[] args) {
		setVisualMode();
		setParameters();
		printParameters();
		
		executeDataSet();

		multiplierWeights = geneticAlgorithm.getFittestCandidate();
		populationMultipliers = geneticAlgorithm.getLatestPopulation();

		printParameters();
		saveParameters();
	}

	/**
	 * Executes {@link #DATA_SIZE} number of iterations with the current parameter weight values to retrieve.
	 */
	private static void executeDataSet() {
		int maxScore = Integer.MIN_VALUE;
		int minScore = Integer.MAX_VALUE;
		int sum = 0;
		int var = 0;
		int counter = DATA_SIZE; // set to 30 for more accurate sample size

		geneticAlgorithm = new GeneticAlgorithm(populationMultipliers);
		multiplierWeights = populationMultipliers.get(0);
		while(counter-- > 0) {
			State s = new State();
			int score = 0;

			if (visualMode) {
				visualize(s);
			} else {
				PlayerSkeleton p = new PlayerSkeleton();
				while (!s.hasLost() && (s.getTurnNumber() < TURNS_LIMIT)) {
					s.makeMove(p.pickMove(s, s.legalMoves()));
					if (s.getTurnNumber() % SAMPLING_INTERVAL == 0) {
						score += getScore(s);
					}
				}
			}

            System.out.println("Row Cleared: " + s.getRowsCleared());
            geneticAlgorithm.sendScore(multiplierWeights, Math.max(score, 1)); // positive scores only
            maxScore = Math.max(maxScore, score);
            minScore = Math.min(minScore, score);
            sum += score;
            var += score * score;

//			System.out.println("You have completed " + s.getRowsCleared() + " rows.");
        }

        var -= ((double) sum) * ((double) sum) / DATA_SIZE;
        var /= DATA_SIZE - 1;

        System.out.println(" Ave: " + (sum / DATA_SIZE) + " Min: " + minScore + " Max: " + maxScore + " Var: " + var);
    }

    private static void setVisualMode() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Visual Mode? 1 for yes, 0 for no");
        visualMode = sc.nextInt() == 1;
        sc.close();
    }

    private static void visualize(State s) {
        TFrame window = new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();

        while (!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));

            if (visualMode) {
                s.draw();
                s.drawNext(0, 0);
            }

            // This creates a delay, making it harder to test multiple tests
			/*
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			*/

        }
        window.dispose();
    }

    protected static void triggerSaveParameters() {
        populationMultipliers = geneticAlgorithm.getLatestPopulation();
        saveParameters();
    }

    protected static void setMultiplierWeights(double[] newWeights) {
//        System.out.println("to be replaced..." + multiplierWeights[0] + " " + multiplierWeights[1] +" "+ multiplierWeights[2] +" "+ multiplierWeights[3] + " " + multiplierWeights[4]);
        for (int i = 0; i < NUM_PARAMETERS; i++) {
            multiplierWeights[i] = newWeights[i];
        }
//        System.out.println("replaced..." + newWeights[0] + " " + newWeights[1] +" "+ newWeights[2] +" "+ newWeights[3] + " " + newWeights[4]);
    }

    /********************************* Score calculation *********************************************/
    private static final int MAX_HEALTHY_HEIGHT = 7;
    private static final int HOLE_MULTIPLIER = -4;

    /**
     * Returns the health score of the state of the board, based on max height and number of holes
     * @param s current state of the board
     * @return health score
     */
    private static int getScore(State s) {
        int maxHeight = getMaxHeight(s);
        // Increasing bonus given for decreasing heights until MAX_HEALTHY_HEIGHT.
        int heightBonus = (s.getField().length - MAX_HEALTHY_HEIGHT) * (s.getField().length - MAX_HEALTHY_HEIGHT);
        if (maxHeight > MAX_HEALTHY_HEIGHT) {
            heightBonus -= (maxHeight - MAX_HEALTHY_HEIGHT) * (maxHeight - MAX_HEALTHY_HEIGHT);
        }
        int score = heightBonus + HOLE_MULTIPLIER * getHoles(s);
        return score;
    }

    /**
     * Returns the max height of the occupied cells in the state
     */
    private static int getMaxHeight(State s) {
        return getMax(s.getTop());
    }

    /**
     * Returns the maximum element in an integer array
     */
    private static int getMax(int[] arr) {
        int max = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }
        return max;
    }

    /**
     * Returns the number of holes/glitches in the state
     */
    private static int getHoles(State s) {
        int count = 0;
        int[][] field = s.getField();
        int[] top = s.getTop();

        int cols = field[0].length;

        for(int c = 0; c < cols; c++) {
            for(int r = top[c]; r >= 0; r--) {
                if (isEmpty(field[r][c])) {
                    count++;
                }
            }

        }

        return count;
    }

    private static boolean isEmpty(int grid) {
        return grid == 0;
    }

	/********************************* End of score calculation **************************************/


	/********************************* Parameter weight optimization *********************************/
	private static final String PARAM_FILE_NAME = "parameters.txt";

	/**
         * Sets parameter multiplierWeights for the current iteration. Parameters stored in parameters.txt in same directory as
	 * PlayerSkeleton file. If file is empty, then use default parameters.
	 *
	 * {@link PlayerSkeleton#setParameters(String[])} for information about how the parameters are set.
	 */
	private static void setParameters() {
		// This will reference one line at a time
		String line;
		Integer size;

		// read first line from parameters.txt
		try {
			FileReader fileReader = new FileReader(PARAM_FILE_NAME);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader =  new BufferedReader(fileReader);

			line = bufferedReader.readLine();

			if (line == null) {
				System.out.println(PARAM_FILE_NAME + " is empty, using default values");
			} else {
				size = parseInt(line);
				populationMultipliers = new ArrayList<>();
				for (int i = 0; i < size; i++) {
					line = bufferedReader.readLine();
					String[] values;
					if (line == null) {
//						setParameters(DEFAULT_PARAMETERS.split(" "));
                        values = DEFAULT_PARAMETERS.split(" ");
                    } else {
                        values = line.split(" ");
                    }

                    populationMultipliers.add(stringToDouble(values));

                }

                System.out.println("========================================================");
                for (int i = 0; i < populationMultipliers.size(); i++) {
                    System.out.println(Arrays.toString(populationMultipliers.get(i)));
                }
            }

            bufferedReader.close();
        } catch (FileNotFoundException fnfe) {
            populationMultipliers = new ArrayList<>();
            for (int i = 0; i < DEFAULT_GENERATION_SIZE; i++) {
                populationMultipliers.add(GeneticAlgorithm.createRandomChromosome());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    private static void setParameters(String[] values) {
        for (int i = 0; i < NUM_PARAMETERS; i++) {
            System.out.println(values[i]);
            multiplierWeights[i] = Double.parseDouble(values[i]);
            System.out.println(multiplierWeights[i]);
        }
    }

    /**
     * Parses a String array into an array of doubles
     */
    private static double[] stringToDouble(String[] values) {
        double[] result = new double[NUM_PARAMETERS];
        for (int i = 0; i < NUM_PARAMETERS; i++) {
            result[i] = Double.parseDouble(values[i]);
        }

        return result;
    }

    /**
     * Saves parameter multiplierWeights of the current iteration. Parameters stored in parameters.txt in same directory as
     * PlayerSkeleton file.
     *
     * {@link PlayerSkeleton#setParameters(String[])} for information about how the parameters are set.
     */
    private static void saveParameters() {
        try {
            FileWriter fileWriter =  new FileWriter(PARAM_FILE_NAME);

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            String line = populationMultipliers.size() + "\n";
            bufferedWriter.write(line);

            for(int i = 0; i < populationMultipliers.size(); i++) {
                multiplierWeights = populationMultipliers.get(i);
                line = "" + multiplierWeights[0];
                for (int j = 1; j < NUM_PARAMETERS; j++) {
                    line += " " + multiplierWeights[j];
                }
                line += "\n";

                bufferedWriter.write(line);
            }
            bufferedWriter.close();

            System.out.println("PARAMETERS SAFELY SAVED!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints value of parameters
     */
    private static void printParameters() {
        String line = multiplierNames[0] + ": " + multiplierWeights[0];

        for (int i = 1; i < NUM_PARAMETERS; i++) {
            line += " " + multiplierNames[i] + ": " + multiplierWeights[i];
        }

        System.out.println(line);
    }
    /********************************* End of Parameter weight optimization *********************************/



    /********************************* Nested class for state simulation *********************************/

    /**
     * {@code SimulatedState} is a simulated state. This helps to evaluate the value of performing a move without altering
     * the game state.
     */
    class SimulatedState extends State {

        private int field[][];
        private int top[];

        public SimulatedState (State s) {
            // initialize field
            field = new int[s.getField().length][s.getField()[0].length];
            for (int i = 0; i < field.length; i++) {
                for (int j = 0; j < field[0].length; j++) {
                    field[i][j] = s.getField()[i][j];
                }
            }

            // initialize top
            top = new int[s.getTop().length];
            for (int i = 0; i < top.length; i++) {
                top[i] = s.getTop()[i];
            }

            nextPiece = s.getNextPiece();
        }

        /**
         * Returns the value of making a move, defined by weights and parameters.
         */
        public double getMoveValue(int move[]) {
            return getMoveValue(move[ORIENT], move[SLOT]);
        }

        public double getMoveValue(int orient, int slot) {
            //height if the first column makes contact
            int height = top[slot]-getpBottom()[nextPiece][orient][0];
            //for each column beyond the first in the piece
            for(int c = 1; c < pWidth[nextPiece][orient];c++) {
                height = Math.max(height,top[slot+c]-State.getpBottom()[nextPiece][orient][c]);
            }

            // Check if game ended - penalize heavily.
            if(height+State.getpHeight()[nextPiece][orient] >= ROWS) {
                return Integer.MIN_VALUE;
            }

            /********************************* Perform simulation of adding piece *********************************/
            /********************************* Please ignore this chunk (unless necessary) *********************************/
            //for each column in the piece - fill in the appropriate blocks
            for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
                //from bottom to top of brick
                for(int h = height+State.getpBottom()[nextPiece][orient][i]; h < height+State.getpTop()[nextPiece][orient][i]; h++) {
                    field[h][i+slot] = 1;
                }
            }

            //adjust top
            for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
                top[slot+c]=height+State.getpTop()[nextPiece][orient][c];
            }

            int rowsCleared = 0;

            //check for full rows - starting at the top
            for(int r = height+State.getpHeight()[nextPiece][orient]-1; r >= height; r--) {
                //check all columns in the row
                boolean full = true;
                for(int c = 0; c < COLS; c++) {
                    if(field[r][c] == 0) {
                        full = false;
                        break;
                    }
                }
                //if the row was full - remove it and slide above stuff down
                if(full) {
                    rowsCleared++;
                    //for each column
                    for(int c = 0; c < COLS; c++) {

                        //slide down all bricks
                        for(int i = r; i < top[c]; i++) {
                            field[i][c] = field[i+1][c];
                        }
                        //lower the top
                        top[c]--;
                        while(top[c]>=1 && field[top[c]-1][c]==0)
                            top[c]--;
                    }
                }
            }
            /********************************* End of simulation *********************************/



            return multiplierWeights[ROWS_CLEARED_MULT_INDEX] * rowsCleared
                    + multiplierWeights[GLITCH_COUNT_MULT_INDEX] * getGlitchCount()
                    + multiplierWeights[BUMPINESS_MULT_INDEX] * getBumpiness()
                    + multiplierWeights[TOTAL_HEIGHT_MULT_INDEX] * getTotalHeight()
                    + multiplierWeights[MAX_HEIGHT_MULT_INDEX] * getMaxHeight()
                    + multiplierWeights[VERTICALLY_CONNECTED_HOLES_MULT_INDEX] * getVerticalHeightHoles()
                    + multiplierWeights[SUM_OF_ALL_WELLS_INDEX] * getSumofAllWells()
                    + multiplierWeights[MAX_WELL_DEPTH_INDEX] * getMaxWellDepth()
                    + multiplierWeights[BLOCKS_INDEX] * getBlocks()
                    + multiplierWeights[WEIGHTED_BLOCKS_INDEX] * getWeightedBlocks()
                    + multiplierWeights[ROW_TRANSITIONS_INDEX] * getRowTransitions()
                    + multiplierWeights[COL_TRANSITIONS_INDEX] * getColTransitions()
                    + multiplierWeights[BALANCE_INDEX] * getImbalance()
                    + multiplierWeights[IDEAL_INDEX] * getIdealPositions();
        }

        /**
         * Returns the number of glitches (covered holes)
         */
        // Heuristic 2
        private int getGlitchCount() {
            int glitchCount = 0;

            for (int c = 0; c < field[0].length; c++) {
                for (int r = 0; r < top[c]; r++) {
                    if (field[r][c] == 0) {
                        glitchCount++;
                    }
                }
            }

            return glitchCount;
        }

        /**
         * Returns the "bumpiness", or the square of the sum of absolute differences between the highest occupied cells
         * of every pair of adjacent columns.
         */
        // Heuristic 3
        private int getBumpiness() {
            int bumpiness = 0;
            for (int i = 0; i < top.length - 1; i++) {
                bumpiness += Math.pow(Math.abs(top[i] - top[i + 1]), 2);
            }

            return bumpiness;
        }

        /**
         * Returns the aggregate height
         */
        // Heuristic 4
        private int getTotalHeight() {
            int totalHeight = 0;
            for (int i = 0; i < top.length; i++) {
                totalHeight += top[i];
            }

            return totalHeight;
        }

        /**
         * Returns maximum height of the board
         */
        // Heuristic 5
        private int getMaxHeight() {
            int maxHeight = 0;

            for (int i = 0; i < top.length; i++) {
                if (top[i] > maxHeight) {
                    maxHeight = top[i];
                }
            }

            return maxHeight;
        }

        /**
         * Returns the number of vertically counted holes. Each vertically connected hole is counted as one.
         */
        // Heuristic 6
        private int getVerticalHeightHoles() {
            int verticalHoles = 0;
            int[] curr = new int[top.length];

            for (int c = 0; c < COLS; c++) {
                while (curr[c] < top[c]) {
                    if (field[curr[c]][c] == 0) {
                        verticalHoles++;
                        while (field[curr[c]][c] == 0) {
                            curr[c]++;
                        }
                    }

                    curr[c]++;
                }
            }

            return verticalHoles;
        }

        /**
         * Returns the sum of all cells that can be considered wells
         */
        // Heuristic 7
        private int getSumofAllWells() {
            int wellCount = 0;
            for(int c = 0; c < field[0].length; c++) {
                for(int r = top[c]; r < field.length; r++) {
                    if(field[r][c] != 0) break;
                    else if(isWell(r, c)) wellCount++;
                }
            }

            return wellCount;
        }

        /**
         * Returns the depth of the deepest well
         */
        // Heuristic 8
        private int getMaxWellDepth() {
            int maxDepth = 0;
            for (int c = 0; c < field[0].length; c++) {
                int currDepth = 0;
                for(int r = top[c]; r < field.length; r++) {
                    if(field[r][c] != 0) break;
                    else if (isWell(r, c)) currDepth++;
                }
                maxDepth = (currDepth > maxDepth)? currDepth : maxDepth;
            }

            return maxDepth;
        }

        /**
         * Returns true if block at (r,c) is a well
         */
        private boolean isWell(int r, int c) {
            return (((c == 0) && (field[r][c + 1] != 0))
                    || ((c == field[0].length - 1) && (field[r][c - 1] != 0))
                    || ((c != 0) && (c != field[0].length - 1) &&(field[r][c - 1] != 0) && (field[r][c + 1] != 0)));
        }

        /**
         * Returns the number of total occupied cells
         */
        // Heuristic 9
        private int getBlocks() {
            int rows = field.length;
            int cols = field[0].length;
            int blocks = 0;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!isEmpty(field[r][c])) {
                        blocks++;
                    }
                }
            }

            return blocks;
        }

        /**
         * Returns the sum of weighted blocks. Each block is multiplied by its height (with the bottom row having a
         * height of 1)
         */
        // Heuristic 10
        private int getWeightedBlocks() {
            int rows = field.length;
            int cols = field[0].length;
            int blocks = 0;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!isEmpty(field[r][c])) {
                        blocks += r + 1;
                    }
                }
            }

            return blocks;
        }

        /**
         * Returns the number of occupied-unoccupied row transitions (and vice versa). The outside columns on either
         * side are considered occupied.
         */
        // Heuristic 11
        private int getRowTransitions() {
            int rows = field.length;
            int cols = field[0].length;
            int transitions = 0;
            for (int r = 0; r < rows; r++) {
                boolean grid = true;
                for (int c = 0; c < cols; c++) {
                    if (isEmpty(field[r][c]) && grid) {
                        grid = false;
                        transitions++;
                    } else if (!isEmpty(field[r][c]) && !grid) {
                        grid = true;
                        transitions++;
                    }
                }

                if (!grid) {
                    transitions++;
                }
            }

            return transitions;
        }

        /**
         * Returns the number of occupied-unoccupied column transitions (and vice versa). The outside row on the bottom
         * is considered occupied.
         */
        // Heuristic 12
        private int getColTransitions() {
            int rows = field.length;
            int cols = field[0].length;
            int transitions = 0;
            for (int c = 0; c < cols; c++) {
                boolean grid = true;
                for (int r = 0; r < rows; r++) {
                    if (isEmpty(field[r][c]) && grid) {
                        grid = false;
                        transitions++;
                    } else if (!isEmpty(field[r][c]) && !grid) {
                        grid = true;
                        transitions++;
                    }
                }
            }

            return transitions;
        }

        /**
         * Returns the total number of occupied cells in a column of the field
         */
        private int getBlocksInCol(int col) {
            int count = 0;

            int rows = field.length;

            for(int r = 0; r < rows; r++) {
                if (!isEmpty(field[r][col])) {
                    count++;
                }
            }

            return count;
        }

        /**
         * Returns the imbalance, given by the sum of the absolute differences of the number of occupied cells between
         * every pair of adjacent columns.
         */
        // Heuristic 13
        private int getImbalance() {
            int cols = field[0].length;

            int imbalance = 0;

            for(int c = 0; c < cols - 1; c++) {
                imbalance += Math.abs(getBlocksInCol(c) - getBlocksInCol(c + 1));
            }

            return imbalance;
        }

        // Heuristic 14
        private int getIdealPositions() {
            int[][] idealPositions = {{3, 0}, {2, -1}, {2, 1}, {1, 2}, {1, -2}, {3, 0, 0}, {1, 0, 1}, {1, -1, -1},
                    {1, 1, 1}, {1, 0, -1}, {1, -1, 0}, {1, 0, 0, 0}};
            int positions = 0;

            for (int i = 0; i < top.length; i++) {
                for (int j = 0; j < idealPositions.length; j++) {
                    if (checkIdealPosition(idealPositions[j], i, top)) {
                        positions += idealPositions[j][0];
                    }
                }
            }

            return positions;

        }

        private boolean checkIdealPosition(int[] idealPosition, int pos, int[] top) {
            if (pos + idealPosition.length > top.length) {
                return false;
            } else {
                int first = top[pos];
                for (int i = 1; i < idealPosition.length; i++) {
                    if (first + idealPosition[i] != top[pos + i]) {
                        return false;
                    }
                }

                return true;
            }
        }

        /**
         * Checks whether a grid is empty
         */
        private boolean isEmpty(int grid) {
            return grid == 0;
        }

    }

    /********************************* End of nested class for state simulation *********************************/
}
