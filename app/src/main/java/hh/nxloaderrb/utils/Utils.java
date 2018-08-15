package hh.nxloaderrb.utils;

import android.content.Context;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by laole918 on 2016/4/23.
 */
public class Utils {

    public static void startShareActivity(Context context, String title, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, title));
    }

    public static void startShareActivity(Context context, int titleId, String text) {
        startShareActivity(context, context.getString(titleId), text);
    }

    public static String exec(String cmd) {
        StringBuilder sb = new StringBuilder();
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            if (process.waitFor() != 0) {
                System.err.println("exit value = " + process.exitValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
