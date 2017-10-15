
/**
 * Created by loujian on 9/3/17.
 * Use the maximal social welfare program to check whether a player has chance to be promoted
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;


public class main_promotion_MSW_n50m3_alpha020_81_100
{

    public static void main(String[] args)throws Exception
    {
        Scanner cin=new Scanner(new File("SN_Scale_Free_n50m3_81-100.txt"));
        File writename = new File("PICSW_alpha_020_SN_Scale_Free_n50m3_81-100.txt");
        writename.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(writename));

        int num_cases=20;
        int N= 50;
        double alpha=0.20;

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

            promotion_MSW PICSW = new promotion_MSW(N, utility, flag, alpha);
            PICSW.solve_problem();
            double sw= PICSW.get_sw();
            for(int i=0; i< N; i++)
            {
                if(teammates[i]!=i)
                {
                    sw+= utility[i][teammates[i]];
                }
            }
            double e= PICSW.get_epsilon();
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
