/**
 * Created by loujian on 8/29/17.
 */

import ilog.concert.*;
import ilog.cplex.*;

public class LPex2 {

    public static void main(String[] args)
    {
        try
        {
            IloCplex cplex_orig = new IloCplex();
            cplex_orig.importModel("bip2.lp");
            IloLPMatrix lp = (IloLPMatrix)cplex_orig.LPMatrixIterator().next();
            IloCopyManager cm = new IloCopyManager(cplex_orig);
            IloCplex cplex = new IloCplex();
            IloObjective obj = (IloObjective)cplex_orig.getObjective().makeCopy(cm);
            cplex.add(obj);//Adding the objective function
            int numRanges = lp.getRanges().length;
            for(int r=0;r<numRanges;r++)
            {
                //System.out.println("Adding -> "+lp.getRange(r).toString());//uncomment to print constraints
                IloRange temp = (IloRange)lp.getRange(r).makeCopy(cm);
                cplex.add(temp);//Adding the individual constraints
            }
            if (cplex.solve())
            {
                System.out.println("Model Feasible");
                System.out.println("Solution status = " + cplex.getStatus());
                System.out.println("Solution value  = " + cplex.getObjValue());
                double[] x = cplex.getValues(lp);
                for (int j = 0; j < x.length; ++j)
                    System.out.println("Variable Name:" + lp.getNumVar(j).getName() + ";Value = " +
                            x[j]+";LB="+lp.getNumVar(j).getLB()+";UB="+lp.getNumVar(j).getUB()+";Type="+lp.getNumVar(j).getType().toString());
            }
            else
            {
                System.out.println("Solution status = " + cplex.getStatus());
            }
        }
        catch (IloException e)
        {
            System.out.println("Concert exception caught: " + e);
        }
    }

}
