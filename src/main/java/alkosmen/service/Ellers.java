package src.main.java.alkosmen.service;

import java.util.*;

/**
 * Maze generator using Eller's algorithm (row-by-row).
 *
 * Produces grid with size (2*cols+1) x (2*rows+1):
 * 1 = wall, 0 = passage.
 *
 * Typical usage:
 *   Ellers e = new Ellers(rows, cols);
 *   int[][] maze = e.generate();
 *   e.printMaze();
 */
public class Ellers {

    private final int rows;
    private final int cols;

    // grid[y][x] : 1 wall, 0 passage
    private int[][] grid;

    // Sets for current row
    private int[] setId;
    private int nextSetId = 1;

    private final Random rnd = new Random();

    public Ellers(int rows, int cols) {
        if (rows <= 0 || cols <= 0) throw new IllegalArgumentException("rows/cols must be > 0");
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * Backward compatible method name if your code calls makeMaze(...).
     * Parameter is ignored (kept for compatibility with your call ell.makeMaze(p)).
     */
    public void makeMaze(Object ignored) {
        generate();
    }

    /**
     * Generates and returns maze grid.
     */
    public int[][] generate() {
        // init all walls
        grid = new int[2 * rows + 1][2 * cols + 1];
        for (int y = 0; y < grid.length; y++) {
            Arrays.fill(grid[y], 1);
        }

        // prepare first row sets
        setId = new int[cols];
        for (int x = 0; x < cols; x++) {
            setId[x] = nextSetId++;
        }

        for (int r = 0; r < rows; r++) {
            // open cells in this row
            for (int c = 0; c < cols; c++) {
                carveCell(r, c);
            }

            // horizontal joins (except last row handled differently)
            if (r < rows - 1) {
                horizontalStep(r);
                verticalStep(r);
                prepareNextRow();
            } else {
                // last row: must connect all sets horizontally
                lastRowConnect(r);
            }
        }

        // add entrances (optional): top-left and bottom-right
        grid[1][0] = 0;
        grid[2 * rows - 1][2 * cols] = 0;

        return grid;
    }

    public int[][] getGrid() {
        return grid;
    }

    public void printMaze() {
        if (grid == null) {
            System.out.println("(maze not generated yet)");
            return;
        }
        for (int y = 0; y < grid.length; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < grid[0].length; x++) {
                sb.append(grid[y][x] == 1 ? '#' : ' ');
            }
            System.out.println(sb);
        }
    }

    // ---- internal steps ----

    private void carveCell(int r, int c) {
        int gy = 2 * r + 1;
        int gx = 2 * c + 1;
        grid[gy][gx] = 0;
    }

    private void carveRightWall(int r, int c) {
        int gy = 2 * r + 1;
        int gx = 2 * c + 2; // wall between cells
        grid[gy][gx] = 0;
    }

    private void carveDownWall(int r, int c) {
        int gy = 2 * r + 2; // wall between rows
        int gx = 2 * c + 1;
        grid[gy][gx] = 0;
    }

    /**
     * Randomly join adjacent cells horizontally where they are in different sets.
     */
    private void horizontalStep(int r) {
        for (int c = 0; c < cols - 1; c++) {
            if (setId[c] != setId[c + 1] && rnd.nextBoolean()) {
                // remove right wall
                carveRightWall(r, c);
                // merge sets: replace setId[c+1] -> setId[c]
                int from = setId[c + 1];
                int to = setId[c];
                for (int i = 0; i < cols; i++) {
                    if (setId[i] == from) setId[i] = to;
                }
            }
        }
    }

    /**
     * For each set, create at least one vertical connection downward.
     * Randomly carve down walls for some cells in each set.
     */
    private void verticalStep(int r) {
        // group columns by set
        Map<Integer, List<Integer>> bySet = new HashMap<>();
        for (int c = 0; c < cols; c++) {
            bySet.computeIfAbsent(setId[c], k -> new ArrayList<>()).add(c);
        }

        // decide vertical openings
        boolean[] goesDown = new boolean[cols];

        for (Map.Entry<Integer, List<Integer>> e : bySet.entrySet()) {
            List<Integer> cells = e.getValue();

            // ensure at least one goes down
            int must = cells.get(rnd.nextInt(cells.size()));
            goesDown[must] = true;

            // other cells randomly
            for (int c : cells) {
                if (c != must && rnd.nextBoolean()) {
                    goesDown[c] = true;
                }
            }
        }

        // carve down walls for selected cells
        for (int c = 0; c < cols; c++) {
            if (goesDown[c]) {
                carveDownWall(r, c);
            } else {
                // if no down passage, next row cell will get a new set id
                // handled in prepareNextRow()
            }
        }

        // store goesDown in a field-like pattern by reusing special marker: 0 means new set later
        for (int c = 0; c < cols; c++) {
            if (!goesDown[c]) setId[c] = -setId[c]; // mark as NOT going down
        }
    }

    /**
     * Prepare set ids for next row.
     * Cells that had a vertical connection keep their set.
     * Cells without a vertical connection get new unique sets.
     */
    private void prepareNextRow() {
        for (int c = 0; c < cols; c++) {
            if (setId[c] < 0) {
                // did not go down -> new set
                setId[c] = nextSetId++;
            }
            // else keep the set
        }
    }

    /**
     * Last row: connect all adjacent different sets (must become one set).
     */
    private void lastRowConnect(int r) {
        for (int c = 0; c < cols - 1; c++) {
            if (setId[c] != setId[c + 1]) {
                carveRightWall(r, c);
                int from = setId[c + 1];
                int to = setId[c];
                for (int i = 0; i < cols; i++) {
                    if (setId[i] == from) setId[i] = to;
                }
            }
        }
    }
}
