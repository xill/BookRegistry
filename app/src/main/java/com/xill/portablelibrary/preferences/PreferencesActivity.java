package com.xill.portablelibrary.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.xill.portablelibrary.R;

/**
 * Created by Sami on 5/26/2018.
 */

public class PreferencesActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
	}

	public static class MyPreferenceFragment extends PreferenceFragment
	{
		private SharedPreferences pref = null;
		private Context context = null;

		private final String SEARCH_TYPE = "searchType";
		private final String LANG_TYPE = "searchLangType";

		@Override
		public void onCreate(final Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			context = getActivity();

			// listener for search type changes.
			final ListPreference prefListThemes = (ListPreference) findPreference(SEARCH_TYPE);
			prefListThemes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					updateSettingsText(Integer.parseInt(newValue.toString()));
					return true;
				}
			});
			// listener for lang filter type changes.
			final ListPreference prefLangList = (ListPreference) findPreference(LANG_TYPE);
			prefLangList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					updateLangSettingsText(Integer.parseInt(newValue.toString()));
					return true;
				}
			});
			pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

			// set default search type.
			setDefaults();
		}

		private void setDefaults() {
			int ind = Integer.parseInt(pref.getString(SEARCH_TYPE,"0"));
			updateSettingsText(ind);
			ind = Integer.parseInt(pref.getString(LANG_TYPE,"0"));
			updateLangSettingsText(ind);
		}

		private void updateSettingsText(int index) {
			String[] values = context.getResources().getStringArray(R.array.searchListArray);
			Preference prefItem = (Preference) findPreference(SEARCH_TYPE);
			prefItem.setSummary(values[index]);
		}

		private void updateLangSettingsText(int index) {
			String[] values = context.getResources().getStringArray(R.array.searchLangListArray);
			Preference prefItem = (Preference) findPreference(LANG_TYPE);
			prefItem.setSummary(values[index]);
		}
	}
}
