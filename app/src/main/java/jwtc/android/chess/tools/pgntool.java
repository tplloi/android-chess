package jwtc.android.chess.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

import androidx.appcompat.app.AppCompatActivity;
import jwtc.android.chess.HtmlActivity;
import jwtc.android.chess.MyPGNProvider;
import jwtc.android.chess.R;
import jwtc.chess.PGNColumns;
import jwtc.chess.algorithm.UCIWrapper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class pgntool extends AppCompatActivity {

    public static final String TAG = "pgntool";

    protected static final int MODE_IMPORT = 1;
    protected static final int MODE_DB_IMPORT = 2;
    protected static final int MODE_DB_POINT = 3;
    protected static final int MODE_UCI_INSTALL = 4;
    protected static final int MODE_UCI_DB_INSTALL = 5;
    protected static final int MODE_CREATE_PRACTICE = 6;
    protected static final int MODE_IMPORT_PRACTICE = 7;
    protected static final int MODE_IMPORT_PUZZLE = 8;
    protected static final int MODE_IMPORT_OPENINGDATABASE = 9;
    protected static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    protected static final String EXTRA_MODE = "jwtc.android.chess.tools.Mode";

    private ListView _lvStart;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pgntool);

        _lvStart = (ListView) findViewById(R.id.ListPgn);

        final CharSequence[] arrString;

        arrString = getResources().getTextArray(R.array.pgn_tool_menu);

        _lvStart.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                if (false == pgntool.this.hasPermission()) {
                    return;
                }

                if (arrString[arg2].equals(getString(R.string.pgntool_export_explanation))) {
                    doExport();
                } else if (arrString[arg2].equals(getString(R.string.pgntool_import_explanation))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_IMPORT);
                } else if (arrString[arg2].equals(getString(R.string.pgntool_create_db_explanation))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_DB_IMPORT);

                } else if (arrString[arg2].equals(getString(R.string.pgntool_point_db_explanation))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_DB_POINT);
                } else if (arrString[arg2].equals(getString(R.string.pgntool_install_uci_engine))) {

                    // install engine via open chess engine interface
                    // @see https://code.google.com/p/chessenginesupport-androidlib/
                    try {
                        ChessEngineResolver resolver = new ChessEngineResolver(getApplicationContext());
                        final List<ChessEngine> engines = resolver.resolveEngines();
                        if (engines.size() > 0) {

                            ArrayList<String> arrList = new ArrayList<String>();
                            AlertDialog.Builder builder = new AlertDialog.Builder(pgntool.this);

                            for (int i = 0; i < engines.size(); i++) {
                                arrList.add(engines.get(i).getName());
                            }
                            final String[] arrItems = (String[]) arrList.toArray(new String[arrList.size()]);
                            builder.setItems(arrItems, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int item) {
                                    dialog.dismiss();

                                    ChessEngine selectedEngine = engines.get(item);
                                    String sEngine = "files/" + selectedEngine.getFileName();
                                    Log.i("engines", sEngine);

                                    try {
                                        selectedEngine.copyToFiles(pgntool.this.getContentResolver(), pgntool.this.getFilesDir());
                                        UCIWrapper.runConsole("/system/bin/chmod 744 /data/data/jwtc.android.chess/*");

                                        SharedPreferences.Editor editor = getSharedPreferences("ChessPlayer", MODE_PRIVATE).edit();
                                        editor.putString("UCIEngine", sEngine);
                                        editor.commit();

                                        doToast(String.format(getString(R.string.pgntool_uci_engine_success), sEngine));

                                    } catch (Exception ex) {
                                        doToast(getString(R.string.pgntool_uci_engine_error));
                                    }
                                }
                            });
                            AlertDialog alert = builder.create();
                            alert.show();

                        } else {

                            Log.i("engines", "No engines found");
                            AlertDialog.Builder builder = new AlertDialog.Builder(pgntool.this);
                            builder.setTitle(getString(R.string.pgntool_uci_engines_not_found));

                            builder.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    } catch (Exception ex) {
                        Log.e("engines", ex.toString());
                    }


                } else if (arrString[arg2].equals(getString(R.string.pgntool_delete_explanation))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(pgntool.this);
                    builder.setTitle(getString(R.string.pgntool_confirm_delete));

                    builder.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            pgntool.this.getContentResolver().delete(MyPGNProvider.CONTENT_URI, "1=1", null);
                            doToast(getString(R.string.pgntool_deleted));
                        }
                    });
                    builder.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();

                } else if (arrString[arg2].equals(getString(R.string.pgntool_help))) {
                    Intent i = new Intent();
                    i.setClass(pgntool.this, HtmlActivity.class);
                    i.putExtra(HtmlActivity.HELP_MODE, "help_pgntool");
                    startActivity(i);
                } else if (arrString[arg2].equals(getString(R.string.pgntool_point_uci_engine))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_UCI_INSTALL);

                } else if (arrString[arg2].equals(getString(R.string.pgntool_unset_uci_engine))) {
                    SharedPreferences prefs = getSharedPreferences("ChessPlayer", MODE_PRIVATE);
                    String sEngine = prefs.getString("UCIEngine", null);
                    if (sEngine != null) {
                        File f = new File("/data/data/jwtc.android.chess/" + sEngine);
                        if (f.delete()) {
                            Log.i("engines", sEngine + " deleted");
                        } else {
                            Log.w("engines", sEngine + " NOT deleted");
                        }
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("UCIEngine", null);
                        editor.commit();
                        doToast("Engine " + sEngine + " uninstalled");
                    } else {
                        doToast("No engine installed");
                    }
                } else if (arrString[arg2].equals(getString(R.string.pgntool_install_uci_database))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_UCI_DB_INSTALL);

                } else if (arrString[arg2].equals(getString(R.string.pgntool_unset_uci_database))) {
                    SharedPreferences prefs = getSharedPreferences("ChessPlayer", MODE_PRIVATE);
                    String sDatabase = prefs.getString("UCIDatabase", null);
                    if (sDatabase != null) {
                        File f = new File("/data/data/jwtc.android.chess/" + sDatabase);
                        if (f.delete()) {
                            Log.i("database", sDatabase + " deleted");
                        } else {
                            Log.w("database", sDatabase + " NOT deleted");
                        }
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("UCIDatabase", null);
                        editor.commit();
                        doToast("Database " + sDatabase + " uninstalled");
                    } else {
                        doToast("No database installed");
                    }
                } else if (arrString[arg2].equals(getString(R.string.pgntool_reset_practice))) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(pgntool.this);
                    builder.setTitle(getString(R.string.pgntool_confirm_practice_reset));

                    builder.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            SharedPreferences prefs = getSharedPreferences("ChessPlayer", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("practicePos", 0);
                            editor.putInt("practiceTicks", 0);
                            editor.commit();

                            doToast(getString(R.string.practice_set_reset));

                        }
                    });
                    builder.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();


                } else if (arrString[arg2].equals(getString(R.string.pgntool_import_practice))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_IMPORT_PRACTICE);

                } else if (arrString[arg2].equals(getString(R.string.pgntool_create_practice_explanation))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_IMPORT_PRACTICE);
                } else if (arrString[arg2].equals(getString(R.string.pgntool_import_puzzle))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_IMPORT_PUZZLE);

                } else if (arrString[arg2].equals(getString(R.string.pgntool_import_opening))) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, MODE_IMPORT_OPENINGDATABASE);
                }
                //
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                // API 5+ solution
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.d(TAG, "result" + requestCode + "  " + resultCode);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                if (uri != null) {
                    Intent myIntent = new Intent();
                    myIntent.putExtra(pgntool.EXTRA_MODE, requestCode);
                    myIntent.setClass(pgntool.this, importactivity.class);
                    myIntent.setData(uri);

                    startActivity(myIntent);
                    finish();
                }
            }
            Log.d(TAG, "res url" + uri);
        }
    }

    public boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                return false;
            }
        } else {
            return true;
        }
    }

    public void doExport() {
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                String sFile = Environment.getExternalStorageDirectory() + "/chess.pgn";
                int i = 0;
                Cursor cursor = managedQuery(MyPGNProvider.CONTENT_URI, PGNColumns.COLUMNS, null, null, PGNColumns.DEFAULT_SORT_ORDER);
                if (cursor != null) {

                    if (cursor.getCount() > 0) {

                        cursor.moveToFirst();
                        String s = "";

                        while (cursor.isAfterLast() == false) {

                            s += cursor.getString(cursor.getColumnIndex(PGNColumns.PGN)) + "\n\n\n";
                            cursor.moveToNext();
                            i++;
                        }

                        FileOutputStream fos;
                        fos = new FileOutputStream(sFile);
                        fos.write(s.getBytes());
                        fos.flush();
                        fos.close();
                    }
                }
                doToast(String.format(getString(R.string.pgntool_numexport), i));

            } else {
                doToast(getString(R.string.err_sd_not_mounted));
            }
        } catch (Exception e) {

            //doToast(getString(R.string.err_send_email));
            Log.e("ex", e.toString());
            return;
        }
    }

    public void doToast(String s) {
        Toast t = Toast.makeText(this, s, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }
}