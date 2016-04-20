package jp.org.example.geckour.paincalc;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.jpn.geckour.calculator.app.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import icepick.Icepick;
import icepick.Icicle;

public class MainActivity extends ActionBarActivity {
    TextView resultView;
    TextView undoView;
    TextView redoView;
    @Icicle String resultViewText;
    @Icicle String undoViewText;
    @Icicle String redoViewText;
    @Icicle ArrayList<ValCom> valCom = new ArrayList<>();
    @Icicle ArrayList<ValCom> histValCom = new ArrayList<>();
    HashMap<String, String> comName = new HashMap<>();
    @Icicle int histP = 0;
    Double mValue = 0.0;
    FrameLayout container;
    View leftView;
    View rightView;
    boolean isButtonsLeft = false;
    final Activity main = this;
    SharedPreferences sp;
    BigDecimal output = new BigDecimal(0.0);
    @Icicle boolean isEndCommand = false;
    String commandsRegex = "plus|minus|multi|div|power|mod|root|log";
    String commandsRegexMini = "plus|minus|multi|div|power|mod";
    int showImageMode = 0;
    int orientation;
    int imageWidth = 0;
    int imageHeight = 0;
    int ivWidth = 0;
    int ivHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);

        setContentView(R.layout.activity_main);
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        Tracker t = ((Analytics) getApplication()).getTracker(Analytics.TrackerName.APP_TRACKER);
        t.setScreenName("MainActivity");
        t.send(new HitBuilders.AppViewBuilder().build());

        comName.put("pi", "π");
        comName.put("plus", " + ");
        comName.put("minus", " - ");
        comName.put("multi", " × ");
        comName.put("div", " ÷ ");
        comName.put("power", "^");
        comName.put("mod", " % ");
        comName.put("root", "√");
        comName.put("loge", "log_");
        comName.put("log10", "log10_");
        comName.put("log2", "log2_");
        comName.put("left", "(");
        comName.put("right", ")");
        comName.put("plusminus", "-");

        showImageMode = Integer.parseInt(sp.getString("bg_image_scale_mode", "0"));

        //読み込まれたlayoutがxlargeか否か
        if (this.getResources().getBoolean(R.bool.isXlarge)) {
            container = (FrameLayout) findViewById(R.id.container);
            leftView = getLayoutInflater().inflate(R.layout.activity_left, null);
            rightView = getLayoutInflater().inflate(R.layout.activity_right, null);
            container.addView(rightView);
            resultView = (TextView) rightView.findViewById(R.id.textView1);
            undoView = (TextView) rightView.findViewById(R.id.textView0);
            redoView = (TextView) rightView.findViewById(R.id.textView2);
            rightView.findViewById(R.id.mr).setOnTouchListener(new FlickListener());
            rightView.findViewById(R.id.ac).setOnTouchListener(new FlickListener());
            rightView.findViewById(R.id.div).setOnTouchListener(new FlickListener());
            rightView.findViewById(R.id.dot).setOnTouchListener(new FlickListener());
            rightView.findViewById(R.id.zero2).setOnTouchListener(new FlickListener());
            rightView.findViewById(R.id.zero).setOnTouchListener(new FlickListener());
            rightView.findViewById(R.id.five).setOnTouchListener(new FlickListener());
            rightView.findViewById(R.id.power).setOnTouchListener(new FlickListener());
        } else {
            resultView = (TextView) findViewById(R.id.textView1);
            undoView = (TextView) findViewById(R.id.textView0);
            redoView = (TextView) findViewById(R.id.textView2);
            findViewById(R.id.mr).setOnTouchListener(new FlickListener());
            findViewById(R.id.div).setOnTouchListener(new FlickListener());
            findViewById(R.id.dot).setOnTouchListener(new FlickListener());
            findViewById(R.id.zero2).setOnTouchListener(new FlickListener());
            findViewById(R.id.zero).setOnTouchListener(new FlickListener());
            findViewById(R.id.five).setOnTouchListener(new FlickListener());
            findViewById(R.id.power).setOnTouchListener(new FlickListener());
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        //設定値を読んで背景色変更
        try {
            String defColor = String.format("%06X", 0xffffff & getResources().getColor(R.color.container_bg)).toLowerCase();
            if (sp.getString("bg_color", "null").equals("null") || sp.getString("bg_color", "null") == null) {
                sp.edit().putString("bg_color", defColor).apply();
            }
            if (this.getResources().getBoolean(R.bool.isXlarge)) {
                leftView.findViewById(R.id.container).setBackgroundColor(Color.parseColor("#" + sp.getString("bg_color", defColor)));
                rightView.findViewById(R.id.container).setBackgroundColor(Color.parseColor("#" + sp.getString("bg_color", defColor)));
            } else {
                findViewById(R.id.container).setBackgroundColor(Color.parseColor("#" + sp.getString("bg_color", defColor)));
            }
        } catch (Exception e) {
            Log.e("", "Can't translate color in int.");
        }

        //resultViewを横スクロール可能に
        resultView.setMovementMethod(ScrollingMovementMethod.getInstance());
        resultView.setText("0");
        undoView.setText("");
        redoView.setText("");
        if (resultViewText != null) {
            resultView.setText(resultViewText);
        }
        if (undoViewText != null) {
            undoView.setText(undoViewText);
        }
        if (redoViewText != null) {
            redoView.setText(redoViewText);
        }
        if (valCom.size() == 0 && histValCom.size() == 0) {
            valCom.add(new ValCom(0));
            histValCom.add(new ValCom(0));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

        //設定値を読んで背景画像変更
        String path = sp.getString("bg_image_pick", "");
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        } catch (IOException e) {
            Log.e("ExifInterface", "Can't create ExifInterface.");
        }
        boolean isXlarge = this.getResources().getBoolean(R.bool.isXlarge);
        ImageView iv = null;
        ImageView ivL = null;
        ImageView ivR = null;
        if (isXlarge) {
            ivL = (ImageView) leftView.findViewById(R.id.imageView);
            ivR = (ImageView) rightView.findViewById(R.id.imageView);
        } else {
            iv = (ImageView) findViewById(R.id.imageView);
        }
        BitmapFactory.Options imageOptions = setBitmapOption(new BitmapFactory.Options());
        if (imageOptions != null) {
            imageWidth = imageOptions.outWidth;
            imageHeight = imageOptions.outHeight;
            Log.v("echo", "w:" + imageWidth + ", h:" + imageHeight);
            try {
                if (!isXlarge) {
                    //Log.v("onWindowFocusChanged0", "imageW:" + iv.getDrawable().getIntrinsicWidth() + ", imageH:" + iv.getDrawable().getIntrinsicHeight());
                    iv.setImageURI(Uri.parse(path));
                    //Log.v("onWindowFocusChanged1", "imageW:" + iv.getDrawable().getIntrinsicWidth() + ", imageH:" + iv.getDrawable().getIntrinsicHeight());
                    setMatrix(orientation, iv);
                } else {
                    ivL.setImageURI(Uri.parse(path));
                    setMatrix(orientation, ivL);
                    ivR.setImageURI(Uri.parse(path));
                    setMatrix(orientation, ivR);
                }
            } catch (NullPointerException e) {
                Log.e("OnWindowFocusChanged", "" + e.getMessage());
            }
            Log.v("onWindowFocusChanged", "ivWidth:" + ivWidth + ", ivHeight:" + ivHeight);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        resultViewText = resultView.getText().toString();
        undoViewText = undoView.getText().toString();
        redoViewText = redoView.getText().toString();

        Icepick.saveInstanceState(this, outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        BitmapDrawable bd = (BitmapDrawable) imageView.getDrawable();
        bd.getBitmap().recycle();
        imageView.setImageURI(null);
    }

    public BitmapFactory.Options setBitmapOption(BitmapFactory.Options bo) {
        bo.inJustDecodeBounds = true;
        ContentResolver contentResolver = getContentResolver();
        try {
            InputStream is = contentResolver.openInputStream(Uri.fromFile(new File(sp.getString("bg_image_pick", ""))));
            BitmapFactory.decodeStream(is, null, bo);
            is.close();
            return bo;
        } catch (Exception e) {
            Log.e("", "Unable to translate path to Uri.");
            return null;
        }
    }

    //typeの値によって表示内容を制御するToast
    public void showToast(String type) {
        int rString = 0, rLayout = 0;
        if (type.equals("copied")) {
            rString = R.string.copied;
            rLayout = R.layout.toast_copy;
        }
        if (type.equals("copy_failed")) {
            rString = R.string.copy_failed;
            rLayout = R.layout.toast_copy_failed;
        }
        if (type.equals("paste_failed")) {
            rString = R.string.paste_failed;
            rLayout = R.layout.toast_paste_failed;
        }
        if (type.equals("paste_unable")) {
            rString = R.string.paste_unable;
            rLayout = R.layout.toast_paste_unable;
        }
        try {
            Toast toast = Toast.makeText(main, rString, Toast.LENGTH_SHORT);
            LayoutInflater inflater = getLayoutInflater();
            toast.setView(inflater.inflate(rLayout, null));
            int contentHeight = findViewById(R.id.container).getHeight();
            Display disp = getWindowManager().getDefaultDisplay();
            Point p = new Point();
            disp.getSize(p);
            int dispHeight = p.y;
            toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, dispHeight - contentHeight + resultView.getMeasuredHeight() / 2);
            toast.show();
        } catch (Exception e) {
            Log.e("", "Can't show toast.");
        }
    }

    class ValCom implements Parcelable {
        String str;
        int index;
        boolean isCommand = false;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(str);
            parcel.writeInt(index);
            parcel.writeByte((byte) (isCommand ? 1 : 0));
        }

        public final Parcelable.Creator<ValCom> CREATOR = new Parcelable.Creator<ValCom>() {
            public ValCom createFromParcel(Parcel in) {
                return new ValCom(in);
            }
            public ValCom[] newArray(int size) {
                return new ValCom[size];
            }
        };

        public ValCom(Parcel in) {
            str = in.readString();
            index = in.readInt();
            isCommand = in.readByte() == 1;
        }

        public ValCom(int i) {
            str = "";
            index = i;
        }
        public ValCom(String s, int i) {
            str = s;
            index = i;
        }

        public void set(String s, int i) {
            str = s;
            index = i;
        }
        public void setStr(String s) {
            str = s;
        }
        public void setIndex(int i) {
            index = i;
        }
    }

    class Node {
        Node left = null, right = null;
        ArrayList<ValCom> exp = new ArrayList<>();
        boolean isError = false;
        boolean isZeroDiv = false;
        boolean doReCalc = false;
        String stackTrace = "";

        public Node(ArrayList<ValCom> expression) {
            exp = expression;
        }

        public int checkSyntax() {
            ValCom past = exp.size() > 0 ? exp.get(0) : null;
            for (int i = 0; i < exp.size(); i++) {
                ValCom present = exp.get(i);
                if (past == null ||
                        present.str.matches(".*\\.\\..*") ||
                        (i > 0 && ((present.str.matches(commandsRegexMini) && past.str.matches(commandsRegexMini)) ||
                                (present.str.matches(commandsRegex + "|left|right") && past.str.matches(".*\\.")) ||
                                (present.str.equals("plusminus") && !past.isCommand)))) {
                    Log.e("", "Failed to Compile with syntax error.");
                    return -1;
                }
                past = valCom.get(i);
            }
            return 0;
        }

        public void preCompile() {
            int brackets = 0;
            for (int i = 0; i < exp.size(); i++) {
                ValCom expId = exp.get(i);
                if (expId.str.matches("left|right") && !(i > 0 && exp.get(i - 1).str.matches("left|right"))) {
                    brackets++;
                }
                if (expId.str.equals("right") && (i > 0 && exp.get(i - 1).str.equals("left"))) {
                    exp.remove(i);
                    exp.remove(i - 1);
                    histP -= 2;
                }
                if (i > 0 && expId.str.equals("plusminus") && exp.get(i - 1).str.equals("plusminus")) {
                    exp.remove(i);
                    exp.remove(i - 1);
                    histP -= 2;
                }
                if (i > 0 && ((!expId.isCommand && !exp.get(i - 1).isCommand) || (expId.str.matches("loge|log10|log2|root|left") && !exp.get(i - 1).isCommand))) {
                    exp.add(i, new ValCom("multi", i));
                    exp.get(i).isCommand = true;
                }
            }
            if (brackets == 2 && exp.get(0).str.equals("left") && exp.get(exp.size() - 1).str.equals("right")) {
                exp = new ArrayList<>(exp.subList(1, exp.size() - 1));
                preCompile();
            }
            if (exp.size() == 1 && exp.get(0).str.equals("")) {
                addNum("0");
            }
        }

        public void parse() {
            int posOperator = getOperatorPos(exp);
            if (posOperator < 0) {
                if (exp.size() > 1) {
                    node.isError = true;
                }
                left = null;
                right = null;
                return;
            }

            // left-hand side
            if (posOperator != 0) {
                left = new Node(removeBracket(new ArrayList<>(exp.subList(0, posOperator))));
                left.parse();
            } else {
                left = null;
            }

            // right-hand side
            right = new Node(removeBracket(new ArrayList<>(exp.subList(posOperator + 1, exp.size()))));
            right.parse();

            // operator
            exp = new ArrayList<>(Arrays.asList(exp.get(posOperator)));
        }

        private ArrayList<ValCom> removeBracket(ArrayList<ValCom> expression) {
            if (expression.size() < 1) {
                isError = true;
                return expression;
            } else {
                if (!(expression.get(0).str.equals("left") && expression.get(expression.size() - 1).str.equals("right"))) {
                    return expression;
                }
                int nest = 1;

                for (int i = 1; i < expression.size() - 1; i++) {
                    ValCom expId = expression.get(i);
                    if (expId.str.equals("left")) {
                        nest++;
                    } else if (expId.str.equals("right")) {
                        nest--;
                    }
                    if (nest == 0) {
                        return expression;
                    }
                }

                if (nest != 1) {
                    Log.e("", "Unbalanced bracket.");
                    isError = true;
                    return expression;
                }

                expression = new ArrayList<>(expression.subList(1, expression.size() - 1));

                if (expression.get(0).str.equals("left")) {
                    return removeBracket(expression);
                } else {
                    return expression;
                }
            }
        }

        private int getOperatorPos(ArrayList<ValCom> expression) {
            if (expression == null || expression.size() == 0) {
                return -1;
            }

            int pos = -1;
            int nest = 0;
            int priority = 0;
            int lowestPriority = 5;
            boolean existLeftBefore = false;
            int leftPos = -1;

            for(int i = 0; i < expression.size(); i++) {
                ValCom v = expression.get(i);
                if (v.isCommand) {
                    if (v.str.equals("plus")) priority = 1;
                    if (v.str.equals("minus")) priority = 1;
                    if (v.str.equals("mod")) priority = 2;
                    if (v.str.equals("multi")) priority = 2;
                    if (v.str.equals("div")) priority = 2;
                    if (v.str.equals("power")) priority = 3;
                    if (v.str.equals("plusminus")) priority = 4;
                    if (v.str.equals("root")) priority = 5;
                    if (v.str.equals("loge")) priority = 5;
                    if (v.str.equals("log10")) priority = 5;
                    if (v.str.equals("log2")) priority = 5;
                    if (v.str.equals("left")) {
                        nest++;
                        leftPos = i;
                        existLeftBefore = true;
                    }
                    if (v.str.equals("right")) {
                        nest--;
                        if (!existLeftBefore ||
                            (leftPos > 0 && expression.get(leftPos + 1).isCommand && !expression.get(leftPos - 1).isCommand) ||
                            (i < expression.size() - 1 && expression.get(i - 1).isCommand && !expression.get(i + 1).isCommand)) {
                            if (leftPos > -1){
                                expression.remove(i);
                                expression.remove(leftPos);
                                histP -= 2;
                                for (ValCom vc: valCom) {
                                    Log.v("Echo", "valCom:" + vc.str);
                                }
                                doReCalc = true;
                            } else {
                                isError = true;
                            }
                        }
                    }

                    if (nest == 0 && priority <= lowestPriority && !(v.str.equals("left") || v.str.equals("right"))) {
                        lowestPriority = priority;
                        pos = i;
                    }
                }
            }
            if (nest != 0) {
                isError = true;
            }

            return pos;
        }

        public BigDecimal calculate() {
            if (left != null && right != null) {
                BigDecimal leftOperand  = left.calculate();
                BigDecimal rightOperand = right.calculate();

                if (exp.get(0).str.equals("plus")) {
                    if (left.stackTrace.equals("") && right.stackTrace.equals("")) {
                        return leftOperand.add(rightOperand);
                    } else if (!left.stackTrace.equals("") && !right.stackTrace.equals("")) {
                        node.isError = true;
                        return new BigDecimal(0.0);
                    } else {
                        stackTrace = left.stackTrace.equals("") ? right.stackTrace : left.stackTrace;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.equals("minus")) {
                    if (left.stackTrace.equals("") && right.stackTrace.equals("")) {
                        return leftOperand.subtract(rightOperand);
                    } else if (!left.stackTrace.equals("") && !right.stackTrace.equals("")) {
                        node.isError = true;
                        return new BigDecimal(0.0);
                    } else {
                        stackTrace = left.stackTrace.equals("") ? right.stackTrace : left.stackTrace;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.equals("mod")) {
                    if (left.stackTrace.equals("") && right.stackTrace.equals("")) {
                        return leftOperand.remainder(rightOperand);
                    } else {
                        node.isError = true;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.equals("multi")) {
                    if (left.stackTrace.equals("") && right.stackTrace.equals("")) {
                        return leftOperand.multiply(rightOperand);
                    } else if (!left.stackTrace.equals("") && !right.stackTrace.equals("")) {
                        node.isError = true;
                        return new BigDecimal(0.0);
                    } else {
                        stackTrace = left.stackTrace.equals("") ? right.stackTrace : left.stackTrace;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.equals("div")) {
                    if (rightOperand.doubleValue() == 0.0) {
                        if (right.stackTrace.equals("")) {
                            node.isZeroDiv = true;
                            return new BigDecimal(0.0);
                        } else if (left.stackTrace.equals("")) {
                            return new BigDecimal(0.0);
                        } else {
                            node.isError = true;
                            return new BigDecimal(0.0);
                        }
                    } else {
                        if (left.stackTrace.equals("") && right.stackTrace.equals("")) {
                            return leftOperand.divide(rightOperand, 50, BigDecimal.ROUND_HALF_UP);
                        } else if (!left.stackTrace.equals("") && !right.stackTrace.equals("")) {
                            node.isError = true;
                            return new BigDecimal(0.0);
                        } else {
                            stackTrace = left.stackTrace;
                            return new BigDecimal(0.0);
                        }
                    }
                }
                else if (exp.get(0).str.equals("power")) {
                    if (left.stackTrace.equals("") && right.stackTrace.equals("")) {
                        try {
                            return new BigDecimal(Math.pow(leftOperand.doubleValue(), rightOperand.doubleValue()), new MathContext(50));
                        } catch (NumberFormatException e) {
                            stackTrace = e.getMessage().replaceAll(".+\\s(.+)", "$1");
                            return new BigDecimal(0.0);
                        }
                    } else if (!left.stackTrace.equals("") && !right.stackTrace.equals("")) {
                        node.isError = true;
                        return new BigDecimal(0.0);
                    } else {
                        stackTrace = left.stackTrace.equals("") ? right.stackTrace : left.stackTrace;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.matches("root|loge|log10|log2|plusminus")) {
                    node.isError = true;
                    return new BigDecimal(0.0);
                }
                else {
                    return new BigDecimal(0.0);
                }
            } else if (right != null) {
                BigDecimal rightOperand = right.calculate();

                if (exp.get(0).str.equals("root")) {
                    if (right.stackTrace.equals("")) {
                        return new BigDecimal(Math.sqrt(rightOperand.doubleValue()));
                    } else {
                        stackTrace = right.stackTrace;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.equals("loge")) {
                    if (right.stackTrace.equals("")) {
                        try {
                            return new BigDecimal(Math.log(rightOperand.doubleValue()));
                        } catch (NumberFormatException e) {
                            stackTrace = e.getMessage().replaceAll(".+\\s(.+)", "$1");
                            return new BigDecimal(0.0);
                        }
                    } else {
                        stackTrace = right.stackTrace;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.equals("log10")) {
                    if (right.stackTrace.equals("")) {
                        try {
                            return new BigDecimal(Math.log10(rightOperand.doubleValue()));
                        } catch (NumberFormatException e) {
                            stackTrace = e.getMessage().replaceAll(".+\\s(.+)", "$1");
                            return new BigDecimal(0.0);
                        }
                    } else {
                        stackTrace = right.stackTrace;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.equals("log2")) {
                    if (right.stackTrace.equals("")) {
                        try {
                            return new BigDecimal(Math.log(rightOperand.doubleValue()) / Math.log(2));
                        } catch (NumberFormatException e) {
                            stackTrace = e.getMessage().replaceAll(".+\\s(.+)", "$1");
                            return new BigDecimal(0.0);
                        }
                    } else {
                        stackTrace = right.stackTrace;
                        return new BigDecimal(0.0);
                    }
                }
                else if (exp.get(0).str.equals("plusminus"))
                    if (right.stackTrace.equals("")) {
                        return rightOperand.negate();
                    } else {
                        stackTrace = right.stackTrace;
                        return new BigDecimal(0.0);
                    }
                else {
                    return new BigDecimal(0.0);
                }
            } else {
                if (exp.size() > 0 && exp.get(0).str.equals("pi"))
                    return new BigDecimal(Math.PI);
                else if (exp.size() > 0 && exp.get(0).str.equals("e"))
                    return new BigDecimal(Math.E);
                else {
                    try {
                        return new BigDecimal(exp.get(0).str);
                    } catch (Exception e) {
                        Log.e("", "Can't parse value into BigDecimal.");
                        node.isError = true;
                        return new BigDecimal(0.0);
                    }
                }
            }
        }
    }

    //計算実行
    Node node;
    public void calcOutput() {
        node = new Node(valCom);
        node.preCompile();
        if (node.checkSyntax() < 0) {
            resultView.setText(getText(R.string.syntax_error));
        } else {
            node.parse();
            if (node.isError) {
                resultView.setText(getText(R.string.syntax_error));
            } else {
                output = node.calculate();

                String errorStr = "";
                if (!node.stackTrace.equals("")) errorStr = node.stackTrace;
                if (node.isZeroDiv) errorStr = getText(R.string.zerodiv_error).toString();
                if (node.isError) errorStr = getText(R.string.syntax_error).toString();

                DecimalFormat bd = new DecimalFormat("#,##0.############");
                //出力
                resultView.setText(errorStr.equals("") ? "= " + bd.format(output) : errorStr);
            }
        }
    }

    //受け付けた数値の処理
    public void addNum(String num) {
        //undo後に値入力でそのindex以降のhistValComをクリア
        int histSize = histValCom.size();
        if (histP < histSize - 1) {
            for (int i = histP + 1; i < histSize; i++) {
                histValCom.remove(histP + 1);
            }
        }
        //'='入力後'AC'が入力されなかった場合
        if (valCom.size() > 0 && valCom.get(valCom.size() - 1).str.equals("equal")) {
            initState();
            clearHist();
        }

        //コマンドが追加されている場合などに数値を入れるListのindexを追加
        if (valCom.size() == 0 || ((valCom.size() > 1 || (valCom.size() == 1 && !valCom.get(0).str.equals(""))) && num.matches("^(e|pi)$")) || isEndCommand) {
            valCom.add(new ValCom(histP + 1));
            histValCom.add(new ValCom(histP + 1));
            if (valCom.size() > 1) histP++;
        }
        //Listの一番最後のindexに数値追加(index数はそのまま)
        if (valCom.get(valCom.size() - 1).str.equals("") && num.equals(".")) {
            valCom.get(valCom.size() - 1).setStr("0" + num);
            histValCom.get(histP).setStr("0" + num);
        } else {
            valCom.get(valCom.size() - 1).setStr(valCom.get(valCom.size() - 1).str + num);
            histValCom.get(histP).setStr(histValCom.get(histP).str + num);
        }
        //現在表示されている文字列取得
        //String text = resultView.getText().toString();
        //(初期値の0が表示されている時はそれを置き換えて)入力値を表示
        //resultView.setText(((text.equals("0")) ? "" : text) + ((num.equals(".") && (text.equals("0") || valCom.get(valCom.size() - 1).str.matches("0+\\."))) ? "0" : "") + ((text.equals("0") && num.equals("00")) ? "0" : (comName.get(num) == null ? num : comName.get(num))));
        resultView.setText("");
        for (ValCom tValCom: valCom) {
            resultView.setText(resultView.getText().toString() + (comName.get(tValCom.str) == null ? tValCom.str.replaceAll("^0+$", "0").replaceAll("^0+([1-9].*)", "$1").replaceAll("^0+(0\\..*)", "$1") : comName.get(tValCom.str)));
            resultView.setText(resultView.getText().toString().replace("--", ""));
        }

        refreshViewUndoRedo();
    }

    //受け付けたコマンドの処理
    public void addCommand(String com) {
        //'='入力後'AC'が入力されなかった場合
        if (valCom.size() > 0 && valCom.get(valCom.size() - 1).str.equals("equal")) {
            initState();
            clearHist();
            addNum(String.valueOf(output));
            if (output.doubleValue() < 0.0) {
                valCom.get(valCom.size() - 1).str = valCom.get(valCom.size() - 1).str.replaceAll("^0(.+)", "$1");
            }
        }
        //undo後にコマンド入力でそのindex以降のhistValComをクリア
        int histSize = histValCom.size();
        if (histP < histSize - 1) {
            for (int i = histP + 1; i < histSize; i++) {
                histValCom.remove(histP + 1);
            }
        }
        //初期状態にroot||loge||log10||log2||plusminusが入力された時は置換する
        if (valCom.size() == 1 && valCom.get(0).str.equals("") && com.matches("root|loge|log10|log2|plusminus")) {
            valCom.remove(0);
            histValCom.remove(0);
            histP--;
            resultView.setText("");
        }
        valCom.add(new ValCom(com, histP + 1));
        valCom.get(valCom.size() - 1).isCommand = true;
        histValCom.add(new ValCom(com, histP + 1));
        histValCom.get(histValCom.size() - 1).isCommand = true;
        if (valCom.size() == 2 && valCom.get(0).str.equals("") && com.equals("left")) {
            valCom.remove(0);
            histValCom.remove(0);
            resultView.setText("");
            histP--;
        }
        histP++;

        refreshViewUndoRedo();

        //現在表示されている文字列取得
        String text = resultView.getText().toString();
        //コマンドを表示する形式に変換
        String showCom = comName.get(com) == null ? "ex" : comName.get(com);

        //resultView.setText(com.equals("equal") ? text : (valCom.size() > 1 && com.equals("plusminus") && valCom.get(valCom.size() - 2).str.equals("plusminus") ? text.replaceAll("^(.+\\s)-(.*)", "$1$2").replaceAll("^=\\s(.+)", "$1") : text.replaceAll("^=\\s(.+)", "$1") + showCom));
        resultView.setText(com.equals("equal") ? text : text.replaceAll("^=\\s(.+)", "$1") + showCom);
        resultView.setText(resultView.getText().toString().replace("--", ""));
    }

    public void refreshViewUndoRedo() {
        //undoView表示更新
        if (histP > 0) {
            String histStr = histValCom.get(histP - 1).str;
            histStr = histStr.replaceAll("^0+([1-9].*)", "$1").replaceAll("^0*(0\\.\\d+)", "$1").replaceAll("^0([1-9]+\\.\\d+)", "$1");
            histStr = comName.get(histStr) == null ? histStr : comName.get(histStr);
            undoView.setText(histStr);
        } else {
            undoView.setText("");
        }
        //redoView表示更新
        if (histP < histValCom.size() - 1) {
            String histStr = histValCom.get(histP + 1).str;
            histStr = histStr.replaceAll("^0+([1-9].*)", "$1").replaceAll("^0*(0\\.\\d+)", "$1").replaceAll("^0([1-9]+\\.\\d+)", "$1");
            histStr = comName.get(histStr) == null ? histStr : comName.get(histStr);
            redoView.setText(histStr);
        } else {
            redoView.setText("");
        }
    }

    public void initState() {
        resultView.setText("0");
        undoView.setText("");
        redoView.setText("");
        valCom.clear();
        valCom.add(new ValCom(0));
        isEndCommand = false;
    }

    public void clearHist() {
        histValCom.clear();
        histValCom.add(new ValCom(0));
        histP = 0;
    }

    //xlarge用のレイアウト切替えに伴う諸処理
    public void flipButtonsView() {
        if (this.getResources().getBoolean(R.bool.isXlarge)) {
            String tempUndoStr = redoView.getText().toString();
            String tempResultStr = resultView.getText().toString();
            String tempRedoStr = redoView.getText().toString();
            if (isButtonsLeft) {
                container.removeAllViews();
                container.addView(rightView);

                undoView = (TextView) rightView.findViewById(R.id.textView0);
                undoView.setText(tempUndoStr);
                resultView = (TextView) rightView.findViewById(R.id.textView1);
                resultView.setText(tempResultStr);
                redoView = (TextView) rightView.findViewById(R.id.textView2);
                redoView.setText(tempRedoStr);
                rightView.findViewById(R.id.mr).setOnTouchListener(new FlickListener());
                rightView.findViewById(R.id.ac).setOnTouchListener(new FlickListener());
                rightView.findViewById(R.id.div).setOnTouchListener(new FlickListener());
                rightView.findViewById(R.id.dot).setOnTouchListener(new FlickListener());
                rightView.findViewById(R.id.zero2).setOnTouchListener(new FlickListener());
                rightView.findViewById(R.id.zero).setOnTouchListener(new FlickListener());
                rightView.findViewById(R.id.five).setOnTouchListener(new FlickListener());
                rightView.findViewById(R.id.power).setOnTouchListener(new FlickListener());
            } else {
                container.removeAllViews();
                container.addView(leftView);

                undoView = (TextView) leftView.findViewById(R.id.textView0);
                undoView.setText(tempUndoStr);
                resultView = (TextView) leftView.findViewById(R.id.textView1);
                resultView.setText(tempResultStr);
                redoView = (TextView) leftView.findViewById(R.id.textView2);
                redoView.setText(tempRedoStr);
                leftView.findViewById(R.id.mr).setOnTouchListener(new FlickListener());
                leftView.findViewById(R.id.ac).setOnTouchListener(new FlickListener());
                leftView.findViewById(R.id.div).setOnTouchListener(new FlickListener());
                leftView.findViewById(R.id.dot).setOnTouchListener(new FlickListener());
                leftView.findViewById(R.id.zero2).setOnTouchListener(new FlickListener());
                leftView.findViewById(R.id.zero).setOnTouchListener(new FlickListener());
                leftView.findViewById(R.id.five).setOnTouchListener(new FlickListener());
                leftView.findViewById(R.id.power).setOnTouchListener(new FlickListener());
            }
            isButtonsLeft = !isButtonsLeft;
        }
    }

    public void onClick00(View v) {
        addNum("00");
        isEndCommand = false;
    }

    public void onClick0(View v) {
        addNum("0");
        isEndCommand = false;
    }

    public void onClick1(View v) {
        addNum("1");
        isEndCommand = false;
    }

    public void onClick2(View v) {
        addNum("2");
        isEndCommand = false;
    }

    public void onClick3(View v) {
        addNum("3");
        isEndCommand = false;
    }

    public void onClick4(View v) {
        addNum("4");
        isEndCommand = false;
    }

    public void onClick5(View v) {
        addNum("5");
        isEndCommand = false;
    }

    public void onClick6(View v) {
        addNum("6");
        isEndCommand = false;
    }

    public void onClick7(View v) {
        addNum("7");
        isEndCommand = false;
    }

    public void onClick8(View v) {
        addNum("8");
        isEndCommand = false;
    }

    public void onClick9(View v) {
        addNum("9");
        isEndCommand = false;
    }

    public void onClickDot(View v) {
        addNum(".");
        isEndCommand = false;
    }

    public void onClickAc(View v) {
        initState();
        clearHist();
    }

    public void onClickEqual(View v) {
        calcOutput();
        while (node.doReCalc) {
            calcOutput();
        }
        valCom.add(new ValCom("equal", valCom.size()));
        histP++;
        refreshViewUndoRedo();
    }

    public void onClickUndo(View v) {
        //一旦メイン記憶用Listをクリアして履歴保存用Listのポインタを1つ過去側にずらし、メインにリセット
        if (!(histP == 0)) {
            //メインListクリア
            initState();
            resultView.setText("");
            histP--;
            //メインListリセット
            valCom.clear();
            for (int i = 0; i <= histP; i++) {
                //クラスをそのままコピーするとエイリアスになってしまってバグを生むので新しく生成
                valCom.add(new ValCom(histValCom.get(i).str, i));
                valCom.get(i).isCommand = histValCom.get(i).isCommand;
                isEndCommand = false;
                String histVal = histValCom.get(i).str;
                resultView.setText(resultView.getText().toString() + (comName.get(histVal) == null ? histVal.replaceAll("^0+([1-9].*)", "$1").replaceAll("^0+(0\\..+)", "$1") : comName.get(histVal)));
                resultView.setText(resultView.getText().toString().replace("--", ""));
                if ( i == histP && histValCom.get(i).isCommand ) isEndCommand = true;
            }

            refreshViewUndoRedo();
        }
    }

    public void onClickRedo(View v) {
        if (histP < histValCom.size() - 1) {
            initState();
            resultView.setText("");
            histP++;
            valCom.clear();
            for (int i = 0; i <= histP; i++) {
                valCom.add(new ValCom(histValCom.get(i).str, i));
                valCom.get(i).isCommand = histValCom.get(i).isCommand;
                isEndCommand = false;
                String histVal = histValCom.get(i).str;
                resultView.setText(resultView.getText().toString() + (comName.get(histVal) == null ? histVal.replaceAll("^0+([1-9].*)", "$1").replaceAll("^0+(0\\..+)", "$1") : comName.get(histVal)));
                resultView.setText(resultView.getText().toString().replace("--", ""));
                if ( i == histP && histValCom.get(i).isCommand ) isEndCommand = true;
            }

            refreshViewUndoRedo();
        }
    }

    public void onClickMr(View v) {
        if (mValue != 0.0) {
            addNum(String.valueOf(mValue).replaceAll("(.+)\\.0$", "$1"));
            isEndCommand = false;
        }
    }

    public void onClickMp(View v) {
        if (valCom.size() <= 1 || valCom.get(valCom.size() - 1).str.equals("equal")) {
            String tempStr = resultView.getText().toString().replaceAll("=?\\s(.*)$", "$1");
            mValue += Double.parseDouble(tempStr);
        }
    }

    public void onClickMm(View v) {
        if (valCom.size() <= 1 || valCom.get(valCom.size() - 1).str.equals("equal")) {
            String tempStr = resultView.getText().toString().replaceAll("=?\\s(.*)$", "$1");
            mValue -= Double.parseDouble(tempStr);
        }
    }

    public void onClickMc(View v) {
        mValue = 0.0;
    }

    public void onClickPlus(View v) {
        addCommand("plus");
        isEndCommand = true;
    }

    public void onClickMinus(View v) {
        addCommand("minus");
        isEndCommand = true;
    }

    public void onClickMultiple(View v) {
        addCommand("multi");
        isEndCommand = true;
    }

    public void onClickDivision(View v) {
        addCommand("div");
        isEndCommand = true;
    }

    public void onClickPower(View v) {
        addCommand("power");
        isEndCommand = true;
    }

    public void onClickMod(View v) {
        addCommand("mod");
        isEndCommand = true;
    }

    public void onClickTax(View v) {
        addCommand("multi");
        isEndCommand = true;
        addNum("1.08");
        isEndCommand = false;
    }

    public void onClickRoot(View v) {
        addCommand("root");
        isEndCommand = true;
    }

    public void onClickLogE(View v) {
        addCommand("loge");
        isEndCommand = true;
    }

    public void onClickLog10(View v) {
        addCommand("log10");
        isEndCommand = true;
    }

    public void onClickLog2(View v) {
        addCommand("log2");
        isEndCommand = true;
    }

    public void onClickPi(View v) {
        addNum("pi");
        isEndCommand = false;
    }

    public void onClickE(View v) {
        addNum("e");
        isEndCommand = false;
    }

    public void onClickLeft(View v) {
        addCommand("left");
        isEndCommand = true;
    }

    public void onClickRight(View v) {
        addCommand("right");
        isEndCommand = true;
    }

    public void onClickPlusMinus(View v) {
        addCommand("plusminus");
        isEndCommand = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //menuレイアウトを表示
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            //menuから"設定"が選ばれた時
            case R.id.action_settings:
                //Pref.javaにintentを渡す
                startActivityForResult(new Intent(this, Pref.class), 0);
                return true;

            case R.id.action_copy:
                if ((valCom.size() > 0 && valCom.get(valCom.size() - 1).str.equals("equal")) || histP == 0) {
                    //コピー実行
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("text_data", resultView.getText().toString().replace(",", "").replaceAll("^=\\s(.+)", "$1").replaceAll("(.+)\\.0$", "$1")));

                    showToast("copied");
                } else {
                    showToast("copy_failed");
                }
                return true;

            case R.id.action_flip:
                flipButtonsView();
                return true;

            case R.id.action_paste:
                double tempD = 0.0;
                boolean pastable = true;
                //クリップボードの内容取得
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData cd = cm.getPrimaryClip();
                if (cd != null) {
                    try {
                        tempD = Double.parseDouble((String) cd.getItemAt(0).getText());
                    } catch (Exception e) {
                        Log.e("", "Can't translate the value on clipboard.");
                        showToast("paste_failed");
                        pastable = false;
                    }
                }
                if (!(isEndCommand || (valCom.size() == 1 && valCom.get(0).str.equals("")))) {
                    showToast("paste_unable");
                    pastable = false;
                }
                if (pastable) {
                    addNum(String.valueOf(tempD).replaceAll("(.+)\\.0$", "$1"));
                }
                return pastable;
        }
        return super.onOptionsItemSelected(item);
    }

    //他classへのintentの戻り値を受け付ける
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //Pref.javaからの戻り値の場合
        if (requestCode == 0){
            if (resultCode == Activity.RESULT_OK) {
                //設定値を読み込み背景色を変える
                try {
                    String defColor = String.valueOf(getResources().getColor(R.color.container_bg)).replace("#", "");
                    if (this.getResources().getBoolean(R.bool.isXlarge)) {
                        leftView.findViewById(R.id.container).setBackgroundColor(Color.parseColor("#" + sp.getString("bg_color", defColor)));
                        rightView.findViewById(R.id.container).setBackgroundColor(Color.parseColor("#" + sp.getString("bg_color", defColor)));
                    } else {
                        findViewById(R.id.container).setBackgroundColor(Color.parseColor("#" + sp.getString("bg_color", defColor)));
                    }
                } catch (Exception e) {
                    Log.e("", "Can't translate color in int.");
                }
                //設定値を読み込み背景画像を変える
                boolean isXlarge = this.getResources().getBoolean(R.bool.isXlarge);
                String path = "";
                Uri imageUri = intent.getData();
                try {
                    path = getPath(imageUri, this);
                    sp.edit().putString("bg_image_pick", path).apply();
                } catch (NullPointerException e) {
                    Log.e("onActivityResult", "path:" + e.getMessage());
                }
                showImageMode = Integer.parseInt(sp.getString("bg_image_scale_mode", "0"));
                ImageView iv = null;
                ImageView ivL = null;
                ImageView ivR = null;
                if (isXlarge) {
                    ivL = (ImageView) leftView.findViewById(R.id.imageView);
                    ivR = (ImageView) rightView.findViewById(R.id.imageView);
                } else {
                    iv = (ImageView) findViewById(R.id.imageView);
                }

                if (path == null || intent.getBooleanExtra("clearbg", false)) {
                    if (isXlarge) {
                        setNullImage(ivL);
                        setNullImage(ivR);
                    } else {
                        setNullImage(iv);
                    }
                }
            }
        }
    }

    public void setMatrix(int orientation, ImageView imageView) {
        Matrix matrix = new Matrix();
        matrix.reset();
        float midW = 0;
        float midH = 0;
        boolean isTurned = false;

        if (showImageMode == 0) {
            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    Log.v("echo", "UNDEFINED");
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                    Log.v("echo", "NORMAL");
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1f, 1f);
                    Log.v("echo", "FLIP_HORIZONTAL");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180f);
                    midW = imageWidth;
                    midH = imageHeight;
                    matrix.postTranslate(midW, midH);
                    Log.v("echo", "ROTATE_180");
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1f, -1f);
                    Log.v("echo", "FLIP_VERTICAL");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    isTurned = true;
                    matrix.postRotate(90f);
                    midW = imageHeight;
                    matrix.postTranslate(midW, midH);
                    Log.v("echo", "ROTATE_90");
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    isTurned = true;
                    matrix.postRotate(-90f);
                    midH = imageWidth;
                    matrix.postTranslate(midW, midH);
                    matrix.postScale(1f, -1f);
                    Log.v("echo", "TRANSVERSE");
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    isTurned = true;
                    matrix.postRotate(90f);
                    midW = imageHeight;
                    matrix.postTranslate(midW, midH);
                    matrix.postScale(1f, -1f);
                    Log.v("echo", "TRANSPOSE");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    isTurned = true;
                    matrix.postRotate(-90f);
                    midH = imageWidth;
                    matrix.postTranslate(midW, midH);
                    Log.v("echo", "ROTATE_270");
                    break;
            }
            imageView.setLayoutParams(new LinearLayout.LayoutParams(isTurned ? imageHeight : imageWidth, isTurned ? imageWidth : imageHeight));
        }
        if (showImageMode == 1) {
            float scale = 1f;
            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    scale = getScale(isTurned);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "UNDEFINED");
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                    scale = getScale(isTurned);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "NORMAL");
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1f, 1f);
                    scale = getScale(isTurned);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "FLIP_HORIZONTAL");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180f);
                    midW = imageWidth;
                    midH = imageHeight;
                    matrix.postTranslate(midW, midH);
                    scale = getScale(isTurned);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "ROTATE_180");
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1f, -1f);
                    scale = getScale(isTurned);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "FLIP_VERTICAL");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    isTurned = true;
                    scale = getScale(isTurned);
                    matrix.postRotate(90f);
                    midW = imageHeight;
                    matrix.postTranslate(midW, midH);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "ROTATE_90");
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    isTurned = true;
                    matrix.postRotate(-90f);
                    midH = imageWidth;
                    matrix.postTranslate(midW, midH);
                    matrix.postScale(1f, -1f);
                    scale = getScale(isTurned);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "TRANSVERSE");
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    isTurned = true;
                    matrix.postRotate(90f);
                    midW = imageHeight;
                    matrix.postTranslate(midW, midH);
                    matrix.postScale(1f, -1f);
                    scale = getScale(isTurned);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "TRANSPOSE");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    isTurned = true;
                    matrix.postRotate(-90f);
                    midH = imageWidth;
                    matrix.postTranslate(midW, midH);
                    scale = getScale(isTurned);
                    matrix.postScale(scale, scale);
                    Log.v("echo", "ROTATE_270");
                    break;
            }
            Log.v("echo", "scale:" + scale);
            imageView.setLayoutParams(new LinearLayout.LayoutParams((int)((isTurned ? imageHeight :imageWidth) * scale), (int)((isTurned ? imageWidth :imageHeight) * scale)));
        }
        ivWidth = imageView.getWidth();
        ivHeight = imageView.getHeight();
        Log.v("echo", "midW:" + midW + ", midH:" + midH);
        Log.v("echo", "matrix: " + matrix);
        if (imageView.getDrawable() == null) {
            Log.v("error", "imageView.getDrawable():null");
        }
        imageView.setImageMatrix(matrix);
    }

    public float getScale(boolean mode) {
        float rootW = findViewById(R.id.container).getWidth();
        float rootH = findViewById(R.id.container).getHeight();

        if (!mode) {
            Log.v("getScale", "rootW:" + rootW + ", rootH:" + rootH);
            Log.v("getScale", "imageWidth:" + imageWidth + ", imageHeight:" + imageHeight);
            return Math.min(rootW / (float)(imageWidth), rootH / (float)(imageHeight));
        } else {
            Log.v("getScale", "ivWidth:" + ivWidth + ", ivHeight:" + ivHeight);
            Log.v("getScale", "imageWidth:" + imageWidth + ", imageHeight:" + imageHeight);
            return Math.min(rootW / (float)(imageHeight), rootH / (float)(imageWidth));
        }
    }

    public String getPath(Uri uri, Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String[] columns = { MediaStore.Images.Media.DATA };
        Cursor cursor = contentResolver.query(uri, columns, null, null, null);
        cursor.moveToFirst();
        return cursor.getString(0);
    }

    public void setNullImage(ImageView imageView) {
        sp.edit().putString("bg_image_pick", "").apply();
        imageView.setImageResource(R.drawable.bg_image);
    }

    //フリック入力のリスナー
    private class FlickListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float upX, upY;
            float width = v.getWidth();
            float height = v.getHeight();
            String idName = getResources().getResourceEntryName(v.getId());

            if (idName.equals("mr")) {
                switch (event.getAction()) {
                    //タッチ
                    case MotionEvent.ACTION_DOWN:
                        //ボタンの背景色を青系に
                        v.setBackgroundColor(getResources().getColor(R.color.color21));
                        break;
                    //スワイプ
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        //指の現在位置によってボタンの背景色を制御
                        if (width < currentX && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color22));
                        } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color21));
                        } else {
                            v.setBackgroundColor(getResources().getColor(R.color.color20));
                        }
                        break;
                    //リリース
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        upX = event.getX();
                        upY = event.getY();
                        //指が離された位置によって実行するメソッドを変更
                        if (width < upX && 0 <= upY && upY <= height) {
                            onClickMc(v);
                        } else if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                            onClickMr(v);
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.color20));
                        break;
                }
            }

            if (idName.equals("ac")) {
                switch (event.getAction()) {
                    //タッチ
                    case MotionEvent.ACTION_DOWN:
                        //ボタンの背景色を青系に
                        v.setBackgroundColor(getResources().getColor(R.color.color01));
                        break;
                    //スワイプ
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        //指の現在位置によってボタンの背景色を制御
                        if (isButtonsLeft) {
                            if (width < currentX && 0 <= currentY && currentY <= height) {
                                v.setBackgroundColor(getResources().getColor(R.color.color02));
                            } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                                v.setBackgroundColor(getResources().getColor(R.color.color01));
                            } else {
                                v.setBackgroundColor(getResources().getColor(R.color.color00));
                            }
                        } else {
                            if (currentX < 0 && 0 <= currentY && currentY <= height) {
                                v.setBackgroundColor(getResources().getColor(R.color.color02));
                            } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                                v.setBackgroundColor(getResources().getColor(R.color.color01));
                            } else {
                                v.setBackgroundColor(getResources().getColor(R.color.color00));
                            }
                        }
                        break;
                    //リリース
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        upX = event.getX();
                        upY = event.getY();
                        //指が離された位置によって実行するメソッドを変更
                        if (isButtonsLeft) {
                            if (width < upX && 0 <= upY && upY <= height) {
                                flipButtonsView();
                            } else if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                                onClickAc(v);
                            }
                        } else {
                            if (upX < 0 && 0 <= upY && upY <= height) {
                                flipButtonsView();
                            } else if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                                onClickAc(v);
                            }
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.color00));
                        break;
                }
            }

            if (idName.equals("div")) {
                switch (event.getAction()) {
                    //タッチ
                    case MotionEvent.ACTION_DOWN:
                        //ボタンの背景色を青系に
                        v.setBackgroundColor(getResources().getColor(R.color.color11));
                        break;
                    //スワイプ
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        //指の現在位置によってボタンの背景色を制御
                        if (currentX < 0 && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color12));
                        } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color11));
                        } else {
                            v.setBackgroundColor(getResources().getColor(R.color.color10));
                        }
                        break;
                    //リリース
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        upX = event.getX();
                        upY = event.getY();
                        //指が離された位置によって実行するメソッドを変更
                        if (upX < 0 && 0 <= upY && upY <= height) {
                            onClickMod(v);
                        } else if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                            onClickDivision(v);
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.color10));
                        break;
                }
            }

            if (idName.equals("power")) {
                switch (event.getAction()) {
                    //タッチ
                    case MotionEvent.ACTION_DOWN:
                        //ボタンの背景色を青系に
                        v.setBackgroundColor(getResources().getColor(R.color.color11));
                        break;
                    //スワイプ
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        //指の現在位置によってボタンの背景色を制御
                        if (currentX < 0 && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color12));
                        } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color11));
                        } else {
                            v.setBackgroundColor(getResources().getColor(R.color.color10));
                        }
                        break;
                    //リリース
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        upX = event.getX();
                        upY = event.getY();
                        //指が離された位置によって実行するメソッドを変更
                        if (upX < 0 && 0 <= upY && upY <= height) {
                            onClickPlusMinus(v);
                        } else if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                            onClickPower(v);
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.color10));
                        break;
                }
            }

            if (idName.equals("five")) {
                switch (event.getAction()) {
                    //タッチ
                    case MotionEvent.ACTION_DOWN:
                        //ボタンの背景色を青系に
                        v.setBackgroundColor(getResources().getColor(R.color.color31));
                        break;
                    //スワイプ
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        //指の現在位置によってボタンの背景色を制御
                        if ((currentX < 0 && 0 <= currentY && currentY <= height) ||
                                (currentX > width && 0 <= currentY && currentY <= height)){
                            v.setBackgroundColor(getResources().getColor(R.color.color32));
                        } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color31));
                        } else {
                            v.setBackgroundColor(getResources().getColor(R.color.color30));
                        }
                        break;
                    //リリース
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        upX = event.getX();
                        upY = event.getY();
                        //指が離された位置によって実行するメソッドを変更
                        if (upX < 0 && 0 <= upY && upY <= height) {
                            onClickLeft(v);
                        }
                        if (upX > width && 0 <= upY && upY <= height) {
                            onClickRight(v);
                        }
                        if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                            onClick5(v);
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.color30));
                        break;
                }
            }

            if (idName.equals("dot")) {
                switch (event.getAction()) {
                    //タッチ
                    case MotionEvent.ACTION_DOWN:
                        //ボタンの背景色を青系に
                        v.setBackgroundColor(getResources().getColor(R.color.color31));
                        break;
                    //スワイプ
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        //指の現在位置によってボタンの背景色を制御
                        if ((currentY < 0 && 0 <= currentX && currentX <= width) ||
                                ((currentX < 0 || currentX >= width) && 0 <= currentY && currentY <= height)){
                            v.setBackgroundColor(getResources().getColor(R.color.color32));
                        } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color31));
                        } else {
                            v.setBackgroundColor(getResources().getColor(R.color.color30));
                        }
                        break;
                    //リリース
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        upX = event.getX();
                        upY = event.getY();
                        //指が離された位置によって実行するメソッドを変更
                        if (upY < 0 && 0 <= upX && upX <= width) {
                            onClickTax(v);
                        }
                        if (upX < 0 && 0 <= upY && upY <= height) {
                            onClickPi(v);
                        }
                        if (upX > width && 0 <= upY && upY <= height) {
                            onClickE(v);
                        }
                        if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                            onClickDot(v);
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.color30));
                        break;
                }
            }

            if (idName.equals("zero2")) {
                switch (event.getAction()) {
                    //タッチ
                    case MotionEvent.ACTION_DOWN:
                        //ボタンの背景色を青系に
                        v.setBackgroundColor(getResources().getColor(R.color.color31));
                        break;
                    //スワイプ
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        //指の現在位置によってボタンの背景色を制御
                        if (currentY < 0 && 0 <= currentX && currentX <= width) {
                            v.setBackgroundColor(getResources().getColor(R.color.color32));
                        } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color31));
                        } else {
                            v.setBackgroundColor(getResources().getColor(R.color.color30));
                        }
                        break;
                    //リリース
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        upX = event.getX();
                        upY = event.getY();
                        //指が離された位置によって実行するメソッドを変更
                        if (upY < 0 && 0 <= upX && upX <= width) {
                            onClickRoot(v);
                        } else if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                            onClick00(v);
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.color30));
                        break;
                }
            }

            if (idName.equals("zero")) {
                switch (event.getAction()) {
                    //タッチ
                    case MotionEvent.ACTION_DOWN:
                        //ボタンの背景色を青系に
                        v.setBackgroundColor(getResources().getColor(R.color.color31));
                        break;
                    //スワイプ
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        //指の現在位置によってボタンの背景色を制御
                        if ((currentY < 0 && 0 <= currentX && currentX <= width) || ((currentX < 0 || currentX > width) && 0 <= currentY && currentY <= height)) {
                            v.setBackgroundColor(getResources().getColor(R.color.color32));
                        } else if (0 <= currentX && currentX <= width && 0 <= currentY && currentY <= height) {
                            v.setBackgroundColor(getResources().getColor(R.color.color31));
                        } else {
                            v.setBackgroundColor(getResources().getColor(R.color.color30));
                        }
                        break;
                    //リリース
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        upX = event.getX();
                        upY = event.getY();
                        //指が離された位置によって実行するメソッドを変更
                        if (upY < 0 && 0 <= upX && upX <= width) {
                            onClickLogE(v);
                        } else if (upX < 0 && 0 <= upY && upY <= height) {
                            onClickLog10(v);
                        } else if (upX > width && 0 <= upY && upY <= height) {
                            onClickLog2(v);
                        } else if (0 <= upX && upX <= width && 0 <= upY && upY <= height) {
                            onClick0(v);
                        }
                        v.setBackgroundColor(getResources().getColor(R.color.color30));
                        break;
                }
            }
            return true;
        }
    }
}
