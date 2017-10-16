
/**
 * Created by loujian on 9/3/17.
 * Use the maximal social welfare program to check whether a player has chance to be promoted
 */

import sun.awt.image.ImageWatched;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;


public class main_promotion_MSW {

    public static void main(String[] args)throws Exception
    {
        Scanner cin=new Scanner(new File("SN_Scale_Free_n10m2.txt"));
        File writename = new File("PICSW_alpha_010_SN_Scale_Free_n10m2.txt");
        writename.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(writename));

        int num_cases=100;
        int N= 10;
        double alpha=0.10;

        double sw_sum=0;
        double epsilon_sum=0;
        double epsilon_sum_real= 0;

        for(int iter=0; iter<num_cases; iter++)
        {
            int[] teammates= new int[N];
            for(int i=0; i<N; i++)
                teammates[i]=i; //at first, players are singletons, so their teammates are themselves.

            ArrayList<LinkedList<Integer>> linked_value = new ArrayList<>();
            double[][] utility = new double[N][N]; //utility[i][j]
            boolean[] flag= new boolean[N]; //to mark whether a player is still available after the IMS preprocessing.
            for(int i=0; i<N; i++)
                flag[i]=true;

            for(int i=0; i<N; i++)
            {
                LinkedList<Integer> tmp= new LinkedList<>();
                linked_value.add(tmp);
            }

            for(int i=0; i<N; i++) //we initialize utility and linked_value
            {
                Integer number_nei= cin.nextInt();
                out.write(number_nei+ " ");
                for(int j=0; j<number_nei; j++)
                {
                    Integer tmp= cin.nextInt(); //here the index of player is from 1 to N
                    tmp--; //our system is from 0 to N-1, So we need to minus 1;
                    out.write(tmp+" ");
                    utility[i][tmp]= (double) (number_nei-j)/number_nei; //we set utility to be normalized utility
                    linked_value.get(i).add(tmp);
                }
                out.write("\r\n");
            }

            //we firstly call IMS preprocessing steps
            ArrayList<LinkedList<Integer>> linked_value_copy= new ArrayList<>(linked_value);
            IMS ims= new IMS(N, linked_value);
            ims.match_soulmates();
            teammates= ims.get_teammates();
            flag=ims.get_available();



            //input the data from the file
            /*
            for (int i = 0; i < N; i++)
                for (int j = 0; j < N; j++)
                    utility[i][j] = cin.nextInt();
            */

            promotion_MSW PICSW = new promotion_MSW(N, utility, flag, alpha);
            PICSW.solve_problem();
            double sw= PICSW.get_sw(); //the SW got in the system
            for(int i=0; i< N; i++)
            {
                if(teammates[i]!=i)
                {
                    sw+= utility[i][teammates[i]]; //we need to add the outcome got from IMS procedure
                }
            }
            double e= PICSW.get_epsilon();
            double[] x_value= PICSW.getX_value();

            out.write("The alpha is " + alpha + "\r\n");
            out.write("The social welfare is "+ sw/N + "\r\n");
            out.write("The epsilon from the PIC program is "+ e + "\r\n");


            for(int i=0; i<N; i++)
                for(int j=0; j<N; j++)
                {
                    if(x_value[i*N+j]==1)
                        teammates[i]= j;
                }


            out.write("The teammate of each player is: \r\n");
            for(int i=0; i< N; i++)
                out.write(i + " "+ teammates[i] + "\r\n");
            out.write("The utility of each player is: \r\n");
            for(int i=0; i< N; i++)
                out.write(i + " : " + utility[i][teammates[i]] + "\r\n");

            //We could know that the social welfare we got here is the true value we want to get.
            //But the epsilon value is not reliable. We will look at what is the "real" value of epsilon here.
            //The mechanism can be terminated here, but the the following evaluation part is necessary.

            double real_epsilon= 0;
            for(int i=0; i<N; i++)
            //for each player, we need to check whether it is possible for her to promote some player
            {
                if(linked_value_copy.get(i).size()==0)
                    continue;
                for(int j=0; j<N; j++)
                {
                    // in some cases we don't need to consider promoting them
                    if( i==j || !linked_value_copy.get(i).contains(j) || linked_value_copy.get(i).getFirst()==j ||utility[i][j]<= utility[i][teammates[i]]) //if j is not better than current teammate, then we ignore it
                        continue;

                    //here we will simulate the mechanism, and see whether the promotion is beneficial
                    //we need to remain the utility of player i and go back later
                    double[] utility_copy_i = new double[N];
                    for(int k=0; k< N; k++)
                        utility_copy_i[k]= utility[i][k];

                    //here we start simulate the mechanism
                    ArrayList<LinkedList<Integer>> current_linked_value= new ArrayList<>(linked_value_copy);
                    boolean[] flag_current= new boolean[N];
                    for(int k=0; k<N; k++)
                        flag_current[k]=true;

                    //we have to revise the current_linked_value
                    current_linked_value.get(i).remove((Integer)j);
                    current_linked_value.get(i).add(0, j);

                    //change the content of utility
                    int neigh= current_linked_value.get(i).size();
                    for(int k: current_linked_value.get(i))
                    {
                        //utility[i][tmp]= (double) (number_nei-j)/number_nei;
                        if(k!=j)
                            utility[i][k]= utility[i][k]-(1.0/neigh);
                        if(k==j) {
                            utility[i][k] = 1.0; //we set it the max payoff
                            break;
                        }
                    }

                    //IMS preprocessing
                    IMS ims_current= new IMS(N, current_linked_value);
                    ims_current.match_soulmates();
                    int[] teammates_current= ims_current.get_teammates();
                    flag_current=ims_current.get_available();



                    //create an object to do the PIC program
                    promotion_MSW PICSW_current = new promotion_MSW(N, utility, flag_current, alpha);
                    PICSW_current.solve_problem();
                    double[] x_value_current= PICSW_current.getX_value();
                    for(int k=0; k<N; k++)
                    {
                        if(x_value_current[i*N+k]==1)
                            teammates_current[i]=k;
                    }
                    real_epsilon= Math.max(utility[i][teammates_current[i]]-  utility[i][teammates[i]], real_epsilon);

                    for(int k=0; k<N; k++)
                        utility[i][k]=utility_copy_i[k]; //go back to the original utility
                }
            }

            out.write("The real epsilon is "+ real_epsilon + "\r\n");
            out.write("\r\n");

            sw_sum+=sw/N;
            epsilon_sum+=e;
            epsilon_sum_real+= real_epsilon;
        }

        out.write("The average social welfare is "+ sw_sum/num_cases + "\r\n");
        out.write("The average epsilon is "+ epsilon_sum/num_cases + "\r\n");
        out.write("The average real epsilon is " + epsilon_sum_real/num_cases+ "\r\n");
        out.flush();
        out.close();

    }

}
