-- Load matrices M and N from the txt files provided
M = LOAD 'M-matrix-small.txt' USING PigStorage(',') AS ( Mi, Mj, Mv );
N = LOAD 'N-matrix-small.txt' USING PigStorage(',') AS ( Ni, Nj, Nv );

-- Join the matrices by using column index of the first matrix and row index of the second matrix
JOINS = JOIN M BY Mj, N BY Ni;

/* Generate intermediate output that contains row index of matrix M, column index of matrix N
   and product of values in M and N */
INTERMEDIATE = FOREACH JOINS GENERATE M::Mi AS Mi, N::Nj AS Nj, (M::Mv * N::Nv) AS Mn;

-- Group the intermediate result by row index of M and column index of N
GROUPS = GROUP INTERMEDIATE BY (Mi, Nj);

/* For each grouped data, generate row index of M, column index of N and sum of multiplied values
   obtained in intermediate output at the respective indices and store it in final ordered by row 
   index of Mi and column index of N. */
FINAL = ORDER ( FOREACH GROUPS GENERATE group.Mi AS Mi, group.Nj AS Nj, SUM( INTERMEDIATE.Mn ) 
        AS Mn ) BY Mi, Nj;

-- store into folder output
STORE FINAL INTO 'output' USING PigStorage(',');

-- just to display final output on the terminal
dump FINAL