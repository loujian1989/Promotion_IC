import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * Created by loujian on 11/2/17.
 */
public class main_stable {

    public static void main(String[] args)throws Exception
    {
        Scanner cin=new Scanner(new File("test_in"));
        File writename = new File("test_out");
        writename.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(writename));

        int num_cases=1;
        int N= 3;
        double beta=0.1;


        double sw_sum=0;
        double gamma_sum=0;

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

            stable ST = new stable(N, utility, flag, beta); //epsilon difference is for checking convergency
            ST.solve_problem();
            double sw= ST.get_sw();
            for(int i=0; i< N; i++)
            {
                if(teammates[i]!=i)
                {
                    sw+= utility[i][teammates[i]];
                }
            }
            double gamma= ST.get_gamma();
            out.write("The alpha is " + beta + "\r\n");
            out.write("The social welfare is "+ sw/N + "\r\n");
            out.write("The gamma is "+ gamma + "\r\n");



            out.write("\r\n");
            sw_sum+=sw/N;
            gamma_sum+=gamma;
        }

        out.write("The average social welfare is "+ sw_sum/num_cases + "\r\n");
        out.write("The average gamma is "+ gamma_sum/num_cases + "\r\n");
        out.flush();
        out.close();

    }

}
