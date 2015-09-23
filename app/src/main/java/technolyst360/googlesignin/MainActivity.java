package technolyst360.googlesignin;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.plus.model.people.Person;

public class MainActivity extends FragmentActivity {



    private GPlus gPlus;
    private SignInButton googleplus;
    private View loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gPlus = new GPlus(this);
        gPlus.onCreate(savedInstanceState);

        initUI();
    }

    private void initUI() {

        loadingView = findViewById(R.id.loading);
        googleplus = (SignInButton) findViewById(R.id.googleplus);

        getGPlus().setSignInRef(googleplus,
                new GPlus.GPlusLoginListener() {

                    @Override

                    public void onLoginButtonClick() {

                        loadingView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onSuccess(Person person) {
                        loadingView.setVisibility(View.GONE);

                        try {
                            Toast.makeText(getApplicationContext(),person.getDisplayName(),Toast.LENGTH_LONG).show();
                        }catch (Exception exp){
                            Log.e(getClass().getSimpleName(),"HandledException",exp);
                        }

                    }

                    @Override
                    public void onError() {
                        loadingView.setVisibility(View.GONE);

                    }


                });

        googleplus.setSize(SignInButton.SIZE_WIDE);
    }


    @Override
    protected void onStart() {
        super.onStart();
        gPlus.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gPlus.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        gPlus.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onStop() {
        super.onStop();
        gPlus.onStop();
    }


    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        gPlus.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public GPlus getGPlus() {
        return gPlus;
    }

    public void setGPlus(GPlus gPlus) {
        this.gPlus = gPlus;
    }
}
