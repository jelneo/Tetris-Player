import java.util.Scanner;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class PlayerSkeleton {

	/********************************* Multipliers to determine value of simulated move *********************************/
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

	/********************************* End of multipliers *********************************/

	private static boolean visualMode = false;
	private static final int DATA_SIZE = 30;

	//implement this function to have a working system
	/**
	 * Picks the move with the highest value.
	 *
	 * @param s - present state
	 * @param legalMoves - List of legal moves
	 * @return the move that has the maximum value based on
	 * {@link PlayerSkeleton#simulateMove(State, int[]) simulateMove} method
	 */
	public int pickMove(State s, int[][] legalMoves) {

		int maxIdx = 0;
		float max = simulateMove(s, legalMoves[0]);
		for (int i = 1; i < legalMoves.length; i++) {
			if (simulateMove(s, legalMoves[i]) > max) {
				maxIdx = i;
				max = simulateMove(s, legalMoves[i]);
			}
		}

		return maxIdx;
	}

	// Simulates a move and returns a float that allows for evaluation. The higher the better.
	public float simulateMove(State s, int[] move) {
		SimulatedState ss = new SimulatedState(s);
		return ss.getMoveValue(move);
	}

	public static void main(String[] args) {
		setVisualMode();
		setParameters();
		printParameters();
		
		executeDataSet();

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
		while(counter-- > 0) {
			State s = new State();

			if (visualMode) {
				visualize(s);
			} else {
				PlayerSkeleton p = new PlayerSkeleton();
				while (!s.hasLost()) {
					s.makeMove(p.pickMove(s, s.legalMoves()));
				}
			}

			maxScore = Math.max(maxScore, s.getRowsCleared());
			minScore = Math.min(minScore, s.getRowsCleared());
			sum += s.getRowsCleared();
			var += s.getRowsCleared() * s.getRowsCleared();
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
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			*/
		}
		window.dispose();
	}


	/********************************* Parameter weight optimization *********************************/
	private static final String PARAM_FILE_NAME = "parameter.txt";

	/**
	 * Sets parameter multiplierWeights for the current iteration. Parameters stored in parameter.txt in same directory as
	 * PlayerSkeleton file. If file is empty, then use default parameters.
	 *
	 * {@link PlayerSkeleton#setParameters(String[])} for information about how the parameters are set.
	 */
	private static void setParameters() {
		// This will reference one line at a time
		String line = null;

		// read first line from parameter.txt
		try {
			FileReader fileReader = new FileReader(PARAM_FILE_NAME);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader =  new BufferedReader(fileReader);

			line = bufferedReader.readLine();

			bufferedReader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

		if (line == null) {
			System.out.println("parameter.txt is empty, using default values");
		} else {
			String[] values = line.split(" ");
			setParameters(values);
		}
	}

	private static void setParameters(String[] values) {
		for (int i = 0; i < NUM_PARAMETERS; i++) {
			multiplierWeights[i] = Float.parseFloat(values[i]);
		}
	}

	/**
	 * Saves parameter multiplierWeights of the current iteration. Parameters stored in parameter.txt in same directory as
	 * PlayerSkeleton file.
	 *
	 * {@link PlayerSkeleton#setParameters(String[])} for information about how the parameters are set.
	 */
	private static void saveParameters() {
		try {
			FileWriter fileWriter =  new FileWriter(PARAM_FILE_NAME);

			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			String line = "" + multiplierWeights[0];
			for (int i = 1; i < NUM_PARAMETERS; i++) {
				line += " " + multiplierWeights[i];
			}
			line += "\n";

			bufferedWriter.write(line);
			bufferedWriter.close();
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

		// Returns the value of making a move
		public float getMoveValue(int move[]) {
			return getMoveValue(move[ORIENT], move[SLOT]);
		}

		public float getMoveValue(int orient, int slot) {
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

			int maxHeight = 0;

			for (int i = 0; i < top.length; i++) {
				if (top[i] > maxHeight) {
					maxHeight = top[i];
				}
			}

			return multiplierWeights[BUMPINESS_MULT_INDEX] * getBumpiness(top)
					+ multiplierWeights[TOTAL_HEIGHT_MULT_INDEX] * getTotalHeight(top)
					+ multiplierWeights[ROWS_CLEARED_MULT_INDEX] * rowsCleared
					+ multiplierWeights[MAX_HEIGHT_MULT_INDEX] * maxHeight
					+ multiplierWeights[GLITCH_COUNT_MULT_INDEX] * getGlitchCount(field, top);
		}

		// Checks for how bumpy the top is
		public float getBumpiness(int[] top) {
			float bumpiness = 0;
			for (int i = 0; i < top.length - 1; i++) {
				bumpiness += Math.abs(top[i] - top[i + 1]);
			}

			return bumpiness;
		}

		// Returns the sum of heights
		public float getTotalHeight(int[] top) {
			float totalHeight = 0;
			for (int i = 0; i < top.length; i++) {
				totalHeight += top[i];
			}

			return totalHeight;
		}

		public float getGlitchCount(int[][] field, int[] top) {
			float glitchCount = 0;

			int[][] temp = new int[ROWS][COLS];

			for (int i = 0; i < COLS; i++) {
				for (int j = 0; j < ROWS; j++) {
					if (j >= top[i]) {
						temp[j][i] = 2;
					} else {
						temp[j][i] = field[j][i];
					}
				}
			}

			for (int r = 0; r < ROWS; r++) {
				for(int c = 0; c < COLS; c++) {
					if (temp[r][c] == 0) {
						// penalize for bigger glitches
						glitchCount = (int) Math.pow(getLocalGlitchSize(temp, r, c), 2);
					}
				}
			}

			return glitchCount;
		}

		/**
		 * Recursive counts the empty holes adjacent to the current square
		 *
		 * @param field - field to count the empty squares from.
		 * @param r - row of the square
		 * @param c - column of the square
		 * @return # of localized glitches
		 */
		private int getLocalGlitchSize(int[][] field, int r, int c) {
			if (r < 0 || r >= ROWS || c < 0 || c >= COLS) {
				return 0;
			}

			if (field[r][c] != 0) {
				return 0;
			}

			int glitchCount = 1;
			field[r][c] = -1;

			/*
			for (int i = r - 1; i <= r + 1; i++) {
				for (int j = c - 1; j <= c + 1; j++) {
					glitchCount += getLocalGlitchSize(field, i, j);
				}
			}
			*/

			for (int i = -1; i <= 1; i++) {
				glitchCount += getLocalGlitchSize(field, r + i, c);
				glitchCount += getLocalGlitchSize(field, r, c + i);
			}

			return glitchCount;
		}
	}

	/********************************* End of nested class for state simulation *********************************/
}

