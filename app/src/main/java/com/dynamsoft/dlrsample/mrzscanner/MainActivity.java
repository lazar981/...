package com.dynamsoft.dlrsample.mrzscanner;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.dynamsoft.core.CoreException;
import com.dynamsoft.core.LicenseManager;
import com.dynamsoft.core.LicenseVerificationListener;
import com.dynamsoft.dlrsample.mrzscanner.ui.main.MainViewModel;
import com.dynamsoft.dlrsample.mrzscanner.ui.main.ScanFragment;

import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private MainViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        mViewModel.currentFragmentFlag.observe(this, flag -> {
            if (flag == MainViewModel.SCAN_FRAGMENT) {
                Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.custom_red)));
                getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.app_name) + "</font>"));
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            } else if (flag == MainViewModel.RESULT_FRAGMENT) {
                Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.custom_red)));
                getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + "Rezultat skeniranja" + "</font>"));
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            }
        });

        mViewModel.deviceOrientation.setValue(Configuration.ORIENTATION_PORTRAIT);

        if (savedInstanceState == null) {
            LicenseManager.initLicense("DLS2eyJoYW5kc2hha2VDb2RlIjoiMTAyNDA3NzE0LVRYbE5iMkpwYkdWUWNtOXFYMlJzY2ciLCJtYWluU2VydmVyVVJMIjoiaHR0cHM6Ly9tZGxzLmR5bmFtc29mdG9ubGluZS5jb20iLCJvcmdhbml6YXRpb25JRCI6IjEwMjQwNzcxNCIsInN0YW5kYnlTZXJ2ZXJVUkwiOiJodHRwczovL3NkbHMuZHluYW1zb2Z0b25saW5lLmNvbSIsImNoZWNrQ29kZSI6NjYyODIyNDUxfQ==    ", this, (isSuccess, error) -> {
                if (!isSuccess) {
                    error.printStackTrace();
                    MainActivity.this.runOnUiThread(() -> {
                        Toast ts = Toast.makeText(getBaseContext(), "Proverite status licence i pokušajte ponovo", Toast.LENGTH_LONG);
                        ts.show();
                    });
                }
            });
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, ScanFragment.newInstance())
                    .commit();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mViewModel.deviceOrientation.setValue(newConfig.orientation);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}