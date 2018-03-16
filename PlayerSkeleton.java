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
	private static final int NUM_PARAMETERS = 5;
	private static final int ROWS_CLEARED_MULT_INDEX = 0;
	private static final int GLITCH_COUNT_MULT_INDEX = 1;
	private static final int BUMPINESS_MULT_INDEX = 2;
	private static final int TOTAL_HEIGHT_MULT_INDEX = 3;
	private static final int MAX_HEIGHT_MULT_INDEX = 4;

	// Heavily prioritise objective of row clearing. Other Multipliers used for tiebreakers.
	// initialized to default values
	private static float[] multiplierWeights = {0.5f, -0.1f, -01.f, -0.5f, -0.1f};
	private static String DEFAULT_PARAMETERS = "0.1 0.1 0.1 0.1 0.1";
	private static List<float[]> populationMultipliers;

	private static String[] multiplierNames = {
		"ROWS_CLEARED_MULT", "GLITCH_COUNT_MUL", "BUMPINESS_MUL", "TOTAL_HEIGHT_MUL", "MAX_HEIGHT_MUL"
	};

	/********************************* End of multipliers *********************************/

	private static boolean visualMode = false;
	private static final int DATA_SIZE = 1000;
	private static final int TURNS_LIMIT = 200000;
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

			if (visualMode) {
				visualize(s);
			} else {
				PlayerSkeleton p = new PlayerSkeleton();
				while (!s.hasLost() && (s.getTurnNumber() < TURNS_LIMIT)) {
					s.makeMove(p.pickMove(s, s.legalMoves()));
				}
			}
			geneticAlgorithm.sendScore(multiplierWeights, s.getRowsCleared() + 1); // no 0 scores
			maxScore = Math.max(maxScore, s.getRowsCleared());
			minScore = Math.min(minScore, s.getRowsCleared());

			sum += s.getRowsCleared();
			var += s.getRowsCleared() * s.getRowsCleared();
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
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			*/
		}
		window.dispose();
	}

	protected static void setMultiplierWeights(float[] newWeights) {
//        System.out.println("to be replaced..." + multiplierWeights[0] + " " + multiplierWeights[1] +" "+ multiplierWeights[2] +" "+ multiplierWeights[3] + " " + multiplierWeights[4]);
		for (int i = 0; i < NUM_PARAMETERS; i++) {
		    multiplierWeights[i] = newWeights[i];
        }
//        System.out.println("replaced..." + newWeights[0] + " " + newWeights[1] +" "+ newWeights[2] +" "+ newWeights[3] + " " + newWeights[4]);
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
		Integer size;

		// read firstx line from parameter.txt
		try {
			FileReader fileReader = new FileReader(PARAM_FILE_NAME);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader =  new BufferedReader(fileReader);

			line = bufferedReader.readLine();

			if (line == null) {
				System.out.println("parameter.txt is empty, using default values");
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

					populationMultipliers.add(stringToFloat(values));

				}

				System.out.println("========================================================");
				for (int i = 0; i < populationMultipliers.size(); i++) {
					System.out.println(Arrays.toString(populationMultipliers.get(i)));
				}
			}

			bufferedReader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	private static void setParameters(String[] values) {
		for (int i = 0; i < NUM_PARAMETERS; i++) {
			System.out.println(values[i]);
			multiplierWeights[i] = Float.parseFloat(values[i]);
			System.out.println(multiplierWeights[i]);
		}
	}

	private static float[] stringToFloat(String[] values) {
		float[] result = new float[NUM_PARAMETERS];
		for (int i = 0; i < NUM_PARAMETERS; i++) {
			result[i] = Float.parseFloat(values[i]);
		}

		return result;
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
					+ multiplierWeights[MAX_HEIGHT_MULT_INDEX] * getBalance(field)
					+ multiplierWeights[GLITCH_COUNT_MULT_INDEX] * getHoles(field);
		}

		// Checks for how bumpy the top is
		public float getBumpiness(int[] top) {
			float bumpiness = 0;
			for (int i = 0; i < top.length - 1; i++) {
				bumpiness += Math.pow(Math.abs(top[i] - top[i + 1]), 2);
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

		public float getGlitchCount(int[][] field) {
			float glitchCount = 0;
			for (int r = 1; r < field.length; r++) {
				for(int c = 0; c < field[r].length; c++) {
					if ((field[r][c] == 0) && (field[r - 1][c] != 0)) {
						glitchCount++;
					}
				}
			}
			return glitchCount;
		}

		private int getHoles(int[][] field) {
			int count = 0;

			int rows = field.length;
			int cols = field[0].length;

			for(int c = 0; c < cols; c++) {
				boolean capped = false;
				for(int r = rows - 1; r >= 0; r--) {
					if(!isEmpty(field[r][c])) {
						capped = true;
					} else if (isEmpty(field[r][c]) && capped)
						count++;
				}

			}

			return count;
		}

		private int getBalance(int[][] field) {
			int cols = field[0].length;

			int balanceness = 0;

			for(int c = 0; c < cols - 1; c++) {
				balanceness += Math.abs(getGridsInCol(field, c) - getGridsInCol(field, c + 1));
			}

			return balanceness;
		}

		private int getGridsInCol(int[][] field, int col) {
			int count = 0;

			int rows = field.length;

			for(int r = 0; r < rows; r++) {
				if (!isEmpty(field[r][col])) {
					count++;
				}
			}

			return count;
		}


		private boolean isEmpty(int grid) {
			return grid == 0;
		}
	}

	/********************************* End of nested class for state simulation *********************************/
}

