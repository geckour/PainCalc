package jp.org.example.geckour.paincalc;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.jpn.geckour.calculator.app.R;

public class Pref extends PreferenceActivity {
    static Preference imagePicker;
    static Preference scaleMode;
    static Preference imageClearer;
    static SharedPreferences sp;
    Activity tempActivity = this;
    static Activity prefActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        Tracker t = ((Analytics) getApplication()).getTracker(Analytics.TrackerName.APP_TRACKER);
        t.setScreenName("PreferenceActivity");
        t.send(new HitBuilders.AppViewBuilder().build());

        prefActivity = tempActivity;
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefFragment()).commit();
    }

    public static boolean isBgColorChanged (Preference preference, Object newValue) {
        preference.setSummary((String) newValue);
        Intent intent = new Intent();
        intent.putExtra("color", "#" + newValue);
        prefActivity.setResult(Activity.RESULT_OK, intent);

        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1){
            if(resultCode == Activity.RESULT_OK) {
                Uri imageUri = intent.getData();
                String path = getPath(imageUri, this);
                imagePicker.setSummary(path);
                setResult(Activity.RESULT_OK, intent);
            }
        }
    }

    public static class PrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);  //設定画面を追加
            final Preference.OnPreferenceChangeListener prefChangeListener = new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return isBgColorChanged(preference, newValue);
                }
            };
            EditTextPreference editTextPreference = (EditTextPreference) findPreference("bg_color");
            editTextPreference.setOnPreferenceChangeListener(prefChangeListener);
            editTextPreference.setSummary("#" + editTextPreference.getText());

            scaleMode = findPreference("bg_image_scale_mode");
            scaleMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Intent intent = new Intent();
                    intent.putExtra("scale_mode", (String)newValue);
                    prefActivity.setResult(Activity.RESULT_OK, intent);
                    return true;
                }
            });

            imagePicker = findPreference("bg_image_pick");
            imagePicker.setSummary(sp.getString("bg_image_pick", ""));
            imagePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(intent, 1);
                    return true;
                }
            });

            imageClearer = findPreference("bg_image_clear");
            imageClearer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent();
                    intent.putExtra("clearbg", true);
                    imagePicker.setSummary("");
                    prefActivity.setResult(Activity.RESULT_OK, intent);
                    return true;
                }
            });
        }

        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            if (requestCode == 1){
                if(resultCode == Activity.RESULT_OK) {
                    Uri imageUri = intent.getData();
                    String path = getPath(imageUri, prefActivity);
                    imagePicker.setSummary(path);
                    prefActivity.setResult(Activity.RESULT_OK, intent);
                }
            }
        }
    }

    public static String getPath(Uri uri, Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String[] columns = { MediaStore.Images.Media.DATA };
        Cursor cursor = contentResolver.query(uri, columns, null, null, null);
        cursor.moveToFirst();
        return cursor.getString(0);
    }
}
