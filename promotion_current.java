import ilog.concert.*;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplexModeler;

/**
 * Created by loujian on 9/3/17.
 *
 * in this algorithm, we use the "current" model to check whether a play could promote another player, which is different from promotion_MSW
 *
 */
public class promotion_current {

    int N;
    double alpha;
    double[][] utility;
    int[] admire; //list the most preferred player that admire player i
    double max_payoff; //it means the maximum payoff a player could get, it can be changed
    boolean[] flag;
    double obj_value=0; //here obj_value means the final obj value of the integer program
    double e=0; //here e means the final epsilon value

    promotion_current(int N, double[][] utility, boolean[] flag, double alpha)
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


    void solve_problem()
    {
        try{

            IloCplex cplex = new IloCplex();

            IloIntVar[][] var= new IloIntVar[1][];
            IloRange[][] rng= new IloRange[4][];
            IloNumVar epsilon= cplex.numVar(0, Double.MAX_VALUE);

            populateByRow(cplex, var, rng, epsilon);

            cplex.exportModel("promotion_current1.lp");

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
                System.out.println("epsilon is "+ e);
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
        model.addMaximize(model.sum( model.prod(1-alpha, model.scalProd(x, objvals)), model.prod(alpha, model.negative(epsilon))));

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

        //Then we check each potential promoted player

        for(int i=0; i<N; i++)
        {
            if(flag[i]==false)
                continue;
            for(int k=0; k<N; k++)
            {
                if(i==k || flag[k]==false)
                    continue;
                if(promotion((IloCplex)model, i, k, rng, var, epsilon))
                {
                    //add constraint \sum_j pi_{ij} u_i(j) \geq u_i(k)- epsilon
                    double[] local_obj= new double[N*N];
                    for(int j=0; j<N; j++) {
                        if(flag[j]==true)
                            local_obj[i * N + j] = utility[i][j];
                    }
                    rng[3][i*N+k]= model.addGe(model.sum(model.scalProd(x, local_obj), epsilon), objvals[i*N+k]);
                }
            }
        }

    }

    boolean promotion(IloCplex cplex, int a,  int b, IloRange[][] rng, IloIntVar[][] var, IloNumVar epsilon)
    {
        double current_obj=0;
        try
        {
            if(cplex.solve())
            {
                current_obj=cplex.getObjValue();
            }
        }catch (IloException e){
            System.err.println("Concert exception caught: " + e);
        }

        double result= (max_payoff+ utility[b][a]);

        try {

            IloCplex cp= new IloCplex();

            IloCopyManager cm= new IloCopyManager(cplex);
            IloRange[][] rng_new= new IloRange[4][];
            rng_new[0]= new IloRange[N];
            rng_new[1]= new IloRange[N];
            rng_new[2]= new IloRange[N*N];
            rng_new[3]= new IloRange[N*N];

            for(int i=0; i<rng[0].length; i++) {
                if(flag[i])
                    rng_new[0][i] = (IloRange) cp.add((IloAddable) rng[0][i].makeCopy(cm));
            }
            for(int i=0; i<rng[1].length; i++)
                if(flag[i])
                    rng_new[1][i] = (IloRange)cp.add((IloAddable) rng[1][i].makeCopy(cm));
            for(int i=0; i<rng[2].length; i++) {
                if(rng[2][i]!=null)
                    rng_new[2][i] = (IloRange) cp.add((IloAddable) rng[2][i].makeCopy(cm));
            }
            for(int i=0; i<rng[3].length; i++) {
                if(rng[3][i]!=null)
                    rng_new[3][i] = (IloRange) cp.add((IloAddable) rng[3][i].makeCopy(cm));
            }

            IloObjective obj = (IloObjective)cplex.getObjective().makeCopy(cm);
            double[][] utility_new= new double[N][N];
            for(int i=0; i<N; i++)
            {
                for(int j=0; j<N; j++)
                {

                    if(i==a || i==b || j==a || j==b)
                        utility_new[i][j]=0;
                    else
                        utility_new[i][j]=utility[i][j];
                }
            }
            double[] objvals_new= new double[N*N];
            for(int i=0; i<N; i++)
                for(int j=0; j<N; j++) {
                    if(flag[i] && flag[j])
                        objvals_new[i * N + j] = utility_new[i][j]; //here is the objective function
                }

            //cp.addMaximize(cp.scalProd(var[0], objvals));
            cp.add(obj);

            cp.addEq(var[0][a*N+b], 1);
            cp.addEq(var[0][b*N+a], 1);

            /*
            cp.setLinearCoefs(obj, objvals_new, var[0]);
            rng_new[0][a].setUB(0);
            rng_new[0][b].setUB(0);
            rng_new[1][a].setUB(0);
            rng_new[1][b].setUB(0);
            */

            cp.exportModel("promotion_current2.lp");
            if(cp.solve())
            {
                //System.out.println("Solution status = " + cp.getStatus());
                //System.out.println("Solution value = " + cp.getObjValue());

                result+= cp.getObjValue();

                double[] x_value= cp.getValues(var[0]);

                /*
                for(int i=0; i<x_value.length; i++)
                    System.out.println(x_value[i]+ " ");
                */
            }

            cp.end();

        }catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }

        return (result > current_obj);
    }

    double get_sw()
    {
        return (obj_value+(e*alpha))/(1-alpha);
    }
    double get_epsilon()
    {
        return e;
    }


}
