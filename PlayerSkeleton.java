import java.util.*;

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

	private static double[] multiplierWeights = {0.0013662109544579984, -0.4600434215937764, -0.034335635445010775, -0.4374906238049451, -0.20678731789175717, -0.18742728133125844, -0.277547707281033, -0.04109120096226091, 0.3182590168906832, 0.0011420137900827127, -0.25101385980985025, -0.5180423960705438, 0.08063852524115317, -0.0012020697798553004};

    private static String[] multiplierNames = {
            "ROWS_CLEARED_MULT", "GLITCH_COUNT_MUL", "BUMPINESS_MUL", "TOTAL_HEIGHT_MUL", "MAX_HEIGHT_MUL", "VERTICAL_HOLES_MUL",
            "SUM_OF_WELLS_MUL", "MAX_WELL_DEPTH_MUL", "BLOCKS_MUL", "WEIGHTED_BLOCKS_MUL", "ROW_TRANSITIONS_MUL",
            "COL_TRANSITIONS_MUL", "BALANCE_MUL", "IDEAL_MUL"
	};

	/********************************* End of multipliers *********************************/

	private static boolean visualMode = false;
	private static final int DATA_SIZE = 1;
	private static final int TURNS_LIMIT = Integer.MAX_VALUE;
    private static final int REPETITIONS = 3;
	private static int score = 0;

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
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < legalMoves.length; i++) {
            double score = simulateMove(s, legalMoves[i]);
            if (score > max) {
                maxIdx = i;
                max = score;
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
		
		executeDataSet();
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
		while(counter-- > 0) {
            score = 0;
            State s = new State();
            if (visualMode) {
                visualize(s);
            } else {
                PlayerSkeleton p = new PlayerSkeleton();
                while (!s.hasLost() && (s.getTurnNumber() < TURNS_LIMIT)) {
                   s.makeMove(p.pickMove(s, s.legalMoves()));
                   if (s.getTurnNumber() % 10000 == 0) {
                       System.out.println("Row Cleared: " + s.getRowsCleared());
                   }
                }
            }

//            System.out.println("Final Row Cleared: " + s.getRowsCleared());

            score /= REPETITIONS;
            maxScore = Math.max(maxScore, score);
            minScore = Math.min(minScore, score);
            sum += score;
            var += score * score;

			System.out.println("You have completed " + s.getRowsCleared() + " rows.");
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
