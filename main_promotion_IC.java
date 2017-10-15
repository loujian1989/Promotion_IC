import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

/**
 * Created by loujian on 8/29/17.
 */
public class main_promotion_IC {


    public static void main(String[] args)throws Exception
    {
        Scanner cin=new Scanner(new File("test_in"));
        File writename = new File("test_out");
        writename.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(writename));

        int N= cin.nextInt(); //the number of players
        int[][] utility = new int[N][N]; //utility[i][j]

        //input the data from the file
        for(int i=0; i<N; i++)
            for(int j=0; j<N; j++)
                utility[i][j]=cin.nextInt();

        promotion_IC PIC= new promotion_IC(N, utility);
        PIC.solve_problem();

        out.flush();
        out.close();

    }

}
