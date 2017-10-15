import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloCopyManager;
import ilog.concert.*;
import ilog.concert.IloNumVar;
import ilog.cplex.*;
import java.lang.Object;
import java.util.Iterator;

/**
 * Created by loujian on 8/29/17.
 */
public class promotion_IC {

    int N;
    int[][] utility;
    int max_payoff;

    promotion_IC(int N, int[][] utility)
    {
        this.N= N;
        max_payoff=N-1;
        this.utility = new int[N][N];
        for(int i=0; i<N; i++)
            for(int j=0; j<N; j++)
                this.utility[i][j]=utility[i][j];
    }

    public int solve_problem()
    {
        try{


            IloCplex cplex = new IloCplex();

            //pi is vinary variable
            IloIntVar[] pi= cplex.boolVarArray(N*N);


            //epsilon is a double variable
            IloNumVar epsilon = cplex.numVar(0, Double.MAX_VALUE);

            int[] objvals= new int[N*N];
            for(int i=0; i<N; i++)
                for(int j=0; j<N; j++)
                    objvals[i*N+j] = utility[i][j];

            //we would like to maximize the social welfare minus epsilon
            cplex.addMaximize( cplex.sum( cplex.scalProd(pi, objvals),  cplex.negative(epsilon)));


            //add constraint: \sum_{j\in N} pi_{ij} \leq 1,  \forall i\in N
            for(int i=0; i<N; i++)
            {
                int[] binary_vector = new int[N*N];
                for(int j=0; j<N; j++)
                    binary_vector[i*N+j]=1;
                cplex.addLe(cplex.scalProd(pi, binary_vector), 1);
            }

            //add constraint: \sum_{i\in N} pi_{ij} \leq 1, \forall j\in N
            for(int j=0; j<N; j++)
            {
                int[]binary_vector= new int[N*N];
                for(int i=0; i<N; i++)
                    binary_vector[i*N+j]=1;
                cplex.addLe(cplex.scalProd(pi, binary_vector), 1);
            }

            //add constraint: pi_{ij}-pi_{ji}=0, \forall i, j\in N
            for(int i=0; i<N; i++)
                for(int j=0; j<=i; j++)
                    cplex.addEq( cplex.sum(pi[i*N+j], cplex.negative(pi[j*N+i]) ), 0);

            cplex.exportModel("max_SW.lp");


            /*
            if(cplex.solve())
            {
                double SW_cur= cplex.getObjValue();
            }
            */
            //Then we check each potential promoted player
            /*

            for(int i=0; i<N; i++)
            {
                for(int k=0; k<N; k++)
                {
                    if(i==k || utility[i][k]==max_payoff)
                        continue;
                    if(isPromotion(cplex, i, k)) //here if
                    {
                        int[] local_obj= new int[N*N];
                        for(int j=0; j<N; j++)
                            local_obj[i*N+j] = utility[i][j];
                        cplex.addGe(cplex.scalProd(pi, local_obj), cplex.sum(objvals[i*N+k], cplex.negative(epsilon)));
                        cplex.exportModel("promotion.lp");
                    }
                }
            }
            */

            //cplex.exportModel("promotion.lp");



            if(cplex.solve())
            {
                System.out.println("Solution status = " + cplex.getStatus());
                System.out.println("Solution value = " + cplex.getObjValue());



                double[] pi_value= cplex.getValues(pi);
                double epsilon_value= cplex.getValue(epsilon);

                for(int i=0; i<N; i++) {
                    for (int j = 0; j < N; j++) {
                        System.out.println(pi_value[i * N + j]+ " ");
                    }
                    System.out.println("\r\n");

                }

                System.out.println("The epsilon is " + epsilon_value);
                System.out.println(cplex.getNcols());
            }


         //   isPromotion(0, 2, pi, epsilon);

            cplex.end();



        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }



        return 0;
    }

    boolean isPromotion(IloCplex cplex, int a, int b, IloIntVar[] pi, IloNumVar epsilon)
    {

        try {




            /*
            IloLPMatrix lp= (IloLPMatrix) cplex.LPMatrixIterator().next();
            IloCopyManager cm = new IloCopyManager(cplex);
            IloCplex cp = new IloCplex(); //create the new cplex object "cp"
            IloObjective obj = (IloObjective)cplex.getObjective().makeCopy(cm);
            cp.add(obj);
            int numRanges= lp.getRanges().length;

            for(int r=0; r<numRanges; r++)
            {
                IloRange temp= (IloRange)lp.getRange(r).makeCopy(cm);
                cp.add(temp);
            }
            */

            //IloNumVar[] x = lp.getNumVars();

            /*
            for(int j=0; j<N; j++)
            {
                if(j==b) {
                    x[a * N + j].setLB(1.0);
                    x[a * N + j].setUB(1.0);
                    x[j * N + a].setLB(1.0);
                    x[j * N + a].setUB(1.0);
                }
                else
                {
                    x[a * N + j].setLB(0.0);
                    x[a * N + j].setUB(0.0);
                    x[j * N + a].setLB(0.0);
                    x[j * N + a].setUB(0.0);
                }
            }
            */

            IloCplex cp= new IloCplex();

            IloCopyManager cm= new IloCopyManager(cplex);
            IloObjective obj = (IloObjective)cplex.getObjective().makeCopy(cm);
            cp.add(obj);


            /*
            class IloIntVar implements Cloneable{

                @Override
                public Object clone() throws CloneNotSupportedException{
                    return super.clone();
                }
            }
            ilog.concert.IloIntVar a = new IloIntVar() {
                @Override
                public Object clone() throws CloneNotSupportedException {
                    return super.clone();
                }
            }
            pi[1].clone();
            */



            if(cplex.solve())
            {
                System.out.println("Solution status = " + cplex.getStatus());
                System.out.println("Solution value = " + cplex.getObjValue());

                cplex.exportModel("max_SW1.lp");


                //IloLPMatrix lp= (IloLPMatrix)cplex.LPMatrixIterator().next();

                double[] x_value= cplex.getValues(pi);

                for(int i=0; i<x_value.length; i++)
                    System.out.println(x_value[i]+ " ");


                //cplex.exportModel("max_SW1.lp");
                /*
                for(int i=0; i<N; i++) {
                    for (int j = 0; j < N; j++) {
                        System.out.println(x_value[i * N + j]+ " ");
                    }
                    System.out.println("\r\n");

                }

                System.out.println( x_value[N*N] );
                */

            }



        }catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }
        return false;

    }


}
