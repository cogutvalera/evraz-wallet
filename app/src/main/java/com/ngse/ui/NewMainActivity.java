package com.ngse.ui;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bitshares.bitshareswallet.AboutActivity;
import com.bitshares.bitshareswallet.BitsharesApplication;
import com.bitshares.bitshareswallet.OnFragmentInteractionListener;
import com.bitshares.bitshareswallet.SettingsActivity;
import com.bitshares.bitshareswallet.room.BitsharesAsset;
import com.bitshares.bitshareswallet.room.BitsharesDao;
import com.bitshares.bitshareswallet.room.BitsharesOperationHistory;
import com.bitshares.bitshareswallet.viewmodel.QuotationViewModel;
import com.bitshares.bitshareswallet.viewmodel.WalletViewModel;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.Broadcast;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.fc.crypto.sha256_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.signed_transaction;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;
import com.ngse.ui.main.MainWalletFragment;
import com.ngse.ui.main.trading.TradingScheduleFragment;
import com.ngse.ui.main.balanceitems.OpenOrdersFragment;
import com.ngse.ui.main.balanceitems.PortfolioFragment;
import com.ngse.ui.main.balanceitems.TransactionsFragment;

import org.evrazcoin.evrazwallet.R;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;


public class NewMainActivity extends AppCompatActivity
        implements OnFragmentInteractionListener {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static boolean rasingColorRevers = false;

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mActionBarDrawerToggle;

    private MainWalletFragment mWalletFragment;
    private TextView mTxtTitle;
    private LinearLayout mLayoutTitle;


    private static final int REQUEST_CODE_SETTINGS = 1;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
            String strChanged = data.getStringExtra("setting_changed");
            if (strChanged.equals("currency_setting")) {
                WalletViewModel walletViewModel = ViewModelProviders.of(this).get(WalletViewModel.class);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BitsharesApplication.getInstance());
                String strCurrency = prefs.getString("currency_setting", "Default");
                walletViewModel.changeCurrency(strCurrency);
            }
        }
    }

    private void onCurrencyUpdate() {
        updateTitle();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Broadcast.CURRENCY_UPDATED));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mWalletFragment.onNewIntent(intent);
    }

    private void updateTitle() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BitsharesApplication.getInstance());
        String strCurrencySetting = prefs.getString("quotation_currency_pair", "BTS:USD");
        String strAsset[] = strCurrencySetting.split(":");

        mTxtTitle.setText(String.format("%s : %s ",
                utils.getAssetSymbolDisply(strAsset[0]),
                utils.getAssetSymbolDisply(strAsset[1]))
        );
    }

    public void setTitleVisible(boolean visible) {
        mLayoutTitle.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rasingColorRevers = getResources().getConfiguration().locale.getCountry().equals("CN");
        setContentView(R.layout.new_activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle("");
        // Toolbar的标题文本不支持居中，故创建了新文本
        mLayoutTitle = (LinearLayout) mToolbar.findViewById(R.id.lay_title);
        mTxtTitle = (TextView) mToolbar.findViewById(R.id.txt_bar_title);
        updateTitle();
        setTitleVisible(false);

        mLayoutTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processChooseCurency();
            }
        });

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                mToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        mDrawerLayout.addDrawerListener(mActionBarDrawerToggle);
        mActionBarDrawerToggle.syncState();


        mWalletFragment = MainWalletFragment.newInstance();


        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.logout:
                    processLogout();
                    break;
                case R.id.settings:
                    Intent intentSettings = new Intent(NewMainActivity.this, SettingsActivity.class);
                    //startActivity(intent);
                    startActivityForResult(intentSettings, REQUEST_CODE_SETTINGS);
                    break;
                case R.id.about:
                    Intent intentAbout = new Intent(NewMainActivity.this, AboutActivity.class);
                    startActivity(intentAbout);
                    break;
            }

            mDrawerLayout.closeDrawer(GravityCompat.START);
            return false;
        });

        WalletViewModel walletViewModel = ViewModelProviders.of(this).get(WalletViewModel.class);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BitsharesApplication.getInstance());
        String strCurrency = prefs.getString("currency_setting", "USD");
        walletViewModel.changeCurrency(strCurrency);


        if (BitsharesWalletWraper.getInstance().load_wallet_file() != 0 ||
                BitsharesWalletWraper.getInstance().is_new() == true) {
            Intent intent = new Intent(this, SignUpButtonActivity.class);
            startActivity(intent);
            finish();
        } else {
            final account_object accountObject = BitsharesWalletWraper.getInstance().get_account();
            if (accountObject != null) {
                View view = navigationView.getHeaderView(0);
                TextView textViewAccountName = (TextView) view.findViewById(R.id.textViewAccountName);
                textViewAccountName.setText(accountObject.name);

                sha256_object.encoder encoder = new sha256_object.encoder();
                encoder.write(accountObject.name.getBytes());

                WebView webView = (WebView) view.findViewById(R.id.webViewAvatar);
                loadWebView(webView, 70, encoder.result().toString());

                TextView textViewAccountId = (TextView) view.findViewById(R.id.textViewAccountId);
                textViewAccountId.setText("#" + accountObject.id.get_instance());

                view.findViewById(R.id.textViewCopyAccount).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("account name", accountObject.name);
                        clipboardManager.setPrimaryClip(clipData);
                        Toast toast = Toast.makeText(NewMainActivity.this, "Copy Successfully", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            }
        }


        QuotationViewModel viewModel = ViewModelProviders.of(this).get(QuotationViewModel.class);
        viewModel.getSelectedMarketTicker().observe(this,
                currencyPair -> {
                    mTxtTitle.setText(String.format("%s : %s ",
                            utils.getAssetSymbolDisply(currencyPair.second),
                            utils.getAssetSymbolDisply(currencyPair.first))
                    );
                });

        showWalletFragment();
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    @Override
    public void notifyTransferComplete(signed_transaction signedTransaction) {
        // 沿用该线程，阻塞住了系统来进行数据更新
        //mWalletFragment.notifyTransferComplete(signedTransaction);
    }

    @Override
    public void notifyCurrencyPairChange() {
        onCurrencyUpdate();
    }


    public static void hideSoftKeyboard(View view, Context context) {
        if (view != null && context != null) {
            InputMethodManager imm = (InputMethodManager) context
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showSoftKeyboard(View view, Context context) {
        if (view != null && context != null) {
            InputMethodManager imm = (InputMethodManager) context
                    .getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, 0);
        }
    }

    private void loadWebView(WebView webView, int size, String encryptText) {
        String htmlShareAccountName = "<html><head><style>body,html { margin:0; padding:0; text-align:center;}</style><meta name=viewport content=width=" + size + ",user-scalable=no/></head><body><canvas width=" + size + " height=" + size + " data-jdenticon-hash=" + encryptText + "></canvas><script src=https://cdn.jsdelivr.net/jdenticon/1.3.2/jdenticon.min.js async></script></body></html>";
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadData(htmlShareAccountName, "text/html", "UTF-8");
    }

    private void processLogout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(NewMainActivity.this, R.style.CustomDialogTheme);
        builder.setPositiveButton(R.string.log_out_dialog_confirm_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Flowable.just(0)
                        .subscribeOn(Schedulers.io())
                        .map(integer -> {
                            BitsharesDao bitsharesDao = BitsharesApplication.getInstance().getBitsharesDatabase().getBitsharesDao();
                            ;
                            List<BitsharesAsset> bitsharesAssetList = bitsharesDao.queryBalanceList();
                            List<BitsharesOperationHistory> bitsharesOperationHistoryList = bitsharesDao.queryOperationHistoryList();
                            bitsharesDao.deleteBalance(bitsharesAssetList);
                            bitsharesDao.deleteOperationHistory(bitsharesOperationHistoryList);

                            return 0;
                        }).subscribe();

                BitsharesWalletWraper.getInstance().reset();
                Intent intent = new Intent(NewMainActivity.this, SignUpButtonActivity.class);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton(R.string.log_out_dialog_cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setMessage(R.string.log_out_dialog_message);
        builder.show();
    }

    private void processChooseCurency() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(NewMainActivity.this, R.style.CustomDialogTheme);
        dialogBuilder.setTitle(R.string.title_select_currency);
        Resources res = getResources();
        final String[] arrValues = res.getStringArray(R.array.quotation_currency_pair_values);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BitsharesApplication.getInstance());
        String strCurrencySetting = prefs.getString("quotation_currency_pair", "BTS:USD");
        int currSelectIndex = 0;
        for (int i = 0; i < arrValues.length; i++) {
            if (arrValues[i].equals(strCurrencySetting)) {
                currSelectIndex = i;
                break;
            }
        }
        dialogBuilder.setSingleChoiceItems(R.array.quotation_currency_pair_options, currSelectIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                prefs.edit().
                        putString("quotation_currency_pair", arrValues[which])
                        .apply();
                String strAsset[] = arrValues[which].split(":");
                QuotationViewModel viewModel = ViewModelProviders.of(NewMainActivity.this).get(QuotationViewModel.class);
                viewModel.selectedMarketTicker(new Pair(strAsset[1], strAsset[0]));

                onCurrencyUpdate();
            }
        });

        dialogBuilder.setPositiveButton(R.string.log_out_dialog_cancel_button, (dialog, which) -> dialog.dismiss());
        dialogBuilder.show();
    }

    public void showWalletFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragmentFrameLayout, mWalletFragment, MainWalletFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    public void showTransactionsFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragmentFrameLayout, TransactionsFragment.newInstance(), TransactionsFragment.TAG);
        ft.addToBackStack(TransactionsFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    public void showPortfolioFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragmentFrameLayout, PortfolioFragment.newInstance(), PortfolioFragment.TAG);
        ft.addToBackStack(PortfolioFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    public void showOpenOrdersFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragmentFrameLayout, OpenOrdersFragment.newInstance(), OpenOrdersFragment.TAG);
        ft.addToBackStack(OpenOrdersFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    public void showTradingScheduleFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragmentFrameLayout, TradingScheduleFragment.newInstance(), TradingScheduleFragment.TAG);
        ft.addToBackStack(TradingScheduleFragment.TAG);
        ft.commitAllowingStateLoss();
    }


}
