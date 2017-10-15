import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.lang.System;
import java.io.*;
import java.util.Scanner;


/**
 * Created by loujian on 9/4/17.
 */
public class IMS {

    int N;
    ArrayList<LinkedList<Integer>> linked_value;
    int[] teammates;
    boolean[] flag;
    LinkedList<Integer> order;

    IMS(int N, ArrayList<LinkedList<Integer>> l)
    {
        this.N= N;
        teammates= new int[N];
        flag= new boolean[N];
        order= new LinkedList<Integer>();
        for(int i=0; i<N; i++) {
            teammates[i] = i;
            flag[i]=true;
            order.add(i);
        }

        linked_value= new ArrayList<LinkedList<Integer>>();
        linked_value.addAll(l);
    }

    void match_soulmates() {
        boolean[] flag_profile = new boolean[N];
        for (int i = 0; i < N; i++)  //Denote whether some player is still available
            flag_profile[i] = false;

        Integer[] prefer = new Integer[N];
        for (int i = 0; i < N; i++)
            prefer[i] = 0;
        boolean count = true;
        while (count == true && order.size() > 1) {
            for (int i = 0; i < N; i++) {
                if (!linked_value.get(i).isEmpty())
                    flag_profile[i] = true;
                else
                    flag_profile[i] = false;
            }

            for (int i = 0; i < N; i++) {
                if (flag_profile[i] == true)
                    prefer[i] = linked_value.get(i).getFirst();
            }

            count = false;

            for (Integer i = 0; i < N; i++) {
                for (Integer j = i + 1; j < N; j++) {
                    if (flag_profile[i] == true && flag_profile[j] == true && prefer[i] == j && prefer[j] == i) {

                        teammates[i] = j;
                        teammates[j] = i;
                        flag[i] = false; //i has been matched, so it is not available in future steps
                        flag[j] = false;
                        count = true;
                        for (int k = 0; k < N; k++) {
                            if (linked_value.get(k).indexOf(i) != -1)
                                linked_value.get(k).remove(i);
                            if (linked_value.get(k).indexOf(j) != -1)
                                linked_value.get(k).remove(j);
                        }
                        order.remove(i);
                        order.remove(j);

                        i = N + 1;
                        j = N + 1;//we could use this way to break the two loops
                    }
                }
            }
        }
    }

    int[] get_teammates()
    {
        return teammates;
    }

    boolean[] get_available()
    {
        return flag;
    }




}
