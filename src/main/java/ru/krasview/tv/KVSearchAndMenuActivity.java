package ru.krasview.tv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;
import com.example.kvlib.R;
import ru.krasview.kvlib.adapter.CombineSimpleAdapter;
import ru.krasview.kvlib.indep.AuthAccount;
import ru.krasview.kvlib.indep.HTTPClient;
import ru.krasview.kvlib.indep.ListAccount;
import ru.krasview.kvlib.indep.consts.AuthRequestConst;
import ru.krasview.kvlib.indep.consts.TagConsts;
import ru.krasview.kvlib.indep.consts.TypeConsts;
import ru.krasview.kvlib.interfaces.FatalErrorExitListener;
import ru.krasview.kvlib.widget.List;


import java.util.Map;

public abstract class KVSearchAndMenuActivity extends AppCompatActivity
									 implements SearchView.OnQueryTextListener, FatalErrorExitListener{

	AuthAccount account = AuthAccount.getInstance();

	EditText editsearch;
	SearchFragment searchFragment;
	View searchHost;
	private View mSearchEditFrame;

	protected abstract void setSearch(boolean a);
	protected abstract void exit();
	protected abstract void home();
	protected abstract void refresh();
	protected abstract void requestFocus();

	@Override
	public void onError() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("Krasview/core", "onCreate");
		super.onCreate(savedInstanceState);
	}

	protected void setSearchWidget() {
		//установка поисковых штук
		searchHost = findViewById(R.id.search);
		FragmentManager fragmentManager = getSupportFragmentManager();
		searchFragment = (SearchFragment) fragmentManager.findFragmentById(R.id.fragment1);
		setSearch(false);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem menuSearch = menu.findItem(R.id.kv_search_item);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuSearch);
		//SearchView searchView = (SearchView) menuSearch.getActionView();

		if(searchView != null) {
			searchView.setFocusable(true);
			searchView.setOnQueryTextListener(this);
		}
		MenuItemCompat.setOnActionExpandListener(menuSearch, new MenuItemCompat.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				setSearch(true);
				//editsearch.requestFocus();
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				setSearch(false);
				/*editsearch.setText("");
	            editsearch.clearFocus();
	            SearchAccount.search_string = null;
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editsearch.getWindowToken(), 0);*/
				return true;
			}});
		return super.onPrepareOptionsMenu(menu);
	}

		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.kv_activity_animator, menu);

		MenuItem loginItem = menu.findItem(R.id.kv_login_item);
		String str = "";
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		int auth_type = prefs.getInt("pref_auth_type", AuthAccount.AUTH_TYPE_UNKNOWN);
		String login = prefs.getString("pref_login", "");
		String password = prefs.getString("pref_password", "");
		switch(auth_type) {
		case AuthAccount.AUTH_TYPE_GUEST:
			str = "Гость";
			break;
		case AuthAccount.AUTH_TYPE_TV:
			str = "Абонент";
			break;
		case AuthAccount.AUTH_TYPE_UNKNOWN:
			str = "Неизвестно";
			break;
		}
		String locLog = str;
		if(login == null||password == null) {
		} else {
			locLog = (login.equals(""))?str:login;
		}

    	loginItem.setTitle(locLog);

    	if(ListAccount.fromLauncher){
    		menu.findItem(R.id.kv_search_item).setVisible(false);
    	}

    	//requestFocus();
		return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.new_game) {
                int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                if (currentapiVersion >= 11) {
                    Intent settingsActivity = new Intent(getBaseContext(), PrMainActivity.class);
                    startActivity(settingsActivity);
                    return true;
                } else {
                    Intent settingsActivity = new Intent(getBaseContext(), OldPreferenceActivity.class);
                    startActivity(settingsActivity);
                    return true;
                }
        } else if (id == R.id.kv_login_item) {
            return true;
        } else if (id == R.id.exit) {
           // exit();
			return true;
		} else if (id == R.id.exitlogin) {
			exit();
			return true;
        } else if (id == R.id.kv_home_item) {
            home();
            return true;
        } else if (id == R.id.kv_refresh_item) {
                refresh();
                return true;
        }

        return super.onOptionsItemSelected(item);
	}

	Map<String, Object> contextMenuMap;
	CombineSimpleAdapter contextMenuAdapter;


	@SuppressWarnings("unchecked")
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		contextMenuAdapter = ((List)v).getAdapter();
		contextMenuMap = (Map<String, Object>) ((List)v).getAdapter().getItem(info.position);
		menu.setHeaderTitle((CharSequence)contextMenuMap.get("name"));

		if(contextMenuMap.get(TagConsts.TYPE) != null
		        && contextMenuMap.get(TagConsts.TYPE).equals(TypeConsts.CHANNEL)) {
			if(!account.isTVAccount()) {
				return;
			}
			if(contextMenuMap.get("star").equals("0")) {
				menu.add(Menu.NONE, 0, 0, "добавить в избранное");
			} else {
				menu.add(Menu.NONE, 1, 0, "удалить из избранного");
			}
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		int menuItemIndex = item.getItemId();
		String[] menuItems = {"add", "remove"};
		String menuItemName = menuItems[menuItemIndex];
		if(menuItemName.equals("add")) {
			if(contextMenuMap != null) {
				AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... arg0) {
						String address = ApiConst.STAR;
						String params = "channel_id=" + arg0[0];
						return HTTPClient.getXML(address, params, AuthRequestConst.AUTH_TV);
					}

					@Override
					protected void onPostExecute(String result) {
						String str;
						if(result.equals("<results status=\"error\"><msg>Can't connect to server</msg></results>")) {
							str = "Невозможно подключиться к серверу";
						} else {
							contextMenuMap.put("star", "1");
							contextMenuAdapter.notifyDataSetChanged();
							str = "Канал добавлен в избранное: " + contextMenuMap.get("name");
						}

						Toast toast = Toast.makeText(getApplicationContext(),
						                             str, Toast.LENGTH_SHORT);
						toast.show();
						return;
					}
				};
				task.execute((String)contextMenuMap.get("id"));
			}
		} else if(menuItemName.equals("remove")) {
			if(contextMenuMap != null) {
				AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... arg0) {
						String address = ApiConst.UNSTAR;
						String params = "channel_id=" + arg0[0];
						return HTTPClient.getXML(address, params, AuthRequestConst.AUTH_TV);
					}

					@Override
					protected void onPostExecute(String result) {
						String str;
						if(result.equals("<results status=\"error\"><msg>Can't connect to server</msg></results>")) {
							str = "Невозможно подключиться к серверу";
						} else {
							contextMenuAdapter.getData().remove(contextMenuMap);
							contextMenuAdapter.notifyDataSetChanged();
							str = "Канал удален из избранного: " + contextMenuMap.get("name");
						}

						Toast toast = Toast.makeText(getApplicationContext(),
						                             str, Toast.LENGTH_SHORT);
						toast.show();
						return;
					}
				};
				task.execute((String)contextMenuMap.get("id"));
			}
		}
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		searchFragment.goSearch(query);
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		// todo searchFragment.goSearch(newText);
		return false;
	}

	@Override
	protected void onDestroy() {
		Log.d("Krasview/core", "onDestroy");
		super.onDestroy();
		int pid = Process.myPid();
		Process.killProcess(pid);
	}
}
