package com.cipa.taskcli;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private TextView output;
    private EditText input;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private File scriptPath;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        output = new TextView(this);
        output.setTypeface(android.graphics.Typeface.MONOSPACE);
        output.setTextSize(12);
        output.setText("TaskCLI ready.\nKetik perintah:\n  help\n  add beli kopi\n  list\n  stats\n\n");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scroll, sp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 0);

        input = new EditText(this);
        input.setHint("perintah...");
        input.setSingleLine(true);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(input, ip);

        Button run = new Button(this);
        run.setText("Run");
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cmd = input.getText().toString().trim();
                input.setText("");
                if (!cmd.isEmpty()) exec(cmd);
            }
        });
        row.addView(run);

        root.addView(row);
        setContentView(root);

        scriptPath = new File(getFilesDir(), "taskcli.sh");
        try (InputStream in = getAssets().open("taskcli.sh");
             OutputStream out = new FileOutputStream(scriptPath)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            append("script copy error: " + e.getMessage() + "\n");
        }
        scriptPath.setExecutable(true, false);
    }

    private void exec(String args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] tokens = args.split("\\s+", 2);
                    String[] full;
                    if (tokens.length == 2) {
                        full = new String[]{ "/system/bin/sh", scriptPath.getAbsolutePath(),
                                tokens[0], tokens[1] };
                    } else {
                        full = new String[]{ "/system/bin/sh", scriptPath.getAbsolutePath(),
                                tokens[0] };
                    }

                    ProcessBuilder pb = new ProcessBuilder(full);
                    pb.redirectErrorStream(true);
                    pb.environment().put("HOME", getFilesDir().getAbsolutePath());
                    Process p = pb.start();

                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append('\n');
                    int code = p.waitFor();

                    final String out = sb.toString()
                            + (code == 0 ? "" : "[exit " + code + "]\n");
                    ui.post(new Runnable() {
                        @Override public void run() { append(out); }
                    });
                } catch (final Exception e) {
                    ui.post(new Runnable() {
                        @Override public void run() {
                            append("error: " + e.getMessage() + "\n");
                        }
                    });
                }
            }
        }).start();
    }

    private void append(String s) {
        output.append(s);
    }
}
