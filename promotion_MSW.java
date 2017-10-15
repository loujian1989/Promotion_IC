import ilog.concert.*;
import ilog.cplex.*;

/**
 * Created by loujian on 9/3/17.
 * In this class, we denote check the promotion by looking at the max_SW program.
 *
 */
public class promotion_MSW {

    int N;
    double alpha;
    double[][] utility;
    int[] admire; //list the most preferred player that admire player i
    double max_payoff; //it means the maximum payoff a player could get, it can be changed
    boolean[] flag;  //means whether a player has been matched in IMS
    double obj_value=0; //here obj_value means the final obj value of the integer program
    double e=0; //here e means the final epsilon value

    promotion_MSW(int N, double[][] utility, boolean[] flag, double alpha)
    {
        this.N= N;
        this.alpha= alpha;
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

    public void solve_problem()
    {
        try{

            IloCplex cplex = new IloCplex();

            IloIntVar[][] var= new IloIntVar[1][];
            IloRange[][] rng= new IloRange[4][];
            IloNumVar epsilon= cplex.numVar(0, Double.MAX_VALUE);

            populateByRow(cplex, var, rng, epsilon);

            cplex.exportModel("promotion_MSW1.lp");

            if(cplex.solve())
            {
                obj_value= cplex.getObjValue();
                double[] x_value= cplex.getValues(var[0]);
                /*
                for(int i=0; i<N; i++) {
                    for (int j = 0; j < N; j++) {
                        System.out.println(x_value[i * N + j]+ " ");
                    }
                    System.out.println("\r\n");
                }
                */
                e= cplex.getValue(epsilon);
                //System.out.println("epsilon is "+ e);
            }

            cplex.end();

        }catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }
    }

    void populateByRow(IloMPModeler model, IloIntVar[][]var, IloRange[][]rng, IloNumVar epsilon)throws IloException
    {
        double[] objvals= new double[N*N];
        for(int i=0; i<N; i++)
            for(int j=0; j<N; j++) {
                if(flag[i]==true && flag[j]==true)
                    objvals[i * N + j] = utility[i][j]; //here is the objective function
            }
        IloIntVar[] x= model.boolVarArray(N*N);
        var[0]=x;

        rng[0]= new IloRange[N];
        rng[1]= new IloRange[N];
        rng[2]= new IloRange[N*N];
        rng[3]= new IloRange[N*N];

        //we would like to maximize the social welfare
        model.addMaximize(model.sum( model.prod(1-alpha, model.scalProd(x, objvals)), model.prod(N*alpha, model.negative(epsilon))));

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



        max_SW MSW= new max_SW(N, utility, flag);
        double current_SW= MSW.solve_problem(); //just the object value

        //Then we check each potential promoted player
        for(int i=0; i<N; i++)
        {
            if(!flag[i])
                continue;
            for(int k=0; k<N; k++)
            {
                if(!flag[k] || i==k)
                    continue;
                if(MSW.Promotion(i, k)>current_SW)
                {
                    //add constraint \sum_j pi_{ij} u_i(j) \geq u_i(k)- epsilon
                    double[] local_obj= new double[N*N];
                    for(int j=0; j<N; j++) {
                        if(flag[j])
                            local_obj[i * N + j] = utility[i][j];
                    }
                    rng[3][i*N+k]= model.addGe(model.sum(model.scalProd(x, local_obj), epsilon), objvals[i*N+k]);
                }
            }
        }



    }

    double get_sw()
    {
        return (obj_value+(N*e*alpha))/(1-alpha);
    }
    double get_epsilon()
    {
        return e;
    }

}
