import ilog.concert.*;
import ilog.cplex.IloCplex;

/**
 * Created by loujian on 11/2/17.
 * It considers the \gamma-stability MIP int the paper
 */
public class stable {

    int N;
    double beta;
    double[][] utility;
    int[] admire; //list the most preferred player that admire player i
    double max_payoff; //it means the maximum payoff a player could get, it can be changed
    boolean[] flag;
    double obj_value=0; //here obj_value means the final obj value of the integer program
    double ga=0; //here ga means the final gamma value



    stable(int N, double[][] utility, boolean[] flag, double beta)
    {
        this.N= N;
        this.beta= beta;
        this.max_payoff= 1; //it means the maximum payoff a player could get, it can be changed
        this.utility = new double[N][N];
        for(int i=0; i<N; i++)
            for(int j=0; j<N; j++)
                this.utility[i][j]=utility[i][j];
        this.admire= new int[N];
        for(int i=0; i<N; i++)
            this.admire[i]=-1; //default set no one admires the player
        for(int i=0; i<N; i++) {
            if(flag[i]==false)
                continue;
            double cur_max= -1;
            for (int j = 0; j < N; j++) {
                if(flag[j]==false)
                    continue;
                if(utility[j][i]==max_payoff && utility[j][i]> cur_max)
                {
                    cur_max= utility[j][i];
                    admire[i]=j; //it is most preferred player that admires i;
                }
            }
        }
        this.flag= new boolean[N];
        for(int i=0; i<N; i++)
            this.flag[i]=flag[i];
    }


    void solve_problem()
    {
        try{

            double[] objvals= new double[N*N];
            for(int i=0; i<N; i++)
                for(int j=0; j<N; j++) {
                    if(flag[i]==true && flag[j]==true)
                        objvals[i * N + j] = utility[i][j]; //here is the objective function
                }

            IloCplex cplex = new IloCplex();

            IloNumVar[][] var= new IloNumVar[1][];
            IloRange[][] rng= new IloRange[4][];
            IloNumVar gamma= cplex.numVar(0, Double.MAX_VALUE);

            populateByRow(cplex, var, rng, gamma);

            if (cplex.solve()) {
                double[] x= cplex.getValues(var[0]);
                //then we start adding stability constraints
                for (int i = 0; i < N - 1; i++)
                    for (int j = i + 1; j < N; j++) {

                        if(flag[i]==false || flag[j]==false) //if i or j has been matched in IMS procedure, then we don't need to consider them
                            continue;

                        double u_i=0; //utility of player i in the program
                        double u_j=0; //utility of player j in the program
                        for (int k = 0; k < N; k++)
                        {
                            if(x[i*N+k]==1 || x[k*N+i]==1) { //then we know the mate of i is k
                                u_i = utility[i][k];
                                break;

                            }
                        }

                        for(int k=0; k<N; k++)
                        {
                            if(x[j*N+k]==1 || x[k*N+j]==1) { //then we know the mate of j is k
                                u_j= utility[j][k];
                                break;
                            }
                        }

                        if(utility[i][j]> u_i && utility[j][i]> u_j) //then we add the constraint and solve the program
                        {

                            double[] local_obj1 = new double[N * N];
                            for (int k = 0; k < N; k++) {
                                if (flag[k] == true)
                                    local_obj1[i * N + k] = utility[i][k];
                            }
                            rng[3][i * N + j] = cplex.addGe(cplex.sum(cplex.scalProd(var[0], local_obj1), gamma), objvals[i * N + j]);

                            double[] local_obj2 = new double[N * N];
                            for (int k = 0; k < N; k++) {
                                if (flag[k] == true)
                                    local_obj2[j * N + k] = utility[j][k];
                            }
                            rng[3][j * N + i] = cplex.addGe(cplex.sum(cplex.scalProd(var[0], local_obj2), gamma), objvals[j * N + i]);

                            if (cplex.solve())
                                x = cplex.getValues(var[0]);

                        }

                    }

                if(cplex.solve())
                {
                    obj_value= cplex.getObjValue();
                    ga= cplex.getValue(gamma);
                }

            }

            cplex.exportModel("stable.lp");

        }catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }

    }


    void populateByRow(IloMPModeler model, IloNumVar[][]var, IloRange[][]rng, IloNumVar gamma)throws IloException
    {
        double[] objvals= new double[N*N];
        for(int i=0; i<N; i++)
            for(int j=0; j<N; j++) {
                if(flag[i]==true && flag[j]==true)
                    objvals[i * N + j] = utility[i][j]; //here is the objective function
            }

        IloNumVarType[] type=  new IloNumVarType[N*N];
        for(int i=0; i<N*N; i++)
            type[i]= IloNumVarType.Bool;
        double[] x_lower= new double[N*N];
        double[] x_upper= new double[N*N];
        for(int i=0; i<N*N; i++)
            x_upper[i]=1;

        IloNumVar[] x= model.numVarArray(N*N, x_lower, x_upper, type); //here we make "x" numVar, but we set the type be "IloNumVarType.Bool"
        var[0]=x;

        rng[0]= new IloRange[N];
        rng[1]= new IloRange[N];
        rng[2]= new IloRange[N*N];
        rng[3]= new IloRange[N*N];

        //we would like to maximize the social welfare
        model.addMaximize(model.sum( model.prod(1-beta, model.scalProd(x, objvals)), model.prod(N*beta, model.negative(gamma))));

        //add constraint: \sum_{j\in N} pi_{ij} \leq 1,  \forall i\in N
        for(int i=0; i<N; i++)
        {
            if(flag[i]==false)
                continue;
            int[] binary_vector = new int[N*N];
            for(int j=0; j<N; j++) {
                if(flag[j]==true)
                    binary_vector[i * N + j] = 1;
            }
            rng[0][i]= model.addLe(model.scalProd(x, binary_vector), 1);
        }

        //add constraint: \sum_{i\in N} pi_{ij} \leq 1, \forall j\in N
        for(int j=0; j<N; j++)
        {
            if(flag[j]==false)
                continue;
            int[]binary_vector= new int[N*N];
            for(int i=0; i<N; i++) {
                if(flag[i]==true)
                    binary_vector[i * N + j] = 1;
            }
            rng[1][j]= model.addLe(model.scalProd(x, binary_vector), 1);
        }

        //add constraint: pi_{ij}-pi_{ji}=0, \forall i, j\in N
        for(int i=0; i<N; i++)
            for(int j=0; j<=i; j++) {
                if(flag[i]==true && flag[j]==true)
                    model.addEq(model.sum(x[i * N + j], model.negative(x[j * N + i])), 0);
            }

    }

    double get_sw()
    {
        return (obj_value+(ga*beta))/(1-beta);
    }
    double get_gamma()
    {
        return ga;
    }

}
