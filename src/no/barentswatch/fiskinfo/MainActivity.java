package no.barentswatch.fiskinfo;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.setContext(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if(getAuthenticationLevel()) {
			loadView(MyPageActivity.class);
		} else {
			Button loginButton = (Button) findViewById(R.id.mainActivityLoginButton);
			
			loginButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ShowLoginDialog(getContext(), R.string.app_name, findViewById(R.layout.dialog_login));
				}
			});
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
