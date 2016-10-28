//package sudoku;

/**
 * Place for your code.
 */
public class SudokuSolver {
    /**
     * @return names of the authors and their student IDs (1 per line).
     */
    public String authors() {
        return "Fred Park, 38152138";
    }

    /**
     * Performs constraint satisfaction on the given Sudoku board using Arc Consistency and Domain Splitting.
     *
     * @param board the 2d int array representing the Sudoku board. Zeros indicate unfilled cells.
     * @return the solved Sudoku board
     */
    public int[][] solve(int[][] board) throws Exception {

        // add an extra dimension to the board that stores each cell's domain
        int[][][] domainBoard = cellDomain(board);

        // initial pass using arc consistency (AC)
        int dirty = 1;
        while (dirty > 0) {
            // initialize dirty to 0; if AC does SOMETHING, dirty > 0 and breaks loop
            dirty = 0;
            // start reducing the domains of every unfilled cell
            for (int i = 0; i < domainBoard.length; i++) {
                for (int j = 0; j < domainBoard[i].length; j++) {
                    // the value of the 0th index indicates a filled cell - if empty, use AC
                    if (domainBoard[i][j][0] == 0) {
                        try {
                            // check constraints in quadrant and row/column
                            dirty += checkQuadrant(domainBoard, i, j);
                            // if domain has only one variable left, fill the cell with that value
                            cleanDomain(domainBoard[i][j]);
                            dirty += checkCross(domainBoard, i, j);
                            cleanDomain(domainBoard[i][j]);
                        } catch (IllegalStateException e) {
                            throw new Exception("Invalid Board from given. No solution.");
                        }
                    }
                }
            }
        }
        // if first pass of AC fails, split domain
        if (!checkSolved(domainBoard))
            domainBoard = splitDomain(domainBoard, true);

        // domain splitting can't produce a result - no solution
        if (domainBoard == null)
            throw new Exception("No solution!");

        // check if changing order of recursion produces a different solution
        checkUniqueSolution(cellDomain(board), domainBoard);

        // convert the solved board into a returnable board
        for (int i = 0; i < domainBoard.length; i++)
            for (int j = 0; j < domainBoard[i].length; j++)
                board[i][j] = domainBoard[i][j][0];

        return board;
    }

    // convert board to have an additional dimension to hold domain for each cell/variable
    public int [][][] cellDomain(int[][] board) {
        int[][][] domainBoard = new int[9][9][10];

        for (int i = 0; i < domainBoard.length; i++)
            for (int j = 0; j < domainBoard[i].length; j++) {
                domainBoard[i][j][0] = board[i][j];
                if (domainBoard[i][j][0] == 0)
                    // domain from 0-9, 0 indicating it's unfilled
                    for (int k = 1; k < domainBoard[i][j].length; k++)
                        domainBoard[i][j][k] = k;
            }
        return domainBoard;
    }

    // check for binary consistencies from the given cell's index by pruning any domain values
    // that conflict with already filled cells found in corresponding quadrant
    public int checkQuadrant(int[][][] board, int row, int col) {
        int changed = 0;
        // set start index to correct quadrant
        int startRow = (row/3)*3;
        int startCol = (col/3)*3;
        for (int i = startRow; i < startRow+3; i++) {
            for (int j = startCol; j <startCol+3; j++) {
                // compare filled cells to variable's domain, and ignore itself
                if (board[i][j][0] != 0 && (i != row && j != col)) {

                    // indicator that there is a conflict - can prune this path
                    if (board[i][j][0] == board[row][col][0])
                        throw new IllegalStateException();

                    // can prune value from domain
                    if (board[row][col][board[i][j][0]]!=0) {
                        board[row][col][board[i][j][0]] = 0;
                        changed = 1;
                    }
                }
            }
        }
        return changed;
    }

    // check for binary consistencies from the given cell's index by pruning any domain values
    // that conflict with already filled cells found in corresponding row and column
    public int checkCross(int[][][] board, int row, int col) {
        int changed = 0;
        for (int i = 0; i < board.length; i++) {
            // check row & ignore own cell
            if (board[row][i][0] != 0 && i != col) {
                // indicator that there is a conflict - can prune this path
                if (board[row][i][0] == board[row][col][0])
                    throw new IllegalStateException();

                // can prune value from domain
                if (board[row][col][board[row][i][0]]!=0) {
                    board[row][col][board[row][i][0]] = 0;
                    changed = 1;
                }
            }
            // check column & ignore own cell
            if (board[i][col][0] != 0 && i != row) {
                if (board[i][col][0] == board[row][col][0])
                    throw new IllegalStateException();

                if (board[row][col][board[i][col][0]]!=0) {
                    board[row][col][board[i][col][0]] = 0;
                    changed = 1;
                }
            }
        }
        return changed;
    }

    // if a variable's domain has one value left, fill cell with that value
    public void cleanDomain(int[] domain) {
        if (domain[0] != 0) return; // already filled - return

        int count = 0;
        int index = 0;
        for (int m = 1; m < 10; m++) {
            if (domain[m] != 0) {
                count++;
                index = m;
                if (count > 1) return; // more than 1 domain - return
            }
        }
        if (count==0) return; // domain is empty - return
        domain[0] = domain[index];
    }

