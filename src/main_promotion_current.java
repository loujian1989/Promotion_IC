import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * Created by loujian on 9/3/17.
 */
public class main_promotion_current {

    public static void main(String[] args)throws Exception
    {
        Scanner cin=new Scanner(new File("SN_Scale_Free_n10m2.txt"));
        File writename = new File("PICC_alpha_015_SN_Scale_Free_n10m2.txt");
        writename.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(writename));

        int num_cases=100;
        int N= 10;
        double alpha=0.15;
        double epsilon_difference=0.0; //as we expect the program to converge, we need to terminate it at some time
                                        //i.e. when the difference of epsilon is less than 0.01

        double sw_sum=0;
        double epsilon_sum=0;

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
                    Integer tmp= cin.nextInt();
                    tmp--;
                    out.write(tmp+" ");
                    utility[i][tmp]= (double) (number_nei-j)/number_nei; //we set utility to be normalized utility
                    linked_value.get(i).add(tmp);
                }
                out.write("\r\n");
            }

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

            promotion_current PICC = new promotion_current(N, utility, flag, alpha, epsilon_difference); //epsilon difference is for checking convergency
            PICC.solve_problem();
            double sw= PICC.get_sw();
            for(int i=0; i< N; i++)
            {
                if(teammates[i]!=i)
                {
                    sw+= utility[i][teammates[i]];
                }
            }
            double e= PICC.get_epsilon();
            out.write("The alpha is " + alpha + "\r\n");
            out.write("The social welfare is "+ sw/N + "\r\n");
            out.write("The epsilon is "+ e + "\r\n");



            out.write("\r\n");
            sw_sum+=sw/N;
            epsilon_sum+=e;
        }

        out.write("The average social welfare is "+ sw_sum/num_cases + "\r\n");
        out.write("The average epsilon is "+ epsilon_sum/num_cases + "\r\n");
        out.flush();
        out.close();

    }



}
