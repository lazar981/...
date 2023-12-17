package com.dynamsoft.dlrsample.mrzscanner.ui.main;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dynamsoft.dlr.MRZResult;
import com.dynamsoft.dlrsample.mrzscanner.MainActivity;
import com.dynamsoft.dlrsample.mrzscanner.R;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ResultFragment extends DialogFragment {

    private MainViewModel mViewModel;
    private MRZResult mrzResult;
    private ProgressDialog progressBar;
    SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserDetails", MODE_PRIVATE);
    Button buttonSendData;
    String gender, docType, issuer;
    String dateOfBirth = null;
    private final String[] mrzResultStrings = new String[7]; // new String[12];

    public static ResultFragment newInstance() {
        return new ResultFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mViewModel.currentFragmentFlag.setValue(MainViewModel.RESULT_FRAGMENT);
        return inflater.inflate(R.layout.result_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView resultsRecyclerView = view.findViewById(R.id.rv_results_list);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Button buttonScanAgain = view.findViewById(R.id.button_scan_again);
        buttonSendData = view.findViewById(R.id.button_send_data);

        mrzResult = mViewModel.mrzResult;

        prepareUserData();

        mrzResultStrings[0] = "Ime : __" + mrzResult.givenName;
        mrzResultStrings[1] = "Prezime : __" + mrzResult.surname;
        mrzResultStrings[2] = "Pol : __" + gender;
        mrzResultStrings[3] = "Tip dokumenta : __" + docType;
        mrzResultStrings[4] = "Broj dokumenta : __" + mrzResult.docId;
        mrzResultStrings[5] = "Država izdavanja : __" + issuer;
        mrzResultStrings[6] = "Datum rođenja : __" + dateOfBirth;
 /*       mrzResultStrings[5] = "Nacionalnost : __" + mrzResult.nationality;
        mrzResultStrings[8] = "Važi do(DD-MM-YY) : __" + mrzResult.dateOfExpiration;
        mrzResultStrings[9] = "Parsirano : __" + (mrzResult.isParsed ? "DA" : "NE");
        mrzResultStrings[10] = "Verifikovano : __" + (mrzResult.isVerified ? "DA" : "NE");
        mrzResultStrings[11] = "MRZ tekst : __" + mrzResult.mrzText;*/

        ResultAdapter resultAdapter = new ResultAdapter(mrzResultStrings);
        resultsRecyclerView.setAdapter(resultAdapter);
        resultAdapter.notifyDataSetChanged();

        buttonScanAgain.setOnClickListener(view12 -> {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        buttonSendData.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle("Potvrda")
                .setMessage("Da li ste sigurni da želite poslati podatke na server?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> new SendDataTask().execute())
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.baseline_warning_24)
                .show());
    }

    private void prepareUserData() {
        prepareGenderField();
        prepareDocTypeField();
        prepareIssuerField();
        prepareDateOfBirthField();
    }

    private void prepareGenderField() {
        switch (mrzResult.gender) {
            case "F":
                gender = "Žensko";
                break;
            case "M":
                gender = "Muško";
                break;
            default:
                gender = "UNKNOWN";
        }
    }

    private void prepareDocTypeField() {
        switch (mrzResult.docType) {
            case "identity":
                docType = "Lična karta";
                break;
            case "passport":
                docType = "Pasoš";
                break;
            default:
                docType = "UNKNOWN";
        }
    }

    private void prepareIssuerField() {
        switch (mrzResult.issuer) {
            case "SRB":
                issuer = "Serbia";
                break;
            case "BIH":
                issuer = "Bosnia and Herzegovina";
                break;
            case "MNE":
                issuer = "Montenegro";
                break;
            case "HRV":
                issuer = "Croatia";
                break;
            case "SVN":
                issuer = "Slovenia";
                break;
            case "ITA":
                issuer = "Italy";
                break;
            case "AUT":
                issuer = "Austria";
                break;
            case "DE":
                issuer = "Germany";
                break;
            default:
                issuer = mrzResult.issuer;
        }
    }

    private void prepareDateOfBirthField() {
        SimpleDateFormat originalFormat = new SimpleDateFormat("dd-MM-yy");
        originalFormat.setLenient(false);
        SimpleDateFormat suitableFormat = new SimpleDateFormat("MM/dd/yyyy");
        try {
            Date parsedDateOfBirth = originalFormat.parse(mrzResult.dateOfBirth);
            dateOfBirth = suitableFormat.format(parsedDateOfBirth);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private class SendDataTask extends AsyncTask<Void, Void, String> {

        private static final String SOAP_NAMESPACE_CUS = "http://siebel.com/CustomUI";
        private static final String SOAP_NAMESPACE_DATA = "http://www.siebel.com/xml/Mtel%20Document%20Scan%20WS%20Request/Data";
        private static final String SOAP_METHOD_NAME = "Mtel_spcDocument_spcScan_spcWS_Input";
        private static final String SOAP_ACTION = "\"" + "document/http://siebel.com/CustomUI:Mtel_spcDocument_spcScan_spcWS" + "\"";
        private final String SOAP_URL = String.format("http://10.0.41.67/eai_enu/start.swe?SWEExtSource=WebService&SWEExtCmd=Execute&Username=%s&Password=%s",
                sharedPreferences.getString("username", "defaultUsername"),
                sharedPreferences.getString("password", "defaultPassword"));

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            buttonSendData.setEnabled(false);
            progressBar = new ProgressDialog(getContext());
            progressBar.setCancelable(true);
            progressBar.setMessage("Podaci se šalju...");
            progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressBar.show();
        }

        @Override
        protected String doInBackground(Void... voids) {

            SoapObject request = new SoapObject(SOAP_NAMESPACE_CUS, SOAP_METHOD_NAME);

            SoapObject requestData = new SoapObject(SOAP_NAMESPACE_DATA, "Request");
            requestData.addProperty("firstName", mrzResult.givenName);
            requestData.addProperty("lastName", mrzResult.surname);
            requestData.addProperty("gender", gender);
            requestData.addProperty("documentType", docType);
            requestData.addProperty("documentNumber", mrzResult.docId);
            requestData.addProperty("documentCountry", issuer);
            requestData.addProperty("birthDate", dateOfBirth);

            request.addSoapObject(requestData);

            SoapSerializationEnvelope envelope =
                    new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);

            HttpTransportSE transport = new HttpTransportSE(SOAP_URL);
            try {
                transport.call(SOAP_ACTION, envelope);
            } catch (IOException | XmlPullParserException e) {
                return "Proverite da li je server dostupan ili da li imate internet konekciju.";
            }

            try {
                SoapObject response = (SoapObject) envelope.bodyIn;
                SoapPrimitive resultMessage = (SoapPrimitive) response.getProperty("ppMessage");
                return resultMessage.toString();
            } catch (ClassCastException classCastException) {
                SoapFault soapFault = (SoapFault) envelope.bodyIn;
                return soapFault.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressBar.dismiss();
            buttonSendData.setEnabled(true);
            Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
        }
    }
}
