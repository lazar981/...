package com.dynamsoft.dlrsample.mrzscanner;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private Button buttonLogin;
    private EditText editTextUsername;
    private TextInputEditText textInputEditTextPassword;
    private ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.custom_red)));
        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.app_name) + "</font>"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        editTextUsername = findViewById(R.id.edit_text_username);
        textInputEditTextPassword = findViewById(R.id.text_input_edit_text_password);
        buttonLogin = findViewById(R.id.button_login);

        buttonLogin.setOnClickListener(view -> {
            if (String.valueOf(editTextUsername.getText()).isEmpty() || String.valueOf(textInputEditTextPassword.getText()).isEmpty()) {
                Toast.makeText(getApplicationContext(), "Korisničko ime i lozinka ne smeju biti prazni", Toast.LENGTH_SHORT).show();
                return;
            }
            SharedPreferences sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString("username", String.valueOf(editTextUsername.getText()));
            editor.putString("password", String.valueOf(textInputEditTextPassword.getText()));
            editor.apply();

            new AuthTask().execute();

        });
    }

    private class AuthTask extends AsyncTask<Void, Void, Map<String, String>> {

        SharedPreferences sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE);
        private static final String SOAP_NAMESPACE_CUS = "http://siebel.com/CustomUI";
        private static final String SOAP_NAMESPACE_DATA = "http://www.siebel.com/xml/Mtel%20Authentication%20Business%20Service%20WS%20Request/Data";
        private static final String SOAP_METHOD_NAME = "Mtel_spcAuthentication_spcBusiness_spcService_spcWF_1_Input";
        private static final String SOAP_ACTION = "\"" + "document/http://siebel.com/CustomUI:Mtel_spcAuthentication_spcBusiness_spcService_spcWF_1" + "\"";
        private static final String SOAP_URL = "http://10.0.41.67/eai_enu/start.swe?SWEExtSource=WebService&SWEExtCmd=Execute&Username=MTEL_BATCH&Password=MTEL_BATCH01";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            buttonLogin.setEnabled(false);
            progressBar = new ProgressDialog(LoginActivity.this);
            progressBar.setCancelable(true);
            progressBar.setMessage("Prijavljivanje u toku...");
            progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressBar.show();
        }

        @Override
        protected Map<String, String> doInBackground(Void... voids) {

            SoapObject request = new SoapObject(SOAP_NAMESPACE_CUS, SOAP_METHOD_NAME);

            SoapObject requestData = new SoapObject(SOAP_NAMESPACE_DATA, "Request");
            requestData.addProperty("userName", sharedPreferences.getString("username", "defaultUsername"));
            requestData.addProperty("password", sharedPreferences.getString("password", "defaultPassword"));

            request.addSoapObject(requestData);

            SoapSerializationEnvelope envelope =
                    new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);

            Map<String, String> result = new HashMap<>();
            HttpTransportSE transport = new HttpTransportSE(SOAP_URL);
            try {
                transport.call(SOAP_ACTION, envelope);
            } catch (IOException | XmlPullParserException e) {
                result.put("ppMessage", "Proverite da li je server dostupan ili da li imate internet konekciju.");
                result.put("ppResult", "1");
                return result;
            }

            SoapObject response = (SoapObject) envelope.bodyIn;
            SoapPrimitive ppMessage = (SoapPrimitive) response.getProperty("ppMessage");
            SoapPrimitive ppResult = (SoapPrimitive) response.getProperty("ppResult");

            result.put("ppMessage", ppMessage.toString());
            result.put("ppResult", ppResult.toString());

            return result;

        }

        @Override
        protected void onPostExecute(Map<String, String> result) {
            super.onPostExecute(result);
            progressBar.dismiss();
            buttonLogin.setEnabled(true);
            Toast.makeText(getApplicationContext(), result.get("ppMessage"), Toast.LENGTH_SHORT).show();

            if (result.get("ppResult").equals("0")) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    }
}