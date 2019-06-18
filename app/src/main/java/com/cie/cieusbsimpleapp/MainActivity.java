package com.cie.cieusbsimpleapp;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.cie.usb.Barcode;
import com.cie.usb.CieUsbPrinter;
import com.cie.usb.DebugLog;
import com.cie.usb.FontStyle;
import com.cie.usb.FontType;
import com.cie.usb.PrintColumnParam;
import com.cie.usb.PrinterWidth;

import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_ATTACHED;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_CONN_DEVICE_NAME;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_CONN_STATE_CONNECTED;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_CONN_STATE_CONNECTING;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_CONN_STATE_LISTEN;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_CONN_STATE_NONE;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_DETATCHED;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_MESSAGES;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_MSG;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_NAME;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_NOTIFICATION_ERROR_MSG;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_NOTIFICATION_MSG;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_NOT_CONNECTED;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_NOT_FOUND;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_PERMISSION_GRANTED;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_PERMISSION_REJECTED;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_REQUESTING_PERMISSION;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_SAVED;
import static com.cie.usb.UsbpConstants.RECEIPT_PRINTER_STATUS;


public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnPrint;
    private CheckBox cbFindBlackMark;
    private boolean bFindBlackMark;

    ProgressDialog pdWorkInProgress;

    private static final int BARCODE_WIDTH = 384;
    private static final int BARCODE_HEIGHT = 100;
    private static final int QRCODE_WIDTH = 100;

    public CieUsbPrinter mPrinter = CieUsbPrinter.INSTANCE;
    private int imageAlignment = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tvStatus = findViewById(R.id.status_msg);
        cbFindBlackMark = findViewById(R.id.cbFindBlackMark);
        btnPrint = findViewById(R.id.btnPrint);

        pdWorkInProgress = new ProgressDialog(this);
        pdWorkInProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        
        try {
            mPrinter.initService(MainActivity.this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncPrint().execute();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIPT_PRINTER_MESSAGES);
        LocalBroadcastManager.getInstance(this).registerReceiver(ReceiptPrinterMessageReceiver, intentFilter);
        mPrinter.onActivityStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(ReceiptPrinterMessageReceiver);
        }
        catch (Exception e) {
            DebugLog.logException(e);
        }
    }

    @Override
    protected void onDestroy() {
        DebugLog.logTrace("onDestroy");
        super.onDestroy();
        mPrinter.onActivityDestroy();
    }

    private final BroadcastReceiver ReceiptPrinterMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DebugLog.logTrace("Printer Message Received");
            Bundle b = intent.getExtras();

            switch (b.getInt(RECEIPT_PRINTER_STATUS)) {
                case RECEIPT_PRINTER_ATTACHED:
                    tvStatus.setText("Printer Attached");
                    break;
                case RECEIPT_PRINTER_DETATCHED:
                    tvStatus.setText("Printer Detached");
                    break;
                case RECEIPT_PRINTER_REQUESTING_PERMISSION:
                    tvStatus.setText("Requesting Printer permission");
                    break;
                case RECEIPT_PRINTER_PERMISSION_REJECTED:
                    tvStatus.setText("Printer Permission Denied");
                    break;
                case RECEIPT_PRINTER_PERMISSION_GRANTED:
                    tvStatus.setText("Printer Permission Granted; Ready");
                    break;
                case RECEIPT_PRINTER_CONN_STATE_NONE:
                    tvStatus.setText(R.string.printer_not_conn);
                    break;
                case RECEIPT_PRINTER_CONN_STATE_LISTEN:
                    tvStatus.setText(R.string.ready_for_conn);
                    break;
                case RECEIPT_PRINTER_CONN_STATE_CONNECTING:
                    tvStatus.setText(R.string.printer_connecting);
                    break;
                case RECEIPT_PRINTER_CONN_STATE_CONNECTED:
                    tvStatus.setText(R.string.printer_connected);

                    break;
                case RECEIPT_PRINTER_CONN_DEVICE_NAME:
                    savePrinterMac(b.getString(RECEIPT_PRINTER_NAME));
                    break;
                case RECEIPT_PRINTER_NOTIFICATION_ERROR_MSG:
                    String n = b.getString(RECEIPT_PRINTER_MSG);
                    tvStatus.setText(n);
                    break;
                case RECEIPT_PRINTER_NOTIFICATION_MSG:
                    String m = b.getString(RECEIPT_PRINTER_MSG);
                    tvStatus.setText(m);
                    break;
                case RECEIPT_PRINTER_NOT_CONNECTED:
                    tvStatus.setText("Status : Printer Not Connected");
                    break;
                case RECEIPT_PRINTER_NOT_FOUND:
                    tvStatus.setText("Status : Printer Not Found");
                    break;
                case RECEIPT_PRINTER_SAVED:
                    tvStatus.setText(R.string.printer_saved);
                    break;
            }
        }
    };

    private void savePrinterMac(String sMacAddr) {
       if (sMacAddr.length() > 4) {
            tvStatus.setText("Preferred Printer saved");
        }
        else {
            tvStatus.setText("Preferred Printer cleared");
        }
    }

    private class AsyncPrint extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bFindBlackMark = cbFindBlackMark.isChecked();
            btnPrint.setEnabled(false);
            cbFindBlackMark.setEnabled(false);

            pdWorkInProgress.setIndeterminate(true);
            pdWorkInProgress.setMessage("Printing ...");
            pdWorkInProgress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            pdWorkInProgress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_48MM);

            mPrinter.resetPrinter();

            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

            mPrinter.printGrayScaleImage(bmp,1);

            mPrinter.setAlignmentCenter();
            mPrinter.setBoldOn();
            mPrinter.setCharRightSpacing(10);
            mPrinter.printTextLine("\nMY COMPANY BILL\n");
            mPrinter.setBoldOff();
            mPrinter.setCharRightSpacing(0);
            mPrinter.printTextLine("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            mPrinter.pixelLineFeed(50);
            // Bill Header End

            // Bill Details Start

            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("Customer Name     : Aditya    \n");
            mPrinter.printTextLine("Customer Order ID : 00067     \n");
            mPrinter.printTextLine("------------------------------\n");
            mPrinter.printTextLine("  Item      Quantity     Price\n");
            mPrinter.printTextLine("------------------------------\n");
            mPrinter.printTextLine("  Item 1          1       1.00\n");
            mPrinter.printTextLine("  Bags           10    2220.00\n");
            mPrinter.printTextLine("  Next Item     999   99999.00\n");
            mPrinter.printLineFeed();
            mPrinter.printTextLine("------------------------------\n");
            mPrinter.printTextLine("  Total              107220.00\n");
            mPrinter.printTextLine("------------------------------\n");
            mPrinter.printLineFeed();
            mPrinter.setFontStyle(true,true, FontStyle.DOUBLE_HEIGHT, FontType.FONT_A);
            mPrinter.printTextLine("    Thank you ! Visit Again   \n");
            mPrinter.setFontStyle(false,false, FontStyle.NORMAL, FontType.FONT_A);
            mPrinter.printLineFeed();
            mPrinter.printTextLine("******************************\n");
            mPrinter.printLineFeed();

            mPrinter.printTextLine("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();

            mPrinter.printQRcode("http://www.coineltech.com/",QRCODE_WIDTH, imageAlignment);
            mPrinter.printLineFeed();

            mPrinter.printBarcode("1234567890123", Barcode.CODE_128,BARCODE_WIDTH, BARCODE_HEIGHT, imageAlignment);
            //mPrinter.setAlignmentCenter();
            mPrinter.setCharRightSpacing(10);
            mPrinter.printTextLine("  1234567890123\n");

            mPrinter.printUnicodeText(" SDK can print in any language that the android device supports and display. \n" +
                    " English - English \n" +
                    " kannada - ಕನ್ನಡ \n" +
                    " Hindi - हिंदी \n" +
                    " Tamil - தமிழ் \n" +
                    " Telugu - తెలుగు \n" +
                    " Marathi - मराठी \n" +
                    " Malayalam - മലയാളം \n" +
                    " Gujarati - ગુજરાતી \n" +
                    " Urdu -  اردو" +
                    "\n");

            String[] sCol1 = {"ABC","DEFG","H","IJKLM","XYZ"};
            PrintColumnParam pcp1stCol = new PrintColumnParam(sCol1,33, Layout.Alignment.ALIGN_NORMAL,22, Typeface.create(Typeface.SANS_SERIF,Typeface.NORMAL));
            String[] sCol2 = {":",":",":",":",":"};
            PrintColumnParam pcp2ndCol = new PrintColumnParam(sCol2,33,Layout.Alignment.ALIGN_CENTER,22);
            String[] sCol3 = {"₹1.00","₹20.00","₹300.00","₹4,000.00","₹50,000.89"};
            PrintColumnParam pcp3rdCol = new PrintColumnParam(sCol3,33,Layout.Alignment.ALIGN_OPPOSITE,22);
            mPrinter.PrintTable(pcp1stCol,pcp2ndCol,pcp3rdCol);
            if (bFindBlackMark) {
                mPrinter.findBlackMarkAndMove(20);
            } else {
                //Clearance for Paper tear
                mPrinter.printLineFeed();
                mPrinter.printLineFeed();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //wait for printing to complete
            try {
                Thread.sleep(6000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            btnPrint.setEnabled(true);
            cbFindBlackMark.setEnabled(true);
            pdWorkInProgress.cancel();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            Toast.makeText(this,"CIE USB Simple App",Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