    // check if every cell is filled
    public boolean checkSolved(int[][][] board) {
        for (int i = 0; i < board.length; i++)
            for (int j = 0; j < board[i].length; j++)
                if (board[i][j][0] == 0)
                    return false;
        return true;
    }

    // Find first unsolved variable, split its domain and attempt to solve
    public int[][][] splitDomain(int[][][] board, boolean inOrder) throws Exception {
        int[] index = findFirstEmpty(board);
        if (index[0] == -1) // Board is filled, return solution
            return board;

        int size = getDomainSize(board[index[0]][index[1]]);
        int leftDom = size/2; // left domain size
        int rightDom = size - leftDom; // right domain size

        int[][][] temp = copyBoard(board);

        // perform AC on split domains, and if unsolved proceed recursively
        if (inOrder) {
            // left domain
            for (int i = 0; i < 10; i++)
                // update the split left domain of variable
                if (temp[index[0]][index[1]][i] != 0) {
                    leftDom--;
                    if (leftDom < 0) temp[index[0]][index[1]][i] = 0;
                }

            try {
                // check AC;
                if (searchArc(temp))
                    return temp;

                temp = splitDomain(temp, inOrder);
                // successfully split domain - return split board
                if (temp != null) return temp;
            } catch (IllegalStateException e) {
                // prune branch! can stop exploring this path
            }

            // right domain
            for (int i = 9; i >= 0; i--) {
                // update the split right domain of variable
                if (board[index[0]][index[1]][i] != 0) {
                    rightDom--;
                    if (rightDom < 0) board[index[0]][index[1]][i] = 0;
                }
            }
            try {
                // check AC;
                if (searchArc(board))
                    return board;

                board = splitDomain(board, inOrder);
                // successfully split domain - return split board
                if (board != null) return board;
            } catch (IllegalStateException e) {
                // prune branch! can stop exploring this path
            }
        }
        // Code is same as the above if condition, just changed order to split right,
        // then left recursively to see if it outputs a different solution
        else {
            // right domain
            for (int i = 9; i >= 0; i--) {
                if (board[index[0]][index[1]][i] != 0) {
                    rightDom--;
                    if (rightDom < 0) board[index[0]][index[1]][i] = 0;
                }
            }
            try {
                if (searchArc(board))
                    return board;
                board = splitDomain(board, inOrder);
                if (board != null) return board;
            } catch (IllegalStateException e) {}

            // left domain
            for (int i = 0; i < 10; i++)
                if (temp[index[0]][index[1]][i] != 0) {
                    leftDom--;
                    if (leftDom < 0) temp[index[0]][index[1]][i] = 0;
                }

            try {
                if (searchArc(temp))
                    return temp;
                temp = splitDomain(temp, inOrder);
                if (temp != null) return temp;
            } catch (IllegalStateException e) {}
        }
        return null;
    }

    // perform AC on split domain
    public boolean searchArc(int[][][] domainBoard) {
        int dirty = 1;
        while (dirty > 0) {
            dirty = 0;
            for (int i = 0; i < domainBoard.length; i++) {
                for (int j = 0; j < domainBoard[i].length; j++) {
                    dirty += checkQuadrant(domainBoard, i, j);
                    cleanDomain(domainBoard[i][j]);
                    dirty += checkCross(domainBoard, i, j);
                    cleanDomain(domainBoard[i][j]);
                    // if AC reduces domain size to 0, prune this path
                    if (getDomainSize(domainBoard[i][j]) == 0)
                        throw new IllegalStateException("Domain Empty!");
                }
            }
        }
        // AC complete, return state of board
        return checkSolved(domainBoard);
    }

    // find first unfilled cell
    public int[] findFirstEmpty(int[][][] board) {
        int[] index = new int[2];
        for (int i = 0; i < board.length; i++)
            for (int j = 0; j < board[i].length; j++)
                if (board[i][j][0] == 0) {
                    index[0] = i;
                    index[1] = j;
                    return index;
                }
        // didn't find an empty cell, perhaps a solution
        index[0] = -1;
        return index;
    }

    public int getDomainSize(int[] domain) {
        int count = 0;
        for (int m = 0; m < 10; m++)
            if (domain[m]!=0)
                count++;
        return count;
    }

    public int[][][] copyBoard(int[][][] board) {
        int[][][] domainBoard = new int[9][9][10];
        for (int i = 0; i < board.length; i++)
            for (int j = 0; j < board[i].length; j++)
                for (int k = 0; k < board[i][j].length; k++)
                    domainBoard[i][j][k] = board[i][j][k];
        return domainBoard;
    }

    // check to see if solution stays unique after switching order of recursion
    public void checkUniqueSolution(int[][][] board, int[][][] solution) throws Exception {
        board = splitDomain(board, false);
        for (int i = 0; i < board.length; i++)
            for (int j = 0; j < board[i].length; j++)
                for (int k = 0; k < board[i][j].length; k++)
                    if (board[i][j][k] != solution[i][j][k])
                        throw new Exception("No unique solution for board!");
    }

}
