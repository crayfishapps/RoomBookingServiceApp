package com.crayfishapps.roombookingservice;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    IntentFilter[] intentFilters;
    String[][] nfcTech;
    PendingIntent pendingIntent;
    NfcAdapter myNfcAdapter;
    String serialnumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (myNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!myNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        serialnumber =  Build.SERIAL;
        // System.out.println(serialnumber);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter mifare = new IntentFilter((NfcAdapter.ACTION_TECH_DISCOVERED));
        intentFilters = new IntentFilter[] { mifare };
        nfcTech = new String[][] { new String[] {  NfcA.class.getName() } };
    }

    public void onPause() {
        super.onPause();
        myNfcAdapter.disableForegroundDispatch(this);
    }

    public void onResume() {
        super.onResume();
        myNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, nfcTech);
    }

    public void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] id = tag.getId();
        ByteBuffer wrapped = ByteBuffer.wrap(id);
        // wrapped.order(ByteOrder.LITTLE_ENDIAN);
        int signedInt = wrapped.getInt();
        long number = signedInt & 0xffffffffl;
        sendRequest(Long.toHexString(number));
    }

    private void sendRequest(String badge) {
        final TextView user= (TextView) findViewById(R.id.textViewUser);
        final TextView status = (TextView) findViewById(R.id.textViewStatus);

        String url = "http://roombookingservice.appspot.com/report?reader=" + serialnumber + "&card=" + badge;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.contains("Error")) {
                            user.setText("");
                            status.setText(response);
                        }
                        else {
                            int cr = response.indexOf("\n");
                            user.setText(response.substring(0, cr));
                            status.setText(response.substring(cr + 1, response.length()));
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                user.setText("");
                status.setText("Communication error");
            }
        });

        queue.add(stringRequest);
    }
}